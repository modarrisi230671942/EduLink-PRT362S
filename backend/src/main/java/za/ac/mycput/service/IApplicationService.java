package za.ac.mycput.service;

import za.ac.mycput.domain.enums.ApplicationStatus;
import za.ac.mycput.dto.ApplicationDtos.ApplicationResponse;
import za.ac.mycput.dto.ApplicationDtos.ApplyRequest;
import za.ac.mycput.security.AuthUser;

import java.util.List;

public interface IApplicationService {

    ApplicationResponse apply(Integer studentUserId, ApplyRequest request);

    List<ApplicationResponse> studentApplications(Integer studentUserId);

    /** A student may withdraw their own application while it is still pending. */
    void withdraw(Integer studentUserId, Integer applicationId);

    List<ApplicationResponse> companyApplications(Integer companyUserId, ApplicationStatus status, Integer jobId);

    ApplicationResponse updateStatus(Integer companyUserId, Integer applicationId, ApplicationStatus status);

    /** The applicant's CV. Allowed for the company that owns the job and for the applicant themselves. */
    IFileStorageService.StoredFile applicantCv(AuthUser actor, Integer applicationId);
}
