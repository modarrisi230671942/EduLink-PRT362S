package za.ac.mycput.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.mycput.domain.Company;
import za.ac.mycput.domain.JobPosting;
import za.ac.mycput.domain.Student;
import za.ac.mycput.domain.enums.AuditAction;
import za.ac.mycput.domain.enums.JobType;
import za.ac.mycput.domain.enums.NotificationType;
import za.ac.mycput.domain.enums.UserType;
import za.ac.mycput.repository.InterviewRepository;
import za.ac.mycput.repository.SavedJobRepository;
import za.ac.mycput.service.IAuditService;
import za.ac.mycput.service.INotificationService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import za.ac.mycput.dto.CommonDtos.PageResponse;
import za.ac.mycput.dto.JobDtos.JobRequest;
import za.ac.mycput.dto.JobDtos.JobResponse;
import za.ac.mycput.exception.ApiException;
import za.ac.mycput.repository.ApplicationRepository;
import za.ac.mycput.repository.JobPostingRepository;
import za.ac.mycput.repository.StudentRepository;
import za.ac.mycput.security.AuthUser;
import za.ac.mycput.service.IJobPostingService;
import za.ac.mycput.service.support.DtoMapper;
import za.ac.mycput.service.support.ProfileLookup;
import za.ac.mycput.service.support.SkillMatcher;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class JobPostingServiceImpl implements IJobPostingService {

    private static final int MAX_PAGE_SIZE = 50;
    /** Minimum skill match (%) for a student to be alerted about a newly posted job. */
    static final int JOB_ALERT_THRESHOLD = 60;

    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final SavedJobRepository savedJobRepository;
    private final InterviewRepository interviewRepository;
    private final ProfileLookup profiles;
    private final INotificationService notifications;
    private final IAuditService audit;

    public JobPostingServiceImpl(JobPostingRepository jobPostingRepository,
                                 ApplicationRepository applicationRepository,
                                 StudentRepository studentRepository,
                                 SavedJobRepository savedJobRepository,
                                 InterviewRepository interviewRepository,
                                 ProfileLookup profiles,
                                 INotificationService notifications,
                                 IAuditService audit) {
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.savedJobRepository = savedJobRepository;
        this.interviewRepository = interviewRepository;
        this.profiles = profiles;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<JobResponse> search(AuthUser viewer, String query, JobType type, String sort, int page, int size) {
        Sort order = "deadline".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Order.asc("applicationDeadline"), Sort.Order.desc("postedDate"))
                : Sort.by(Sort.Order.desc("postedDate"), Sort.Order.desc("jobId"));
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), order);

        String pattern = (query == null || query.isBlank()) ? null : "%" + query.trim().toLowerCase() + "%";
        var results = jobPostingRepository.searchOpenJobs(LocalDate.now(), type, pattern, pageable);

        StudentView studentView = studentView(viewer);
        return PageResponse.of(results, job -> toResponseFor(job, studentView));
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJob(AuthUser viewer, Integer jobId) {
        JobPosting job = jobPostingRepository.findWithCompany(jobId).orElseThrow(() -> ApiException.notFound("Job"));
        boolean isOwner = viewer != null && viewer.role() == UserType.COMPANY
                && job.getCompany().getUser().getUserId().equals(viewer.userId());
        boolean isAdmin = viewer != null && viewer.role() == UserType.ADMIN;
        // Jobs from unverified companies are hidden from the public
        if (!job.getCompany().isVerified() && !isOwner && !isAdmin) {
            throw ApiException.notFound("Job");
        }
        return toResponseFor(job, studentView(viewer));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> companyJobs(Integer companyUserId) {
        Company company = profiles.company(companyUserId);
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : applicationRepository.countPerJobForCompany(company.getCompanyId())) {
            counts.put((Integer) row[0], (Long) row[1]);
        }
        return jobPostingRepository.findByCompanyId(company.getCompanyId()).stream()
                .map(job -> DtoMapper.toJobResponse(job, counts.getOrDefault(job.getJobId(), 0L), null, null))
                .toList();
    }

    @Override
    @Transactional
    public JobResponse create(Integer companyUserId, JobRequest request) {
        Company company = profiles.company(companyUserId);
        // Security fix S7: enforced on the server, not only hidden in the UI
        if (!company.isVerified()) {
            throw ApiException.forbidden(
                    "Your company must be verified by an administrator before you can post jobs.");
        }
        JobPosting job = jobPostingRepository.save(new JobPosting.Builder()
                .setCompany(company)
                .setTitle(request.title().trim())
                .setDescription(request.description().trim())
                .setRequirements(normaliseList(request.requirements()))
                .setLocation(request.location().trim())
                .setJobType(request.jobType())
                .setApplicationDeadline(request.applicationDeadline())
                .setIsActive(true)
                .build());
        sendJobAlerts(job);
        return DtoMapper.toJobResponse(job, 0L, null, null);
    }

    /** Job alerts: tell every opted-in student whose skills strongly match the new job. */
    private void sendJobAlerts(JobPosting job) {
        for (Student student : studentRepository.findByJobAlertsTrue()) {
            int score = SkillMatcher.match(student.getSkills(), job.getRequirements()).score();
            if (score >= JOB_ALERT_THRESHOLD) {
                notifications.notify(student.getUser(), NotificationType.JOB_MATCH,
                        "New " + score + "% match: " + job.getTitle() + " at " + job.getCompany().getCompanyName() + ".",
                        "/jobs?q=" + URLEncoder.encode(job.getTitle(), StandardCharsets.UTF_8));
            }
        }
    }

    @Override
    @Transactional
    public JobResponse update(Integer companyUserId, Integer jobId, JobRequest request) {
        JobPosting job = ownedJob(companyUserId, jobId);
        job.update(
                request.title().trim(),
                request.description().trim(),
                normaliseList(request.requirements()),
                request.location().trim(),
                request.jobType(),
                request.applicationDeadline());
        return DtoMapper.toJobResponse(job, null, null, null);
    }

    @Override
    @Transactional
    public JobResponse setActive(Integer companyUserId, Integer jobId, boolean active) {
        JobPosting job = ownedJob(companyUserId, jobId);
        job.setActive(active);
        return DtoMapper.toJobResponse(job, null, null, null);
    }

    @Override
    @Transactional
    public void delete(AuthUser actor, Integer jobId) {
        JobPosting job = jobPostingRepository.findWithCompany(jobId).orElseThrow(() -> ApiException.notFound("Job"));
        boolean isOwner = actor.role() == UserType.COMPANY
                && job.getCompany().getUser().getUserId().equals(actor.userId());
        if (!isOwner && actor.role() != UserType.ADMIN) {
            throw ApiException.forbidden("You can only delete your own job postings.");
        }
        // Remove dependants explicitly (not only via DB cascades), so this works on every database
        interviewRepository.deleteSlotsByJobId(job.getJobId());
        interviewRepository.deleteByJobId(job.getJobId());
        savedJobRepository.deleteByJobId(job.getJobId());
        applicationRepository.deleteByJobId(job.getJobId());
        jobPostingRepository.delete(job);
        if (!isOwner) {
            audit.record(actor.userId(), actor.email(), AuditAction.JOB_DELETED_BY_ADMIN, "JOB", jobId,
                    job.getTitle() + " (" + job.getCompany().getCompanyName() + ")");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Loads a job and checks it belongs to the logged-in company (security fix S4). */
    private JobPosting ownedJob(Integer companyUserId, Integer jobId) {
        JobPosting job = jobPostingRepository.findWithCompany(jobId).orElseThrow(() -> ApiException.notFound("Job"));
        if (!job.getCompany().getUser().getUserId().equals(companyUserId)) {
            throw ApiException.forbidden("You can only manage your own job postings.");
        }
        return job;
    }

    /** What a student viewer needs to personalise job cards: skills, applied jobs and saved jobs. */
    private record StudentView(String skills, Set<Integer> appliedJobIds, Set<Integer> savedJobIds) {}

    private StudentView studentView(AuthUser viewer) {
        if (viewer == null || viewer.role() != UserType.STUDENT) {
            return null;
        }
        return studentRepository.findByUser_UserId(viewer.userId())
                .map((Student s) -> new StudentView(s.getSkills(),
                        new HashSet<>(applicationRepository.findJobIdsByStudentId(s.getStudentId())),
                        new HashSet<>(savedJobRepository.findJobIdsByStudentId(s.getStudentId()))))
                .orElse(null);
    }

    private static JobResponse toResponseFor(JobPosting job, StudentView view) {
        if (view == null) {
            return DtoMapper.toJobResponse(job, null, null, null);
        }
        return DtoMapper.toJobResponse(job, null,
                SkillMatcher.match(view.skills(), job.getRequirements()),
                view.appliedJobIds().contains(job.getJobId()),
                view.savedJobIds().contains(job.getJobId()));
    }

    private static String normaliseList(String csv) {
        return String.join(", ", SkillMatcher.toList(csv));
    }
}
