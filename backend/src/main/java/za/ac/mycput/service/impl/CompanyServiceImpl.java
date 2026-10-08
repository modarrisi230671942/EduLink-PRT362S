package za.ac.mycput.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.mycput.domain.Company;
import za.ac.mycput.dto.ProfileDtos.CompanyProfileRequest;
import za.ac.mycput.dto.ProfileDtos.CompanyResponse;
import za.ac.mycput.service.ICompanyService;
import za.ac.mycput.service.support.DtoMapper;
import za.ac.mycput.service.support.ProfileLookup;

@Service
public class CompanyServiceImpl implements ICompanyService {

    private final ProfileLookup profiles;

    public CompanyServiceImpl(ProfileLookup profiles) {
        this.profiles = profiles;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getProfile(Integer userId) {
        return DtoMapper.toCompanyResponse(profiles.company(userId));
    }

    @Override
    @Transactional
    public CompanyResponse updateProfile(Integer userId, CompanyProfileRequest request) {
        Company company = profiles.company(userId);
        company.updateProfile(
                request.companyName().trim(),
                blankToNull(request.industry()),
                blankToNull(request.location()),
                blankToNull(request.website()));
        return DtoMapper.toCompanyResponse(company);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
