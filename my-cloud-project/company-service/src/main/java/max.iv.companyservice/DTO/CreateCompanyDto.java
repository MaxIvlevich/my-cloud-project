package max.iv.companyservice.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateCompanyDto(
        @NotBlank(message = "Company name cannot be blank")
        String companyName,
        @Min(value = 0, message = "Company budget must be positive")
        BigDecimal budget
) {}
