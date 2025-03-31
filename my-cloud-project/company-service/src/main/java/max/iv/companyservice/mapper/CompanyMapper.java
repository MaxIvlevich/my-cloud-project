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

    @Mapping(source = "company.id", target = "id")
    @Mapping(source = "company.companyName", target = "companyName")
    @Mapping(source = "company.budget", target = "budget")
    @Mapping(source = "employees", target = "employees") // Передаем готовый список сотрудников
    CompanyDto toCompanyDtoWithEmployees(Company company, List<UserDto> employees);
    List<CompanyDto> toCompanyDtoList(List<Company> companies);

    @Mapping(target = "id", ignore = true) // ID генерируется БД
    @Mapping(target = "employeeIds", ignore = true) // Сотрудники добавляются отдельно
    Company createCompanyDtoToCompany(CreateCompanyDto createCompanyDto);

    @Mapping(target = "id", ignore = true) // Не обновляем ID
    @Mapping(target = "employeeIds", ignore = true) // Не обновляем список ID здесь
    void updateCompanyFromDto(UpdateCompanyDto updateCompanyDto, @MappingTarget Company company);
}
