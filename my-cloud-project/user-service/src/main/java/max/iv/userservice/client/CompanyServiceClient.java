package max.iv.userservice.client;

import max.iv.userservice.DTO.CompanyDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface CompanyServiceClient {
    @GetExchange("/api/v1/companies/{id}")
    CompanyDto getCompanyById(@PathVariable Long id);
}
