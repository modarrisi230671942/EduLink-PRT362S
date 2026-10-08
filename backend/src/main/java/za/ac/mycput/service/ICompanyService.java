package za.ac.mycput.service;

import za.ac.mycput.dto.ProfileDtos.CompanyProfileRequest;
import za.ac.mycput.dto.ProfileDtos.CompanyResponse;

public interface ICompanyService {

    CompanyResponse getProfile(Integer userId);

    CompanyResponse updateProfile(Integer userId, CompanyProfileRequest request);
}
