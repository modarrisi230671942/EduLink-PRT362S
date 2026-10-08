package za.ac.mycput.service;

import za.ac.mycput.dto.AuthDtos.*;

public interface IAuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse registerStudent(StudentRegisterRequest request);

    AuthResponse registerCompany(CompanyRegisterRequest request);

    UserSummary currentUser(Integer userId);

    void changePassword(Integer userId, ChangePasswordRequest request);
}
