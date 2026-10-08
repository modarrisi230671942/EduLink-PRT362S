package za.ac.mycput.service.support;

import org.springframework.stereotype.Component;
import za.ac.mycput.domain.Company;
import za.ac.mycput.domain.Student;
import za.ac.mycput.exception.ApiException;
import za.ac.mycput.repository.CompanyRepository;
import za.ac.mycput.repository.StudentRepository;

/**
 * Finds the profile that belongs to the logged-in user.
 * Services always resolve the profile from the authenticated user ID — never from an ID sent by the
 * client — which is what stops one user acting on another user's data (security fix S4).
 */
@Component
public class ProfileLookup {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;

    public ProfileLookup(StudentRepository studentRepository, CompanyRepository companyRepository) {
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
    }

    public Student student(Integer userId) {
        return studentRepository.findByUser_UserId(userId)
                .orElseThrow(() -> ApiException.notFound("Student profile"));
    }

    public Company company(Integer userId) {
        return companyRepository.findByUser_UserId(userId)
                .orElseThrow(() -> ApiException.notFound("Company profile"));
    }
}
