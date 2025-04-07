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

    @GetMapping
    public ResponseEntity<List<CompanyDto>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyDto> getCompanyById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(companyService.getCompanyById(id));
    }

    @PostMapping
    public ResponseEntity<CompanyDto> createCompany(@RequestBody CreateCompanyDto createCompanyDto) {
        CompanyDto createdCompany = companyService.createCompany(createCompanyDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyDto> updateCompany(@PathVariable("id") Long id, @RequestBody UpdateCompanyDto updateCompanyDto) {
        return ResponseEntity.ok(companyService.updateCompany(id, updateCompanyDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable("id") Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{companyId}/employees/{employeeId}")
    public ResponseEntity<CompanyDto> addEmployee(@PathVariable("companyId") Long companyId, @PathVariable("employeeId") Long employeeId) {
        return ResponseEntity.ok(companyService.addEmployeeToCompany(companyId, employeeId));
    }

    @DeleteMapping("/{companyId}/employees/{employeeId}")
    public ResponseEntity<CompanyDto> removeEmployee(@PathVariable("companyId") Long companyId, @PathVariable("employeeId") Long employeeId) {
        return ResponseEntity.ok(companyService.removeEmployeeFromCompany(companyId, employeeId));
    }

    @GetMapping("/by-ids")
    public ResponseEntity<List<CompanyDto>> getCompaniesByIds(@RequestParam("ids") List<Long> ids) {
        List<CompanyDto> companies = companyService.getCompaniesByIds(ids);
        return ResponseEntity.ok(companies);
    }
}
