package za.ac.mycput.service;

import za.ac.mycput.domain.enums.UserType;
import za.ac.mycput.dto.AdminDtos.AdminStatsResponse;
import za.ac.mycput.dto.AdminDtos.AdminUserResponse;
import za.ac.mycput.dto.AdminDtos.PublicStatsResponse;
import za.ac.mycput.dto.CommonDtos.PageResponse;
import za.ac.mycput.dto.ProfileDtos.CompanyResponse;
import za.ac.mycput.security.AuthUser;

import java.util.List;

public interface IAdminService {

    AdminStatsResponse stats();

    PublicStatsResponse publicStats();

    PageResponse<AdminUserResponse> users(UserType role, String search, int page, int size);

    AdminUserResponse setUserActive(AuthUser admin, Integer userId, boolean active);

    List<CompanyResponse> companies(Boolean verified);

    CompanyResponse setCompanyVerified(AuthUser admin, Integer companyId, boolean verified);
}
