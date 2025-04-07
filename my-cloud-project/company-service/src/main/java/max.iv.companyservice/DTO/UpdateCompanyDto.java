package max.iv.companyservice.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;


public record UpdateCompanyDto(
        @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
        String companyName,
        @Min(value = 0, message = "Company budget must be positive")
        BigDecimal budget
) {}
