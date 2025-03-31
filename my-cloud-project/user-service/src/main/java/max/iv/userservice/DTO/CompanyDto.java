package max.iv.userservice.DTO;

import java.math.BigDecimal;

public record CompanyDto ( Long id,
                           String companyName,
                           BigDecimal budget) {

}
