package za.ac.mycput.service;

import org.springframework.web.multipart.MultipartFile;
import za.ac.mycput.dto.JobDtos.JobResponse;
import za.ac.mycput.dto.ProfileDtos.CvSkillsResponse;
import za.ac.mycput.dto.ProfileDtos.StudentProfileRequest;
import za.ac.mycput.dto.ProfileDtos.StudentResponse;

import java.util.List;

public interface IStudentService {

    StudentResponse getProfile(Integer userId);

    StudentResponse updateProfile(Integer userId, StudentProfileRequest request);

    StudentResponse uploadCv(Integer userId, MultipartFile file);

    IFileStorageService.StoredFile downloadCv(Integer userId);

    StudentResponse deleteCv(Integer userId);

    /** Skills found in the student's uploaded CV, and which of them are not yet on their profile. */
    CvSkillsResponse cvSkills(Integer userId);

    /** Open jobs the student has not applied to, ranked by how well their skills match. */
    List<JobResponse> recommendations(Integer userId, int limit);

    List<JobResponse> savedJobs(Integer userId);

    void saveJob(Integer userId, Integer jobId);

    void unsaveJob(Integer userId, Integer jobId);
}
