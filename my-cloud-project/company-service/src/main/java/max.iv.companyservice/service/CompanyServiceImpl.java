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

    private List<UserDto> fetchEmployees(List<Long> employeeIds) {
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
        List<UserDto> employees = fetchEmployees(company.getEmployeeIds());
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
            return employeeMap; // Возвращаем пустую карту
        }

        log.info("Fetching employee details for {} IDs from page {}.", employeeIds.size(), pageNumber);
        try {
            List<UserDto> allEmployees = fetchEmployees(new ArrayList<>(employeeIds)); // Используем реальный вызов
            for (UserDto employee : allEmployees) {
                if (employee != null && employee.id() != null) {
                    employeeMap.put(employee.id(), employee);
                }
            }
            log.info("Successfully fetched details for {} employees out of {} requested for page {}.",
                    employeeMap.size(), employeeIds.size(), pageNumber);
            if (employeeMap.size() != employeeIds.size()) {
                Set<Long> foundIds = employeeMap.keySet();
                Set<Long> requestedIdsCopy = new HashSet<>(employeeIds);
                requestedIdsCopy.removeAll(foundIds);
                log.warn("Could not fetch details for employee IDs: {}", requestedIdsCopy);
            }
        } catch (Exception e) {
            log.error("Failed to fetch employee details for IDs {} from page {}: {}",
                    employeeIds, pageNumber, e.getMessage(), e);
        }
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
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
        return mapCompanyToDtoWithEmployees(company);
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
        Company existingCompany = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));

        companyMapper.updateCompanyFromDto(updateCompanyDto, existingCompany);
        Company updatedCompany = companyRepository.save(existingCompany);
        log.info("Company updated with id: {}", updatedCompany.getId());
        return mapCompanyToDtoWithEmployees(updatedCompany);
    }

    @Override
    @Transactional
    public void deleteCompany(Long id) {
        log.info("Deleting company with id: {}", id);
        if (!companyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Company not found with id: " + id);
        }
        companyRepository.deleteById(id);
        log.info("Company deleted with id: {}", id);
    }

    @Override
    @Transactional
    public CompanyDto addEmployeeToCompany(Long companyId, Long employeeId) {
        log.info("Adding employee {} to company {}", employeeId, companyId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));
        try {
            log.debug("Checking existence of employee with ID: {}", employeeId);
            userServiceClient.getUserById(employeeId);
            log.debug("Employee {} found via user-service.", employeeId);
        } catch (Exception e) {
            log.error("Error checking employee {} via user-service: {}", employeeId, e.getMessage(), e);
        }
        boolean added = false;
        if (!company.getEmployeeIds().contains(employeeId)) {
            company.getEmployeeIds().add(employeeId);
            companyRepository.save(company);
            added = true;
            log.info("Employee {} reference added locally to company {}", employeeId, companyId);
        } else {
            log.warn("Employee {} already exists in company {}", employeeId, companyId);
        }
        if (added) {
            try {
                log.debug("Attempting to set companyId {} for user {}", companyId, employeeId);
                ResponseEntity<Void> response = userServiceClient.setUserCompany(employeeId, companyId);
                if (!response.getStatusCode().is2xxSuccessful()) {

                    log.warn("User service responded with status {} while setting company for user {}",
                            response.getStatusCode(), employeeId);
                }
                log.info("Successfully notified user-service to set company for user {}", employeeId);
            } catch (Exception e) {
                log.error("Failed to notify user-service to set company for user {}: {}", employeeId, e.getMessage());

            }
        }


        return mapCompanyToDtoWithEmployees(company);
    }

    @Override
    @Transactional
    public CompanyDto removeEmployeeFromCompany(Long companyId, Long employeeId) {
        log.info("Removing employee {} from company {}", employeeId, companyId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));

        boolean removed = company.getEmployeeIds().remove(employeeId);
        if (removed) {
            companyRepository.save(company);
            log.info("Employee {} reference removed locally from company {}", employeeId, companyId);
            try {
                log.debug("Attempting to clear companyId for user {}", employeeId);
                ResponseEntity<Void> response = userServiceClient.setUserCompany(employeeId, null);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    log.warn("User service responded with status {} while clearing company for user {}",
                            response.getStatusCode(), employeeId);
                }
                log.info("Successfully notified user-service to clear company for user {}", employeeId);
            } catch (Exception e) {
                log.error("Failed to notify user-service to clear company for user {}: {}", employeeId, e.getMessage());
            }
        } else {
            log.warn("Employee {} not found in company {}", employeeId, companyId);
        }
        return mapCompanyToDtoWithEmployees(company);
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
}

