package za.ac.mycput.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.mycput.domain.Application;
import za.ac.mycput.domain.Company;
import za.ac.mycput.domain.Interview;
import za.ac.mycput.domain.JobPosting;
import za.ac.mycput.domain.Student;
import za.ac.mycput.domain.enums.ApplicationStatus;
import za.ac.mycput.domain.enums.InterviewStatus;
import za.ac.mycput.repository.InterviewRepository;

import java.util.HashMap;
import java.util.Map;
import za.ac.mycput.domain.enums.NotificationType;
import za.ac.mycput.domain.enums.UserType;
import za.ac.mycput.dto.ApplicationDtos.ApplicationResponse;
import za.ac.mycput.dto.ApplicationDtos.ApplyRequest;
import za.ac.mycput.exception.ApiException;
import za.ac.mycput.repository.ApplicationRepository;
import za.ac.mycput.repository.JobPostingRepository;
import za.ac.mycput.security.AuthUser;
import za.ac.mycput.service.IApplicationService;
import za.ac.mycput.service.IFileStorageService;
import za.ac.mycput.service.INotificationService;
import za.ac.mycput.service.support.DtoMapper;
import za.ac.mycput.service.support.ProfileLookup;

import java.time.LocalDate;
import java.util.List;

@Service
public class ApplicationServiceImpl implements IApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ProfileLookup profiles;
    private final INotificationService notifications;
    private final IFileStorageService fileStorage;
    private final InterviewRepository interviewRepository;

    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                                  JobPostingRepository jobPostingRepository,
                                  ProfileLookup profiles,
                                  INotificationService notifications,
                                  IFileStorageService fileStorage,
                                  InterviewRepository interviewRepository) {
        this.applicationRepository = applicationRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.profiles = profiles;
        this.notifications = notifications;
        this.fileStorage = fileStorage;
        this.interviewRepository = interviewRepository;
    }

    /** Maps applications to responses, attaching each one's interview (loaded in a single query). */
    private List<ApplicationResponse> withInterviews(List<Application> applications) {
        if (applications.isEmpty()) {
            return List.of();
        }
        Map<Integer, Interview> byApplication = new HashMap<>();
        for (Interview interview : interviewRepository.findByApplicationIds(
                applications.stream().map(Application::getApplicationId).toList())) {
            byApplication.put(interview.getApplication().getApplicationId(), interview);
        }
        return applications.stream()
                .map(a -> DtoMapper.toApplicationResponse(a, byApplication.get(a.getApplicationId())))
                .toList();
    }

    @Override
    @Transactional
    public ApplicationResponse apply(Integer studentUserId, ApplyRequest request) {
        // The student is taken from the login token, never from the request body (security fix S4)
        Student student = profiles.student(studentUserId);
        JobPosting job = jobPostingRepository.findWithCompany(request.jobId())
                .orElseThrow(() -> ApiException.notFound("Job"));

        LocalDate today = LocalDate.now();
        if (!job.getCompany().isVerified() || !job.isActive()) {
            throw ApiException.badRequest("This job is no longer accepting applications.");
        }
        if (job.isDeadlinePassed(today)) {
            throw ApiException.badRequest("The application deadline for this job has passed.");
        }
        if (applicationRepository.existsByJob_JobIdAndStudent_StudentId(job.getJobId(), student.getStudentId())) {
            throw ApiException.conflict("You have already applied for this job.");
        }

        Application application = applicationRepository.save(new Application.Builder()
                .setJob(job)
                .setStudent(student)
                .setCoverLetter(request.coverLetter().trim())
                .setStatus(ApplicationStatus.PENDING)
                .build());

        notifications.notify(job.getCompany().getUser(), NotificationType.APPLICATION_RECEIVED,
                student.getFullName() + " applied for " + job.getTitle() + ".",
                "/company/applications");

        return DtoMapper.toApplicationResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> studentApplications(Integer studentUserId) {
        Student student = profiles.student(studentUserId);
        return withInterviews(applicationRepository.findByStudentId(student.getStudentId()));
    }

    @Override
    @Transactional
    public void withdraw(Integer studentUserId, Integer applicationId) {
        Application application = applicationRepository.findWithDetails(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application"));
        if (!application.getStudent().getUser().getUserId().equals(studentUserId)) {
            throw ApiException.forbidden("You can only withdraw your own applications.");
        }
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw ApiException.badRequest("Only pending applications can be withdrawn.");
        }
        JobPosting job = application.getJob();
        notifications.notify(job.getCompany().getUser(), NotificationType.APPLICATION_WITHDRAWN,
                application.getStudent().getFullName() + " withdrew their application for " + job.getTitle() + ".",
                "/company/applications");
        applicationRepository.delete(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> companyApplications(Integer companyUserId, ApplicationStatus status, Integer jobId) {
        Company company = profiles.company(companyUserId);
        return withInterviews(applicationRepository.findForCompany(company.getCompanyId(), status, jobId));
    }

    @Override
    @Transactional
    public ApplicationResponse updateStatus(Integer companyUserId, Integer applicationId, ApplicationStatus status) {
        Application application = applicationRepository.findWithDetails(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application"));
        JobPosting job = application.getJob();
        if (!job.getCompany().getUser().getUserId().equals(companyUserId)) {
            throw ApiException.forbidden("You can only review applications for your own job postings.");
        }
        Interview interview = interviewRepository.findByApplicationId(applicationId).orElse(null);
        if (application.getStatus() == status) {
            return DtoMapper.toApplicationResponse(application, interview);
        }

        application.changeStatus(status);
        // Declining a candidate also cancels any interview that is still planned
        if (status == ApplicationStatus.REJECTED && interview != null && interview.getStatus() != InterviewStatus.CANCELLED) {
            interview.cancel();
        }
        notifications.notify(application.getStudent().getUser(), NotificationType.APPLICATION_STATUS_CHANGED,
                "Your application for " + job.getTitle() + " at " + job.getCompany().getCompanyName()
                        + " is now " + status.label() + ".",
                "/student/applications");

        return DtoMapper.toApplicationResponse(application, interview);
    }

    @Override
    @Transactional(readOnly = true)
    public IFileStorageService.StoredFile applicantCv(AuthUser actor, Integer applicationId) {
        Application application = applicationRepository.findWithDetails(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application"));
        boolean isHiringCompany = actor.role() == UserType.COMPANY
                && application.getJob().getCompany().getUser().getUserId().equals(actor.userId());
        boolean isApplicant = actor.role() == UserType.STUDENT
                && application.getStudent().getUser().getUserId().equals(actor.userId());
        if (!isHiringCompany && !isApplicant) {
            throw ApiException.forbidden("You do not have access to this CV.");
        }
        Student student = application.getStudent();
        if (!student.hasCv()) {
            throw ApiException.notFound("CV");
        }
        return fileStorage.loadCv(student.getCvFileName(), student.getFullName() + " CV");
    }
}
