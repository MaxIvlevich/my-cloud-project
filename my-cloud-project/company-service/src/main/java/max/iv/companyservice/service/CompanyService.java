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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

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
            // Проверяем, все ли запрошенные ID вернулись (опционально)
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


    @Transactional(readOnly = true)
    public List<CompanyDto> getAllCompanies() {
        log.info("Fetching all companies");
        List<Company> companies = companyRepository.findAll();
        if (companies.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> allEmployeeIds = companies.stream()
                .map(Company::getEmployeeIds)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        Map<Long, UserDto> employeeMap = new HashMap<>();
        if (!allEmployeeIds.isEmpty()) {
            List<UserDto> allEmployees = fetchEmployees(new ArrayList<>(allEmployeeIds));
            employeeMap = allEmployees.stream()
                    .collect(Collectors.toMap(UserDto::id, Function.identity()));
        }
        Map<Long, UserDto> finalEmployeeMap = employeeMap; // Для лямбды
        return companies.stream()
                .map(company -> {
                    List<UserDto> companyEmployees = company.getEmployeeIds().stream()
                            .map(finalEmployeeMap::get)
                            .filter(Objects::nonNull)
                            .toList();
                    return companyMapper.toCompanyDtoWithEmployees(company, companyEmployees);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompanyDto getCompanyById(Long id) {
        log.info("Fetching company with id: {}", id);
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
        return mapCompanyToDtoWithEmployees(company);
    }

    @Transactional
    public CompanyDto createCompany(CreateCompanyDto createCompanyDto) {
        log.info("Creating new company with name: {}", createCompanyDto.companyName());
        Company company = companyMapper.createCompanyDtoToCompany(createCompanyDto);
        Company savedCompany = companyRepository.save(company);
        log.info("Company created with id: {}", savedCompany.getId());
        return companyMapper.toCompanyDtoWithEmployees(savedCompany, Collections.emptyList());
    }

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

    @Transactional
    public void deleteCompany(Long id) {
        log.info("Deleting company with id: {}", id);
        if (!companyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Company not found with id: " + id);
        }
        companyRepository.deleteById(id);
        log.info("Company deleted with id: {}", id);
    }

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
            //throw new ResourceNotFoundException("Employee not found:" + employeeId);
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

