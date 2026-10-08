package za.ac.mycput.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import za.ac.mycput.domain.Application;
import za.ac.mycput.domain.Company;
import za.ac.mycput.domain.JobPosting;
import za.ac.mycput.domain.Student;
import za.ac.mycput.domain.User;
import za.ac.mycput.domain.enums.ApplicationStatus;
import za.ac.mycput.domain.enums.JobType;
import za.ac.mycput.domain.enums.NotificationType;
import za.ac.mycput.domain.enums.UserType;
import za.ac.mycput.dto.ApplicationDtos.ApplicationResponse;
import za.ac.mycput.dto.ApplicationDtos.ApplyRequest;
import za.ac.mycput.exception.ApiException;
import za.ac.mycput.repository.ApplicationRepository;
import za.ac.mycput.repository.InterviewRepository;
import za.ac.mycput.repository.JobPostingRepository;
import za.ac.mycput.service.IFileStorageService;
import za.ac.mycput.service.INotificationService;
import za.ac.mycput.service.support.ProfileLookup;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    private static final String COVER_LETTER = "I am very interested in this role and have the right skills for it.";

    @Mock ApplicationRepository applicationRepository;
    @Mock JobPostingRepository jobPostingRepository;
    @Mock ProfileLookup profiles;
    @Mock INotificationService notifications;
    @Mock IFileStorageService fileStorage;
    @Mock InterviewRepository interviewRepository;

    @InjectMocks ApplicationServiceImpl service;

    private User studentUser;
    private User companyUser;
    private Student student;
    private Company company;

    @BeforeEach
    void setUp() {
        studentUser = new User.Builder().setUserId(1).setEmail("alice@test.com").setUserType(UserType.STUDENT).build();
        companyUser = new User.Builder().setUserId(2).setEmail("hr@techcorp.com").setUserType(UserType.COMPANY).build();
        student = new Student.Builder().setStudentId(10).setUser(studentUser).setFullName("Alice Mbatha")
                .setStudentNumber("STU001").setCourse("CS").setInstitution("CPUT").setSkills("Java, SQL").build();
        company = new Company.Builder().setCompanyId(20).setUser(companyUser).setCompanyName("TechCorp")
                .setIsVerified(true).build();
    }

    private JobPosting job(boolean active, LocalDate deadline) {
        return new JobPosting.Builder().setJobId(30).setCompany(company).setTitle("Junior Developer")
                .setRequirements("Java, Spring Boot").setJobType(JobType.GRADUATE)
                .setApplicationDeadline(deadline).setIsActive(active).build();
    }

    @Test
    void applySavesApplicationAndNotifiesTheCompany() {
        when(profiles.student(1)).thenReturn(student);
        when(jobPostingRepository.findWithCompany(30)).thenReturn(Optional.of(job(true, LocalDate.now().plusDays(10))));
        when(applicationRepository.existsByJob_JobIdAndStudent_StudentId(30, 10)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = service.apply(1, new ApplyRequest(30, COVER_LETTER));

        assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(response.match().score()).isEqualTo(50);
        verify(notifications).notify(eq(companyUser), eq(NotificationType.APPLICATION_RECEIVED),
                contains("Alice Mbatha applied for Junior Developer"), anyString());
    }

    @Test
    void applyAfterDeadlineIsRejected() {
        when(profiles.student(1)).thenReturn(student);
        when(jobPostingRepository.findWithCompany(30)).thenReturn(Optional.of(job(true, LocalDate.now().minusDays(1))));

        assertThatThrownBy(() -> service.apply(1, new ApplyRequest(30, COVER_LETTER)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("deadline");
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyToInactiveJobIsRejected() {
        when(profiles.student(1)).thenReturn(student);
        when(jobPostingRepository.findWithCompany(30)).thenReturn(Optional.of(job(false, LocalDate.now().plusDays(10))));

        assertThatThrownBy(() -> service.apply(1, new ApplyRequest(30, COVER_LETTER)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no longer accepting");
    }

    @Test
    void duplicateApplicationIsAConflict() {
        when(profiles.student(1)).thenReturn(student);
        when(jobPostingRepository.findWithCompany(30)).thenReturn(Optional.of(job(true, LocalDate.now().plusDays(10))));
        when(applicationRepository.existsByJob_JobIdAndStudent_StudentId(30, 10)).thenReturn(true);

        assertThatThrownBy(() -> service.apply(1, new ApplyRequest(30, COVER_LETTER)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void companyCannotReviewApplicationsForAnotherCompanysJob() {
        Application application = new Application.Builder().setApplicationId(40)
                .setJob(job(true, LocalDate.now().plusDays(10))).setStudent(student).build();
        when(applicationRepository.findWithDetails(40)).thenReturn(Optional.of(application));

        Integer someOtherCompanyUserId = 99;
        assertThatThrownBy(() -> service.updateStatus(someOtherCompanyUserId, 40, ApplicationStatus.ACCEPTED))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void statusChangeNotifiesTheStudent() {
        Application application = new Application.Builder().setApplicationId(40)
                .setJob(job(true, LocalDate.now().plusDays(10))).setStudent(student).build();
        when(applicationRepository.findWithDetails(40)).thenReturn(Optional.of(application));

        ApplicationResponse response = service.updateStatus(2, 40, ApplicationStatus.ACCEPTED);

        assertThat(response.status()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(response.statusUpdatedAt()).isNotNull();
        verify(notifications).notify(eq(studentUser), eq(NotificationType.APPLICATION_STATUS_CHANGED),
                contains("is now accepted"), eq("/student/applications"));
    }

    @Test
    void onlyPendingApplicationsCanBeWithdrawn() {
        Application application = new Application.Builder().setApplicationId(40)
                .setJob(job(true, LocalDate.now().plusDays(10))).setStudent(student)
                .setStatus(ApplicationStatus.REVIEWED).build();
        when(applicationRepository.findWithDetails(40)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.withdraw(1, 40))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only pending");
        verify(applicationRepository, never()).delete(any());
    }
}
