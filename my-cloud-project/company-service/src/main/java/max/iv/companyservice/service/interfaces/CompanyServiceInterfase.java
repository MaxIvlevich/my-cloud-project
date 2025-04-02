package max.iv.companyservice.service.interfaces;

import max.iv.companyservice.DTO.CompanyDto;
import max.iv.companyservice.DTO.CreateCompanyDto;
import max.iv.companyservice.DTO.UpdateCompanyDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CompanyServiceInterfase {
    @Transactional(readOnly = true)
    List<CompanyDto> getAllCompanies();

    @Transactional(readOnly = true)
    CompanyDto getCompanyById(Long id);

    @Transactional
    CompanyDto createCompany(CreateCompanyDto createCompanyDto);

    @Transactional
    CompanyDto updateCompany(Long id, UpdateCompanyDto updateCompanyDto);

    @Transactional
    void deleteCompany(Long id);

    @Transactional
    CompanyDto addEmployeeToCompany(Long companyId, Long employeeId);

    @Transactional
    CompanyDto removeEmployeeFromCompany(Long companyId, Long employeeId);

    @Transactional(readOnly = true)
    List<CompanyDto> getCompaniesByIds(List<Long> ids);
}
