package za.ac.mycput.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.ac.mycput.domain.JobPosting;
import za.ac.mycput.domain.SavedJob;
import za.ac.mycput.domain.Student;
import za.ac.mycput.dto.JobDtos.JobResponse;
import za.ac.mycput.dto.JobDtos.MatchResult;
import za.ac.mycput.dto.ProfileDtos.CvSkillsResponse;
import za.ac.mycput.dto.ProfileDtos.StudentProfileRequest;
import za.ac.mycput.dto.ProfileDtos.StudentResponse;
import za.ac.mycput.exception.ApiException;
import za.ac.mycput.repository.ApplicationRepository;
import za.ac.mycput.repository.JobPostingRepository;
import za.ac.mycput.repository.SavedJobRepository;
import za.ac.mycput.service.IFileStorageService;
import za.ac.mycput.service.IStudentService;
import za.ac.mycput.service.support.DtoMapper;
import za.ac.mycput.service.support.ProfileLookup;
import za.ac.mycput.service.support.SkillExtractor;
import za.ac.mycput.service.support.SkillMatcher;

import java.time.LocalDate;
import java.util.*;

@Service
public class StudentServiceImpl implements IStudentService {

    private final ProfileLookup profiles;
    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final SavedJobRepository savedJobRepository;
    private final IFileStorageService fileStorage;
    private final SkillExtractor skillExtractor;

    public StudentServiceImpl(ProfileLookup profiles,
                              JobPostingRepository jobPostingRepository,
                              ApplicationRepository applicationRepository,
                              SavedJobRepository savedJobRepository,
                              IFileStorageService fileStorage,
                              SkillExtractor skillExtractor) {
        this.profiles = profiles;
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
        this.savedJobRepository = savedJobRepository;
        this.fileStorage = fileStorage;
        this.skillExtractor = skillExtractor;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getProfile(Integer userId) {
        return DtoMapper.toStudentResponse(profiles.student(userId));
    }

    @Override
    @Transactional
    public StudentResponse updateProfile(Integer userId, StudentProfileRequest request) {
        Student student = profiles.student(userId);
        student.updateProfile(
                request.fullName().trim(),
                request.course().trim(),
                request.institution().trim(),
                request.graduationYear(),
                request.skills() == null ? null : String.join(", ", SkillMatcher.toList(request.skills())));
        if (request.jobAlerts() != null) {
            student.setJobAlerts(request.jobAlerts());
        }
        return DtoMapper.toStudentResponse(student);
    }

    @Override
    @Transactional
    public StudentResponse uploadCv(Integer userId, MultipartFile file) {
        Student student = profiles.student(userId);
        String previous = student.getCvFileName();
        String stored = fileStorage.storeCv(file);
        student.attachCv(stored, file.getOriginalFilename());
        fileStorage.deleteCv(previous);
        return DtoMapper.toStudentResponse(student);
    }

    @Override
    @Transactional(readOnly = true)
    public IFileStorageService.StoredFile downloadCv(Integer userId) {
        Student student = profiles.student(userId);
        if (!student.hasCv()) {
            throw ApiException.notFound("CV");
        }
        return fileStorage.loadCv(student.getCvFileName(), student.getCvOriginalName());
    }

    @Override
    @Transactional
    public StudentResponse deleteCv(Integer userId) {
        Student student = profiles.student(userId);
        fileStorage.deleteCv(student.getCvFileName());
        student.removeCv();
        return DtoMapper.toStudentResponse(student);
    }

    @Override
    @Transactional(readOnly = true)
    public CvSkillsResponse cvSkills(Integer userId) {
        Student student = profiles.student(userId);
        if (!student.hasCv()) {
            throw ApiException.notFound("CV");
        }
        List<String> found = skillExtractor.extractFromPdf(
                fileStorage.loadCv(student.getCvFileName(), student.getCvOriginalName()).resource());

        // A found skill is "new" unless the profile already covers it (same whole-word rule as matching)
        String currentSkills = student.getSkills() == null ? "" : student.getSkills();
        List<String> newSkills = found.stream()
                .filter(skill -> SkillMatcher.match(currentSkills, skill).score() == 0)
                .toList();
        return new CvSkillsResponse(found, newSkills);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> recommendations(Integer userId, int limit) {
        Student student = profiles.student(userId);
        Set<Integer> appliedJobIds = new HashSet<>(applicationRepository.findJobIdsByStudentId(student.getStudentId()));
        Set<Integer> savedJobIds = new HashSet<>(savedJobRepository.findJobIdsByStudentId(student.getStudentId()));

        record Scored(JobPosting job, MatchResult match) {}

        return jobPostingRepository.findOpenJobs(LocalDate.now()).stream()
                .filter(job -> !appliedJobIds.contains(job.getJobId()))
                .map(job -> new Scored(job, SkillMatcher.match(student.getSkills(), job.getRequirements())))
                .filter(scored -> scored.match().score() > 0)
                .sorted(Comparator.comparingInt((Scored s) -> s.match().score()).reversed()
                        .thenComparing(s -> s.job().getApplicationDeadline(), Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .map(scored -> DtoMapper.toJobResponse(scored.job(), null, scored.match(), false,
                        savedJobIds.contains(scored.job().getJobId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> savedJobs(Integer userId) {
        Student student = profiles.student(userId);
        Set<Integer> appliedJobIds = new HashSet<>(applicationRepository.findJobIdsByStudentId(student.getStudentId()));
        return savedJobRepository.findForStudent(student.getStudentId()).stream()
                .map(SavedJob::getJob)
                .map(job -> DtoMapper.toJobResponse(job, null,
                        SkillMatcher.match(student.getSkills(), job.getRequirements()),
                        appliedJobIds.contains(job.getJobId()), true))
                .toList();
    }

    @Override
    @Transactional
    public void saveJob(Integer userId, Integer jobId) {
        Student student = profiles.student(userId);
        JobPosting job = jobPostingRepository.findWithCompany(jobId).orElseThrow(() -> ApiException.notFound("Job"));
        if (!job.getCompany().isVerified()) {
            throw ApiException.notFound("Job");
        }
        SavedJob.Key key = new SavedJob.Key(student.getStudentId(), jobId);
        if (!savedJobRepository.existsById(key)) {
            savedJobRepository.save(new SavedJob(student, job));
        }
    }

    @Override
    @Transactional
    public void unsaveJob(Integer userId, Integer jobId) {
        Student student = profiles.student(userId);
        savedJobRepository.deleteById(new SavedJob.Key(student.getStudentId(), jobId));
    }
}
