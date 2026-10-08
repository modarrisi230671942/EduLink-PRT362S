package za.ac.mycput.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.mycput.domain.Application;
import za.ac.mycput.domain.Interview;
import za.ac.mycput.domain.InterviewSlot;
import za.ac.mycput.domain.JobPosting;
import za.ac.mycput.domain.enums.*;
import za.ac.mycput.dto.InterviewDtos.InterviewResponse;
import za.ac.mycput.dto.InterviewDtos.ProposeInterviewRequest;
import za.ac.mycput.exception.ApiException;
import za.ac.mycput.repository.ApplicationRepository;
import za.ac.mycput.repository.InterviewRepository;
import za.ac.mycput.security.AuthUser;
import za.ac.mycput.service.IInterviewService;
import za.ac.mycput.service.INotificationService;
import za.ac.mycput.service.support.DtoMapper;
import za.ac.mycput.service.support.IcsCalendar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@Service
public class InterviewServiceImpl implements IInterviewService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM 'at' HH:mm", Locale.ENGLISH);

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final INotificationService notifications;

    public InterviewServiceImpl(InterviewRepository interviewRepository,
                                ApplicationRepository applicationRepository,
                                INotificationService notifications) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.notifications = notifications;
    }

    @Override
    @Transactional
    public InterviewResponse propose(Integer companyUserId, Integer applicationId, ProposeInterviewRequest request) {
        Application application = applicationRepository.findWithDetails(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application"));
        JobPosting job = application.getJob();
        if (!job.getCompany().getUser().getUserId().equals(companyUserId)) {
            throw ApiException.forbidden("You can only schedule interviews for your own applicants.");
        }
        if (application.getStatus() == ApplicationStatus.REJECTED) {
            throw ApiException.badRequest("This application was declined. Change its status before scheduling an interview.");
        }
        if (new HashSet<>(request.slots()).size() != request.slots().size()) {
            throw ApiException.badRequest("Each proposed time slot must be different.");
        }
        if (request.mode() != InterviewMode.IN_PERSON && request.mode() != InterviewMode.PHONE
                && (request.location() == null || request.location().isBlank())) {
            throw ApiException.badRequest("Please add the meeting link for an online interview.");
        }

        Interview interview = interviewRepository.findByApplicationId(applicationId)
                .orElseGet(() -> new Interview(application));
        boolean rescheduling = interview.getInterviewId() != null && interview.getStatus() != InterviewStatus.CANCELLED;
        interview.propose(request.mode(), blankToNull(request.location()), blankToNull(request.notes()),
                request.durationMinutes(), request.slots());
        // Flush so the new slots get their IDs before we return them
        interview = interviewRepository.saveAndFlush(interview);

        // Scheduling an interview means the application is being reviewed
        if (application.getStatus() == ApplicationStatus.PENDING) {
            application.changeStatus(ApplicationStatus.REVIEWED);
        }

        notifications.notify(application.getStudent().getUser(), NotificationType.INTERVIEW_PROPOSED,
                job.getCompany().getCompanyName() + (rescheduling ? " rescheduled" : " invited you to")
                        + " an interview for " + job.getTitle() + ". Choose a time that suits you.",
                "/student/applications");
        return DtoMapper.toInterviewResponse(interview);
    }

    @Override
    @Transactional
    public InterviewResponse confirm(Integer studentUserId, Integer interviewId, Integer slotId) {
        Interview interview = load(interviewId);
        Application application = interview.getApplication();
        if (!application.getStudent().getUser().getUserId().equals(studentUserId)) {
            throw ApiException.forbidden("You can only respond to your own interview invitations.");
        }
        if (interview.getStatus() != InterviewStatus.PROPOSED) {
            throw ApiException.badRequest("This interview is no longer awaiting a response.");
        }
        InterviewSlot slot = interview.getSlots().stream()
                .filter(s -> s.getSlotId().equals(slotId))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("That time slot is not part of this invitation."));
        if (!slot.getStartsAt().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("That time slot has already passed. Ask the company to reschedule.");
        }

        interview.confirm(slot);
        JobPosting job = application.getJob();
        notifications.notify(job.getCompany().getUser(), NotificationType.INTERVIEW_CONFIRMED,
                application.getStudent().getFullName() + " confirmed their " + job.getTitle() + " interview for "
                        + WHEN.format(slot.getStartsAt()) + ".",
                "/company/interviews");
        return DtoMapper.toInterviewResponse(interview);
    }

    @Override
    @Transactional
    public InterviewResponse cancel(AuthUser actor, Integer interviewId) {
        Interview interview = load(interviewId);
        Application application = interview.getApplication();
        JobPosting job = application.getJob();
        boolean isCompany = actor.role() == UserType.COMPANY
                && job.getCompany().getUser().getUserId().equals(actor.userId());
        boolean isStudent = actor.role() == UserType.STUDENT
                && application.getStudent().getUser().getUserId().equals(actor.userId());
        if (!isCompany && !isStudent) {
            throw ApiException.forbidden("You do not have access to this interview.");
        }
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            return DtoMapper.toInterviewResponse(interview);
        }

        interview.cancel();
        if (isCompany) {
            notifications.notify(application.getStudent().getUser(), NotificationType.INTERVIEW_CANCELLED,
                    job.getCompany().getCompanyName() + " cancelled your interview for " + job.getTitle() + ".",
                    "/student/applications");
        } else {
            notifications.notify(job.getCompany().getUser(), NotificationType.INTERVIEW_CANCELLED,
                    application.getStudent().getFullName() + " cancelled their interview for " + job.getTitle() + ".",
                    "/company/interviews");
        }
        return DtoMapper.toInterviewResponse(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> upcoming(AuthUser actor) {
        List<Interview> interviews = actor.role() == UserType.STUDENT
                ? interviewRepository.findActiveForStudentUser(actor.userId())
                : interviewRepository.findActiveForCompanyUser(actor.userId());
        LocalDateTime now = LocalDateTime.now();
        return interviews.stream()
                // Hide confirmed interviews that are already over, and invitations whose slots have all passed
                .filter(i -> i.getStatus() == InterviewStatus.CONFIRMED
                        ? i.confirmedEnd().isAfter(now)
                        : i.getSlots().stream().anyMatch(s -> s.getStartsAt().isAfter(now)))
                .sorted(Comparator.comparing(InterviewServiceImpl::sortKey))
                .map(DtoMapper::toInterviewResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String calendarFile(AuthUser actor, Integer interviewId) {
        Interview interview = load(interviewId);
        Application application = interview.getApplication();
        JobPosting job = application.getJob();
        boolean allowed = (actor.role() == UserType.COMPANY && job.getCompany().getUser().getUserId().equals(actor.userId()))
                || (actor.role() == UserType.STUDENT && application.getStudent().getUser().getUserId().equals(actor.userId()));
        if (!allowed) {
            throw ApiException.forbidden("You do not have access to this interview.");
        }
        if (interview.getStatus() != InterviewStatus.CONFIRMED) {
            throw ApiException.badRequest("Only confirmed interviews can be added to a calendar.");
        }
        return IcsCalendar.event(
                "interview-" + interview.getInterviewId() + "@edulink",
                "Interview: " + job.getTitle() + " — " + job.getCompany().getCompanyName(),
                "Candidate: " + application.getStudent().getFullName() + "\n"
                        + "Type: " + interview.getMode().label()
                        + (interview.getNotes() == null ? "" : "\n\n" + interview.getNotes()),
                interview.getLocation(),
                interview.getConfirmedStart(),
                interview.confirmedEnd());
    }

    private Interview load(Integer interviewId) {
        return interviewRepository.findWithDetails(interviewId).orElseThrow(() -> ApiException.notFound("Interview"));
    }

    private static LocalDateTime sortKey(Interview i) {
        if (i.getConfirmedStart() != null) {
            return i.getConfirmedStart();
        }
        return i.getSlots().isEmpty() ? LocalDateTime.MAX : i.getSlots().get(0).getStartsAt();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
