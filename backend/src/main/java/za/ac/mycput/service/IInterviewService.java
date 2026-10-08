package za.ac.mycput.service;

import za.ac.mycput.dto.InterviewDtos.InterviewResponse;
import za.ac.mycput.dto.InterviewDtos.ProposeInterviewRequest;
import za.ac.mycput.security.AuthUser;

import java.util.List;

public interface IInterviewService {

    /** The hiring company proposes (or reschedules) an interview for an application. */
    InterviewResponse propose(Integer companyUserId, Integer applicationId, ProposeInterviewRequest request);

    /** The applicant confirms one of the proposed slots. */
    InterviewResponse confirm(Integer studentUserId, Integer interviewId, Integer slotId);

    /** Either the hiring company or the applicant may cancel. */
    InterviewResponse cancel(AuthUser actor, Integer interviewId);

    /** Proposed and confirmed interviews for the logged-in student or company, soonest first. */
    List<InterviewResponse> upcoming(AuthUser actor);

    /** An iCalendar (.ics) file for a confirmed interview. */
    String calendarFile(AuthUser actor, Integer interviewId);
}
