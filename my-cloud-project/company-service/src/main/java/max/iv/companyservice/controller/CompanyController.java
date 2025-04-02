package max.iv.companyservice.controller;

import max.iv.companyservice.DTO.CompanyDto;
import max.iv.companyservice.DTO.CreateCompanyDto;
import max.iv.companyservice.DTO.UpdateCompanyDto;
import lombok.RequiredArgsConstructor;
import max.iv.companyservice.service.interfaces.CompanyServiceInterfase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
/**
 * REST controller for managing company-related operations via the `/api/v1/companies` endpoint.
 * <p>
 * Provides HTTP endpoints for CRUD operations on companies, managing employee associations
 * within a company, and retrieving companies by multiple IDs.
 * It delegates the business logic to the {@link CompanyServiceInterfase}.
 */
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyServiceInterfase companyService;

    /**
     * Handles GET requests to retrieve all companies, including their associated employee details.
     * <p>
     * Delegates to {@link CompanyServiceInterfase#getAllCompanies()} and wraps the result
     * in a {@link ResponseEntity} with HTTP status 200 OK. The returned DTOs are expected
     * to be enriched with employee information.
     *
     * @return A {@link ResponseEntity} containing a list of {@link CompanyDto} (with employees)
     *         and HTTP status 200 (OK).
     */
    @GetMapping
    public ResponseEntity<List<CompanyDto>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }
    /**
     * Handles GET requests to retrieve a specific company by its ID, including employee details.
     * <p>
     * Delegates to {@link CompanyServiceInterfase#getCompanyById(Long)}. If the company is found,
     * wraps the {@link CompanyDto} (including employees) in a {@link ResponseEntity} with
     * HTTP status 200 OK.
     * @param id The ID of the company to retrieve, extracted from the path variable.
     * @return A {@link ResponseEntity} containing the {@link CompanyDto} (with employees) if found (HTTP status 200 OK),
     *         or an error response if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CompanyDto> getCompanyById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(companyService.getCompanyById(id));
    }
    /**
     * Handles POST requests to create a new company.
     * <p>
     * Expects a {@link CreateCompanyDto} in the request body. Delegates company creation
     * to {@link CompanyServiceInterfase#createCompany(CreateCompanyDto)}. Returns the created
     * company's details ({@link CompanyDto}, initially without employees) in the response body
     * with HTTP status 201 Created.
     *
     * @param createCompanyDto The DTO containing the details for the new company, deserialized from the request body.
     * @return A {@link ResponseEntity} containing the created {@link CompanyDto} (without employees)
     *         and HTTP status 201 (Created).
     */
    @PostMapping
    public ResponseEntity<CompanyDto> createCompany(@RequestBody CreateCompanyDto createCompanyDto) {
        CompanyDto createdCompany = companyService.createCompany(createCompanyDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany);
    }
    /**
     * Handles PUT requests to update an existing company's details (excluding employee list).
     * <p>
     * Expects an {@link UpdateCompanyDto} in the request body containing the fields to update.
     * Delegates the update logic to {@link CompanyServiceInterfase#updateCompany(Long, UpdateCompanyDto)}.
     * Returns the updated company's details ({@link CompanyDto}, including current employees)
     * in the response body with HTTP status 200 OK. Handles potential exceptions (e.g., company not found)
     * from the service layer, typically resulting in error responses.
     *
     * @param id               The ID of the company to update, extracted from the path variable.
     * @param updateCompanyDto The DTO containing the updated company data, deserialized from the request body.
     * @return A {@link ResponseEntity} containing the updated {@link CompanyDto} (with employees)
     *         and HTTP status 200 (OK), or an error response if the update fails.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CompanyDto> updateCompany(@PathVariable("id") Long id, @RequestBody UpdateCompanyDto updateCompanyDto) {
        return ResponseEntity.ok(companyService.updateCompany(id, updateCompanyDto));
    }
    /**
     * Handles DELETE requests to remove a company by its ID.
     * <p>
     * Delegates deletion to {@link CompanyServiceInterfase#deleteCompany(Long)}. Returns an empty
     * {@link ResponseEntity} with HTTP status 204 No Content upon successful deletion.
     * If the company to be deleted is not found, the service layer should throw an exception,
     * leading to an appropriate error response (e.g., 404 Not Found).
     * Note: Check service implementation regarding handling of associated employee references in the User service.
     *
     * @param id The ID of the company to delete, extracted from the path variable.
     * @return A {@link ResponseEntity} with HTTP status 204 (No Content) on success,
     *         or an error response if the company is not found.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable("id") Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    } /**
     * Handles POST requests to associate an existing employee (user) with a company.
     * <p>
     * Delegates the logic to {@link CompanyServiceInterfase#addEmployeeToCompany(Long, Long)}.
     * This typically involves adding the employee's ID to the company's list and potentially
     * notifying the user service. Returns the updated company details ({@link CompanyDto},
     * including the newly added employee) with HTTP status 200 OK.
     * Handles potential errors like company not found or issues during interaction with the user service.
     *
     * @param companyId  The ID of the company, extracted from the path variable.
     * @param employeeId The ID of the employee (user) to associate, extracted from the path variable.
     * @return A {@link ResponseEntity} containing the updated {@link CompanyDto} (with employees)
     *         and HTTP status 200 (OK), or an error response.
     */
    @PostMapping("/{companyId}/employees/{employeeId}")
    public ResponseEntity<CompanyDto> addEmployee(@PathVariable("companyId") Long companyId, @PathVariable("employeeId") Long employeeId) {
        return ResponseEntity.ok(companyService.addEmployeeToCompany(companyId, employeeId));
    }
    /**
     * Handles DELETE requests to remove the association between an employee (user) and a company.
     * <p>
     * Delegates the logic to {@link CompanyServiceInterfase#removeEmployeeFromCompany(Long, Long)}.
     * This typically involves removing the employee's ID from the company's list and potentially
     * notifying the user service to clear the user's company link. Returns the updated company
     * details ({@link CompanyDto}, reflecting the removal) with HTTP status 200 OK.
     * Handles potential errors like company not found or the employee not being associated with the company.
     *
     * @param companyId  The ID of the company, extracted from the path variable.
     * @param employeeId The ID of the employee (user) to disassociate, extracted from the path variable.
     * @return A {@link ResponseEntity} containing the updated {@link CompanyDto} (with remaining employees)
     *         and HTTP status 200 (OK), or an error response.
     */
    @DeleteMapping("/{companyId}/employees/{employeeId}")
    public ResponseEntity<CompanyDto> removeEmployee(@PathVariable("companyId") Long companyId, @PathVariable("employeeId") Long employeeId) {
        return ResponseEntity.ok(companyService.removeEmployeeFromCompany(companyId, employeeId));
    }
    /**
     * Handles GET requests to retrieve multiple companies based on a list of IDs, returning simple details.
     * <p>
     * Delegates fetching to {@link CompanyServiceInterfase#getCompaniesByIds(List)}. This endpoint
     * is expected to return basic company information (likely *without* employee lists) suitable
     * for scenarios like populating selection lists or internal service communication where full details
     * are not required. Expects IDs as a request parameter (e.g., `/api/v1/companies/by-ids?ids=1,2,3`).
     *
     * @param ids A list of company IDs passed as a request parameter named "ids".
     * @return A {@link ResponseEntity} containing a list of found {@link CompanyDto}s (simple representation)
     *         and HTTP status 200 (OK). Returns an empty list if the input 'ids' parameter is null,
     *         empty, or no companies match the IDs.
     */
    @GetMapping("/by-ids")
    public ResponseEntity<List<CompanyDto>> getCompaniesByIds(@RequestParam("ids") List<Long> ids) {
        List<CompanyDto> companies = companyService.getCompaniesByIds(ids);
        return ResponseEntity.ok(companies);
    }
}
