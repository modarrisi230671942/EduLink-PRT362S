package za.ac.mycput.service.support;

import za.ac.mycput.domain.*;
import za.ac.mycput.dto.AdminDtos.AdminUserResponse;
import za.ac.mycput.dto.AdminDtos.AuditLogResponse;
import za.ac.mycput.dto.InterviewDtos.InterviewResponse;
import za.ac.mycput.dto.InterviewDtos.SlotResponse;
import za.ac.mycput.dto.ApplicationDtos.ApplicationResponse;
import za.ac.mycput.dto.CommonDtos.NotificationResponse;
import za.ac.mycput.dto.JobDtos.JobResponse;
import za.ac.mycput.dto.JobDtos.MatchResult;
import za.ac.mycput.dto.ProfileDtos.CompanyResponse;
import za.ac.mycput.dto.ProfileDtos.StudentResponse;

import java.time.LocalDate;

/**
 * Converts JPA entities into API response DTOs.
 * Must be called inside a transaction, because it follows lazy relationships.
 */
public final class DtoMapper {

    private DtoMapper() {}

    public static StudentResponse toStudentResponse(Student s) {
        return new StudentResponse(
                s.getStudentId(),
                s.getUser().getEmail(),
                s.getFullName(),
                s.getStudentNumber(),
                s.getCourse(),
                s.getInstitution(),
                s.getGraduationYear(),
                s.getSkills(),
                SkillMatcher.toList(s.getSkills()),
                s.hasCv(),
                s.getCvOriginalName(),
                s.getCvUploadedAt(),
                s.wantsJobAlerts());
    }

    public static CompanyResponse toCompanyResponse(Company c) {
        User user = c.getUser();
        return new CompanyResponse(
                c.getCompanyId(),
                user.getUserId(),
                user.getEmail(),
                c.getCompanyName(),
                c.getIndustry(),
                c.getLocation(),
                c.getWebsite(),
                c.isVerified(),
                user.isActive(),
                user.getCreatedAt());
    }

    public static JobResponse toJobResponse(JobPosting j, Long applicationCount, MatchResult match, Boolean applied) {
        return toJobResponse(j, applicationCount, match, applied, null);
    }

    /**
     * @param applicationCount only for the owning company's view, otherwise null
     * @param match            only for a logged-in student, otherwise null
     * @param applied          only for a logged-in student, otherwise null
     * @param saved            only for a logged-in student, otherwise null
     */
    public static JobResponse toJobResponse(JobPosting j, Long applicationCount, MatchResult match,
                                            Boolean applied, Boolean saved) {
        Company c = j.getCompany();
        return new JobResponse(
                j.getJobId(),
                j.getTitle(),
                j.getDescription(),
                j.getRequirements(),
                SkillMatcher.toList(j.getRequirements()),
                j.getLocation(),
                j.getJobType(),
                j.getApplicationDeadline(),
                j.getPostedDate(),
                j.isActive(),
                j.isOpenForApplications(LocalDate.now()),
                c.getCompanyId(),
                c.getCompanyName(),
                c.getIndustry(),
                c.getWebsite(),
                applicationCount,
                match,
                applied,
                saved);
    }

    public static ApplicationResponse toApplicationResponse(Application a) {
        return toApplicationResponse(a, null);
    }

    public static ApplicationResponse toApplicationResponse(Application a, Interview interview) {
        JobPosting job = a.getJob();
        Student student = a.getStudent();
        return new ApplicationResponse(
                a.getApplicationId(),
                a.getStatus(),
                a.getCoverLetter(),
                a.getAppliedDate(),
                a.getStatusUpdatedAt(),
                job.getJobId(),
                job.getTitle(),
                job.getJobType(),
                job.getLocation(),
                job.getCompany().getCompanyId(),
                job.getCompany().getCompanyName(),
                student.getStudentId(),
                student.getFullName(),
                student.getUser().getEmail(),
                student.getStudentNumber(),
                student.getCourse(),
                student.getInstitution(),
                student.getGraduationYear(),
                SkillMatcher.toList(student.getSkills()),
                student.hasCv(),
                SkillMatcher.match(student.getSkills(), job.getRequirements()),
                interview == null ? null : toInterviewResponse(interview));
    }

    public static InterviewResponse toInterviewResponse(Interview i) {
        Application a = i.getApplication();
        return new InterviewResponse(
                i.getInterviewId(),
                a.getApplicationId(),
                i.getStatus(),
                i.getMode(),
                i.getLocation(),
                i.getNotes(),
                i.getDurationMinutes(),
                i.getSlots().stream().map(s -> new SlotResponse(s.getSlotId(), s.getStartsAt())).toList(),
                i.getConfirmedStart(),
                i.confirmedEnd(),
                i.getUpdatedAt(),
                a.getJob().getTitle(),
                a.getJob().getCompany().getCompanyName(),
                a.getStudent().getFullName());
    }

    public static AuditLogResponse toAuditLogResponse(AuditLog l) {
        return new AuditLogResponse(l.getLogId(), l.getAction(), l.getActorEmail(), l.getTargetType(),
                l.getTargetId(), l.getDetails(), l.getIpAddress(), l.getCreatedAt());
    }

    public static NotificationResponse toNotificationResponse(Notification n) {
        return new NotificationResponse(n.getNotificationId(), n.getType(), n.getMessage(), n.getLink(),
                n.isRead(), n.getCreatedAt());
    }

    public static AdminUserResponse toAdminUserResponse(User u, String displayName) {
        return new AdminUserResponse(u.getUserId(), u.getEmail(), u.getUserType(), displayName, u.isActive(),
                u.getCreatedAt());
    }
}
