package za.ac.mycput.service;

import za.ac.mycput.domain.enums.JobType;
import za.ac.mycput.dto.CommonDtos.PageResponse;
import za.ac.mycput.dto.JobDtos.JobRequest;
import za.ac.mycput.dto.JobDtos.JobResponse;
import za.ac.mycput.security.AuthUser;

import java.util.List;

public interface IJobPostingService {

    /** Public search of open jobs. When {@code viewer} is a student, each job includes a match score. */
    PageResponse<JobResponse> search(AuthUser viewer, String query, JobType type, String sort, int page, int size);

    JobResponse getJob(AuthUser viewer, Integer jobId);

    /** All of the logged-in company's jobs (including inactive and expired), with application counts. */
    List<JobResponse> companyJobs(Integer companyUserId);

    JobResponse create(Integer companyUserId, JobRequest request);

    JobResponse update(Integer companyUserId, Integer jobId, JobRequest request);

    JobResponse setActive(Integer companyUserId, Integer jobId, boolean active);

    /** Owning company or an admin may delete. */
    void delete(AuthUser actor, Integer jobId);
}
