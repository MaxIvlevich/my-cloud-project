package max.iv.companyservice.DTO;

import java.math.BigDecimal;
import java.util.List;

public record CompanyDto(
        Long id,
        String companyName,
        BigDecimal budget,
        List<UserDto> employees
) {}
