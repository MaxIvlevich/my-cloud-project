package max.iv.userservice.client;

import max.iv.userservice.DTO.CompanyDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "${services.company.name:company-service}", path = "/api/v1/companies")
public interface CompanyServiceClient {

    @GetMapping("/by-ids")
    List<CompanyDto> getCompaniesByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("/{id}")
    CompanyDto getCompanyById(@PathVariable("id") Long id);

}
