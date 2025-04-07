package max.iv.companyservice.service;

import max.iv.companyservice.client.UserServiceClient;
import max.iv.companyservice.DTO.CompanyDto;
import max.iv.companyservice.DTO.CreateCompanyDto;
import max.iv.companyservice.DTO.UpdateCompanyDto;
import max.iv.companyservice.DTO.UserDto;
import max.iv.companyservice.exception.ResourceNotFoundException;
import max.iv.companyservice.mapper.CompanyMapper;
import max.iv.companyservice.model.Company;
import max.iv.companyservice.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class responsible for managing company data and their associated employees.
 * <p>
 * It interacts with the {@link CompanyRepository} for persistence, uses {@link CompanyMapper}
 * for DTO conversions, and communicates with {@link UserServiceClient} to fetch employee (user)
 * details and coordinate updates related to employee-company associations.
 * <p>
 * Logging is provided by SLF4J via Lombok's {@code @Slf4j}.
 * Dependencies are injected via constructor using Lombok's {@code @RequiredArgsConstructor}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyServiceImpl implements max.iv.companyservice.service.interfaces.CompanyServiceInterfase {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final UserServiceClient userServiceClient;

    private List<UserDto> fetchEmployeesByIds(List<Long> employeeIds) {
        if (CollectionUtils.isEmpty(employeeIds)) {
            return Collections.emptyList();
        }
        try {
            log.debug("Fetching employees with IDs: {}", employeeIds);
            List<UserDto> employees = userServiceClient.getUsersByIds(employeeIds);
            log.debug("Fetched {} employees", employees.size());
            if (employees.size() != employeeIds.size()) {
                Set<Long> returnedIds = employees.stream().map(UserDto::id).collect(Collectors.toSet());
                List<Long> missingIds = employeeIds.stream().filter(id -> !returnedIds.contains(id)).toList();
                log.warn("Could not find employee details for IDs: {}", missingIds);
            }
            return employees;
        } catch (Exception e) {
            log.error("Failed to fetch employee details for IDs {}: {}", employeeIds, e.getMessage());
            return Collections.emptyList();
        }
    }


    private CompanyDto mapCompanyToDtoWithEmployees(Company company) {
        List<UserDto> employees = fetchEmployeesByIds(company.getEmployeeIds());
        return companyMapper.toCompanyDtoWithEmployees(company, employees);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<CompanyDto> getAllCompanies(Pageable pageable) {
        log.debug("Attempting to fetch companies page. Page: {}, Size: {}, Sort: {}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        Page<Company> companyPage = companyRepository.findAll(pageable);
        List<Company> companiesOnPage = companyPage.getContent();
        if (companiesOnPage.isEmpty()) {
            log.info("No companies found on page {}. Returning empty page.", pageable.getPageNumber());
            return Page.empty(pageable);
        }
        log.debug("Found {} companies on page {}.", companiesOnPage.size(), pageable.getPageNumber());

        Set<Long> employeeIds = collectEmployeeIds(companiesOnPage);
        Map<Long, UserDto> employeeMap = fetchAndMapEmployees(employeeIds, pageable.getPageNumber());
        List<CompanyDto> companyDtos = mapCompaniesToDto(companiesOnPage, employeeMap);
        log.info("Finished processing getAllCompanies page {}. Returning {} company DTOs. Total elements: {}.",
                pageable.getPageNumber(), companyDtos.size(), companyPage.getTotalElements());
        return new PageImpl<>(companyDtos, pageable, companyPage.getTotalElements());

    }

    private List<CompanyDto> mapCompaniesToDto(List<Company> companies, Map<Long, UserDto> employeeMap) {
        if (CollectionUtils.isEmpty(companies)) {
            return Collections.emptyList();
        }
        List<CompanyDto> companyDtos = new ArrayList<>();
        for (Company company : companies) {
            List<UserDto> companyEmployees = new ArrayList<>();
            List<Long> currentEmployeeIds = company.getEmployeeIds(); // Предполагаем getEmployeeIds() -> List<Long>

            if (!CollectionUtils.isEmpty(currentEmployeeIds)) {
                for (Long empId : currentEmployeeIds) {
                    if (empId != null) {
                        UserDto employee = employeeMap.get(empId); // Ищем в карте
                        if (employee != null) {
                            companyEmployees.add(employee);
                        }
                    }
                }
            }
            CompanyDto dto = companyMapper.toCompanyDtoWithEmployees(company, companyEmployees);
            companyDtos.add(dto);
        }
        log.debug("Mapped {} companies to DTOs.", companyDtos.size());
        return companyDtos;

    }

    private Map<Long, UserDto> fetchAndMapEmployees(Set<Long> employeeIds, int pageNumber) {
        Map<Long, UserDto> employeeMap = new HashMap<>();
        if (CollectionUtils.isEmpty(employeeIds)) {
            log.debug("No employee IDs provided to fetch details for page {}.", pageNumber);
            return employeeMap;
        }
        log.info("Fetching employee details for {} IDs from page {}.", employeeIds.size(), pageNumber);
        List<UserDto> allEmployees = fetchEmployeesByIds(new ArrayList<>(employeeIds));
        for (UserDto employee : allEmployees) {
            if (employee != null && employee.id() != null) {
                employeeMap.put(employee.id(), employee);
            }
        }
        log.info("Successfully mapped {} employees out of {} requested for page {}.",
                employeeMap.size(), employeeIds.size(), pageNumber);
        return employeeMap;
    }

    private Set<Long> collectEmployeeIds(List<Company> companies) {
        Set<Long> allEmployeeIds = new HashSet<>();
        if (CollectionUtils.isEmpty(companies)) {
            return allEmployeeIds;
        }
        for (Company company : companies) {
            List<Long> employeeIds = company.getEmployeeIds();
            if (!CollectionUtils.isEmpty(employeeIds)) {
                for (Long empId : employeeIds) {
                    if (empId != null) {
                        allEmployeeIds.add(empId);
                    }
                }
            }
        }
        log.debug("Collected {} unique employee IDs from the list of companies.", allEmployeeIds.size());
        return allEmployeeIds;
    }


    @Override
    @Transactional(readOnly = true)
    public CompanyDto getCompanyById(Long id) {
        log.info("Fetching company with id: {}", id);
        Company company = findCompanyByIdOrThrow(id);
        List<UserDto> employees = fetchEmployeesByIds(company.getEmployeeIds());
        CompanyDto dto = companyMapper.toCompanyDtoWithEmployees(company, employees);
        log.info("Returning DTO for company ID: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public CompanyDto createCompany(CreateCompanyDto createCompanyDto) {
        log.info("Creating new company with name: {}", createCompanyDto.companyName());
        Company company = companyMapper.createCompanyDtoToCompany(createCompanyDto);
        Company savedCompany = companyRepository.save(company);
        log.info("Company created with id: {}", savedCompany.getId());
        return companyMapper.toCompanyDtoWithEmployees(savedCompany, Collections.emptyList());
    }

    @Override
    @Transactional
    public CompanyDto updateCompany(Long id, UpdateCompanyDto updateCompanyDto) {
        log.info("Updating company with id: {}", id);
        Company existingCompany = findCompanyByIdOrThrow(id);
        companyMapper.updateCompanyFromDto(updateCompanyDto, existingCompany);
        Company updatedCompany = companyRepository.save(existingCompany);
        log.info("Company updated with id: {}", updatedCompany.getId());
        List<UserDto> employees = fetchEmployeesByIds(updatedCompany.getEmployeeIds());
        CompanyDto dto = companyMapper.toCompanyDtoWithEmployees(updatedCompany, employees);
        log.info("Returning updated DTO for company ID: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public void deleteCompany(Long id) {
        log.info("Deleting company with id: {}", id);
        Company company = findCompanyByIdOrThrow(id);
        List<Long> employeeIds = new ArrayList<>(company.getEmployeeIds());
        if (!employeeIds.isEmpty()) {
            log.info("Notifying user-service to clear company link for {} employees of company {}", employeeIds.size(), id);
            for(Long empId : employeeIds) {
                notifyUserServiceOfCompanyChange(empId, null);
            }
        }
        companyRepository.delete(company);
        log.info("Company deleted with id: {}", id);
    }

    @Override
    @Transactional
    public CompanyDto addEmployeeToCompany(Long companyId, Long employeeId) {
        log.info("Adding employee {} to company {}", employeeId, companyId);
        Company company = findCompanyByIdOrThrow(companyId);
        boolean added = false;
        List<Long> employeeIdList = company.getEmployeeIds();
        if (!employeeIdList.contains(employeeId)) {
            employeeIdList.add(employeeId);
            companyRepository.save(company);
            added = true;
            log.info("Employee {} reference added locally to company {}", employeeId, companyId);
        } else {
            log.warn("Employee {} already exists in company {}", employeeId, companyId);
        }
        if (added) {
            notifyUserServiceOfCompanyChange(employeeId, companyId); // Используем хелпер
        }
        List<UserDto> employees = fetchEmployeesByIds(company.getEmployeeIds());
        CompanyDto dto = companyMapper.toCompanyDtoWithEmployees(company, employees);
        log.info("Returning DTO after attempting to add employee {} to company {}", employeeId, companyId);
        return dto;
    }

    @Override
    @Transactional
    public CompanyDto removeEmployeeFromCompany(Long companyId, Long employeeId) {
        log.info("Removing employee {} from company {}", employeeId, companyId);
        Company company = findCompanyByIdOrThrow(companyId);
        boolean removed = false;
        List<Long> employeeIdList = company.getEmployeeIds();
        if (employeeIdList.remove(employeeId)) {
            companyRepository.save(company);
            removed = true;
            log.info("Employee {} reference removed locally from company {}", employeeId, companyId);
        } else {
            log.warn("Employee {} not found in company {}", employeeId, companyId);
        }
        if (removed) {
            notifyUserServiceOfCompanyChange(employeeId, null);
        }
        List<UserDto> employees = fetchEmployeesByIds(company.getEmployeeIds());
        CompanyDto dto = companyMapper.toCompanyDtoWithEmployees(company, employees);
        log.info("Returning DTO after attempting to remove employee {} from company {}", employeeId, companyId);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyDto> getCompaniesByIds(List<Long> ids) {
        log.debug("Fetching companies by IDs: {}", ids);
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<Company> companies = companyRepository.findAllById(ids);
        return companies.stream()
                .map(companyMapper::toSimpleCompanyDto)
                .collect(Collectors.toList());
    }
    private Company findCompanyByIdOrThrow(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> {
                    log.warn("Company not found with id: {}", companyId);
                    return new ResourceNotFoundException("Company not found with id: " + companyId);
                });
    }
    private void notifyUserServiceOfCompanyChange(Long userId, Long companyId) {
        String action = (companyId == null) ? "clearing" : "setting";
        try {
            log.debug("Attempting to notify user-service about {} companyId ({}) for user {}", action, companyId, userId);
            ResponseEntity<Void> response = userServiceClient.setUserCompany(userId, companyId); // Передаем null если нужно
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("User service responded with status {} while {} company for user {}",
                        response.getStatusCode(), action, userId);
            } else {
                log.info("Successfully notified user-service about {} company for user {}", action, userId);
            }
        } catch (Exception e) {
            log.error("Failed to notify user-service about {} company for user {}: {}", action, userId, e.getMessage(), e);
        }
    }
}

