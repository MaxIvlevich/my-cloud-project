package max.iv.companyservice.DTO;

import java.math.BigDecimal;


public record UpdateCompanyDto(
        String companyName,
        BigDecimal budget
) {}
