package max.iv.companyservice.DTO;

import java.math.BigDecimal;

public record CreateCompanyDto(
        String companyName,
        BigDecimal budget
) {}
