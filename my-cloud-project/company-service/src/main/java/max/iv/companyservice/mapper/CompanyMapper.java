package max.iv.companyservice.mapper;

import max.iv.companyservice.DTO.CompanyDto;
import max.iv.companyservice.DTO.CreateCompanyDto;
import max.iv.companyservice.DTO.UpdateCompanyDto;
import max.iv.companyservice.DTO.UserDto;
import max.iv.companyservice.model.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    @Mapping(target = "employees", ignore = true)
    CompanyDto toCompanyDto(Company company);
    @CompanyWithEmployeesMapper
    @Mapping(source = "company.id", target = "id")
    @Mapping(source = "company.companyName", target = "companyName")
    @Mapping(source = "company.budget", target = "budget")
    @Mapping(source = "employees", target = "employees")
    CompanyDto toCompanyDtoWithEmployees(Company company, List<UserDto> employees);
    List<CompanyDto> toCompanyDtoList(List<Company> companies);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeIds", ignore = true)
    Company createCompanyDtoToCompany(CreateCompanyDto createCompanyDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeIds", ignore = true)
    void updateCompanyFromDto(UpdateCompanyDto updateCompanyDto, @MappingTarget Company company);
    @SimpleCompanyMapper
    @Mapping(target = "employees", ignore = true)
    CompanyDto toSimpleCompanyDto(Company company);
}
