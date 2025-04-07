package max.iv.companyservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import max.iv.companyservice.DTO.CompanyDto;
import max.iv.companyservice.DTO.CreateCompanyDto;
import max.iv.companyservice.DTO.UpdateCompanyDto;
import lombok.RequiredArgsConstructor;
import max.iv.companyservice.service.interfaces.CompanyServiceInterfase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@Slf4j
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyServiceInterfase companyService;

    @GetMapping
    public ResponseEntity<Page<CompanyDto>> getAllCompanies(Pageable pageable) {
        log.info("getAllCompanies request received. Page: {}, Size: {}, Sort: {}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        Page<CompanyDto> companyPage = companyService.getAllCompanies(pageable);
        return ResponseEntity.ok(companyPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyDto> getCompanyById(
            @PathVariable("id") @Min(value = 1, message = "Company ID must be positive") Long id) {
        log.info("getCompanyById request for ID: {}", id);
        return ResponseEntity.ok(companyService.getCompanyById(id));
    }

    @PostMapping
    public ResponseEntity<CompanyDto> createCompany(
            @RequestBody  @Valid CreateCompanyDto createCompanyDto) {
        log.info("createCompany request received with name: {}", createCompanyDto.companyName());
        CompanyDto createdCompany = companyService.createCompany(createCompanyDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyDto> updateCompany(
            @PathVariable("id")  @Min(value = 1, message = "Company ID must be positive") Long id,
            @RequestBody @Valid UpdateCompanyDto updateCompanyDto) {
        log.info("updateCompany request for ID: {}", id);
        return ResponseEntity.ok(companyService.updateCompany(id, updateCompanyDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(
            @PathVariable("id") @Min(value = 1, message = "Company ID must be positive") Long id) {
        companyService.deleteCompany(id);
        log.info("deleteCompany request for ID: {}", id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{companyId}/employees/{employeeId}")
    public ResponseEntity<CompanyDto> addEmployee(
            @PathVariable("companyId")  @Min(value = 1, message = "Company ID must be positive")Long companyId,
            @PathVariable("employeeId") @Min(value = 1, message = "Employee ID must be positive") Long employeeId) {
        log.info("addEmployee request for companyId: {}, employeeId: {}", companyId, employeeId);
        return ResponseEntity.ok(companyService.addEmployeeToCompany(companyId, employeeId));
    }

    @DeleteMapping("/{companyId}/employees/{employeeId}")
    public ResponseEntity<CompanyDto> removeEmployee(
            @PathVariable("companyId")  @Min(value = 1, message = "Company ID must be positive") Long companyId,
            @PathVariable("employeeId") @Min(value = 1, message = "Employee ID must be positive") Long employeeId) {
        log.info("removeEmployee request for companyId: {}, employeeId: {}", companyId, employeeId);
        return ResponseEntity.ok(companyService.removeEmployeeFromCompany(companyId, employeeId));
    }

    @GetMapping("/by-ids")
    public ResponseEntity<List<CompanyDto>> getCompaniesByIds(
            @RequestParam("ids") @NotEmpty(message = "ID list cannot be empty")
            @Size(max = 100, message = "Cannot request more than 100 IDs at once")
            List<@NotNull @Min(value = 1, message = "Company ID in list must be positive") Long> ids) {
        List<CompanyDto> companies = companyService.getCompaniesByIds(ids);
        return ResponseEntity.ok(companies);
    }
}
