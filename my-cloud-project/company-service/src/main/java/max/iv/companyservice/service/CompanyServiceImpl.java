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
    /**
     * Fetches details for a list of employees using their IDs via the UserServiceClient.
     * <p>
     * Handles empty input lists and catches exceptions during the service call, logging errors
     * and returning an empty list in case of failure. It also logs a warning if not all
     * requested employee IDs could be found by the user service.
     *
     * @param employeeIds A list of employee (user) IDs to fetch.
     * @return A list of {@link UserDto} objects representing the fetched employees.
     *         Returns an empty list if the input list is empty or if the call fails.
     */
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

    /**
     * Maps a {@link Company} entity to a {@link CompanyDto}, enriching it with employee details.
     * <p>
     * It retrieves the list of employee IDs from the company entity, fetches the corresponding
     * {@link UserDto} details using {@link #fetchEmployees(List)}, and then uses the
     * {@link CompanyMapper} to combine the company entity and the fetched employee DTOs.
     *
     * @param company The {@link Company} entity to map. Must not be null.
     * @return A {@link CompanyDto} enriched with details of the company's employees.
     *         The employee list might be empty if fetching fails or the company has no employees.
     */
    private CompanyDto mapCompanyToDtoWithEmployees(Company company) {
        List<UserDto> employees = fetchEmployees(company.getEmployeeIds());
        return companyMapper.toCompanyDtoWithEmployees(company, employees);
    }

    /**
     * Retrieves a list of all companies, each enriched with details of their associated employees.
     * <p>
     * Fetches all {@link Company} entities, collects all unique employee IDs across all companies,
     * performs a single bulk fetch for all employee details using {@link #fetchEmployees(List)},
     * and then maps each company to a {@link CompanyDto}, associating the correct fetched employees.
     * This approach minimizes calls to the user service.
     * This operation is performed within a read-only transaction.
     *
     * @return A list of {@link CompanyDto} objects, each potentially containing a list of employee DTOs.
     *         Returns an empty list if no companies exist.
     */
    @Override
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
    /**
     * Retrieves a specific company by its ID, enriched with employee details.
     * <p>
     * Finds the {@link Company} entity by ID. If not found, throws {@link ResourceNotFoundException}.
     * If found, uses {@link #mapCompanyToDtoWithEmployees(Company)} to fetch employee details
     * and map the entity to a {@link CompanyDto}.
     * This operation is performed within a read-only transaction.
     *
     * @param id The ID of the company to retrieve.
     * @return The {@link CompanyDto} representing the found company, including employee details.
     * @throws ResourceNotFoundException If no company exists with the provided ID.
     */
    @Override
    @Transactional(readOnly = true)
    public CompanyDto getCompanyById(Long id) {
        log.info("Fetching company with id: {}", id);
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
        return mapCompanyToDtoWithEmployees(company);
    }
    /**
     * Creates a new company based on the provided data.
     * <p>
     * Maps the {@link CreateCompanyDto} to a {@link Company} entity, saves it using the repository,
     * and then maps the saved entity back to a {@link CompanyDto}. The newly created company
     * will initially have an empty list of employees. Employee associations are managed separately.
     * This operation is performed within a transaction.
     *
     * @param createCompanyDto The DTO containing the details for the new company. Must not be null.
     * @return The {@link CompanyDto} representing the newly created company (without employees initially).
     */
    @Override
    @Transactional
    public CompanyDto createCompany(CreateCompanyDto createCompanyDto) {
        log.info("Creating new company with name: {}", createCompanyDto.companyName());
        Company company = companyMapper.createCompanyDtoToCompany(createCompanyDto);
        Company savedCompany = companyRepository.save(company);
        log.info("Company created with id: {}", savedCompany.getId());
        return companyMapper.toCompanyDtoWithEmployees(savedCompany, Collections.emptyList());
    }
    /**
     * Updates an existing company's details (excluding employee list manipulation).
     * <p>
     * Finds the existing {@link Company} by ID. If not found, throws {@link ResourceNotFoundException}.
     * Updates the fields of the existing {@link Company} entity using the data from {@link UpdateCompanyDto}
     * via the {@link CompanyMapper#updateCompanyFromDto}. Saves the updated entity.
     * Finally, maps the updated entity back to a {@link CompanyDto}, enriching it with the
     * current list of associated employee details fetched from the user service.
     * This operation is performed within a transaction.
     *
     * @param id               The ID of the company to update.
     * @param updateCompanyDto The DTO containing the updated company details. Must not be null.
     * @return The {@link CompanyDto} representing the updated company, including current employee details.
     * @throws ResourceNotFoundException If no company exists with the provided ID.
     */
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
    /**
     * Deletes a company by its ID.
     * <p>
     * Checks if a company with the given ID exists. If not, throws {@link ResourceNotFoundException}.
     * If the company exists, deletes it from the repository.
     * **Note:** This operation currently only removes the company record. It does *not* automatically
     * notify the user service to clear the company association for the employees who belonged to
     * this company. This might lead to dangling references in the user service unless handled elsewhere.
     * This operation is performed within a transaction.
     *
     * @param id The ID of the company to delete.
     * @throws ResourceNotFoundException If no company exists with the provided ID.
     */
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
    /**
     * Adds a reference to an employee (user) to a company's list of employees.
     * <p>
     * Finds the company by ID. Checks if the employee already exists in the company's list.
     * If not, adds the employee ID to the list, saves the company entity, and then attempts
     * to notify the user service (via {@link UserServiceClient#setUserCompany}) to set the
     * company association for that user. Failures during the user service notification are logged
     * but do not roll back the local addition of the employee reference.
     * This method also performs a check for the employee's existence using the user service,
     * logging an error if the check fails, but proceeds with adding the reference locally.
     * This operation is performed within a transaction.
     *
     * @param companyId  The ID of the company to add the employee to.
     * @param employeeId The ID of the employee (user) to add.
     * @return The updated {@link CompanyDto} including the newly added employee's details (if fetchable).
     * @throws ResourceNotFoundException If the company with the given {@code companyId} is not found.
     */
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
    /**
     * Removes an employee reference from a company's list of employees.
     * <p>
     * Finds the company by ID. Removes the employee ID from the company's list if present,
     * saves the company entity, and then attempts to notify the user service
     * (via {@link UserServiceClient#setUserCompany}) to clear the company association for that user
     * by setting the company ID to null. Failures during the user service notification are logged
     * but do not roll back the local removal of the employee reference.
     * This operation is performed within a transaction.
     *
     * @param companyId  The ID of the company to remove the employee from.
     * @param employeeId The ID of the employee (user) to remove.
     * @return The updated {@link CompanyDto} reflecting the removal (employee details will be fetched again).
     * @throws ResourceNotFoundException If the company with the given {@code companyId} is not found.
     */
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
    /**
     * Retrieves a list of companies based on a list of IDs, returning *simple* DTOs.
     * <p>
     * This method is designed to return basic company information without fetching
     * associated employee details, making it suitable for scenarios where only core
     * company data is needed (e.g., populating dropdowns, internal service calls).
     * It uses {@link CompanyMapper#toSimpleCompanyDto} for the mapping.
     * This operation is performed within a read-only transaction.
     *
     * @param ids A list of company IDs to retrieve. Can be null or empty.
     * @return A list of {@link CompanyDto} objects containing only basic company information
     *         (likely excluding the employee list). Returns an empty list if the input is empty
     *         or no companies match the IDs.
     */
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

