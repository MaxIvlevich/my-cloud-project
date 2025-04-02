package max.iv.userservice.client;

import max.iv.userservice.DTO.CompanyDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
/**
 * A Feign client interface for interacting with the Company Service API.
 * <p>
 * This client defines methods to consume endpoints exposed by the company service,
 * specifically under the base path {@code /api/v1/companies}. The actual service name
 * (`company-service`) is resolved from the configuration property `services.company.name`,
 * with a default value of "company-service" if the property is not set.
 * <p>
 * Feign handles the implementation of this interface, creating proxies that make
 * HTTP requests to the specified service endpoints.
 */
@FeignClient(name = "${services.company.name:company-service}", path = "/api/v1/companies")
public interface CompanyServiceClient {
    /**
     * Retrieves a list of company details based on a list of company IDs.
     * <p>
     * Sends a GET request to the {@code /api/v1/companies/by-ids} endpoint of the company service.
     * The list of IDs is passed as a request parameter named "ids".
     *
     * @param ids A list of company IDs for which details are requested. Must not be null.
     *            An empty list will likely result in an empty list response.
     * @return A {@link List} of {@link CompanyDto} objects corresponding to the provided IDs.
     *         The list may be smaller than the input list if some IDs were not found.
     *         Returns an empty list if the input list is empty or no matching companies are found.
     * @throws feign.FeignException If the downstream service call fails (e.g., network error, server error).
     */
    @GetMapping("/by-ids")
    List<CompanyDto> getCompaniesByIds(@RequestParam("ids") List<Long> ids);
    /**
     * Retrieves the details of a single company by its unique ID.
     * <p>
     * Sends a GET request to the {@code /api/v1/companies/{id}} endpoint of the company service,
     * where {id} is replaced by the provided company ID.
     *
     * @param id The unique identifier of the company to retrieve.
     * @return The {@link CompanyDto} containing the details of the requested company.
     * @throws feign.FeignException If the downstream service call fails. This could include cases
     *                              where the company with the specified ID is not found (often resulting
     *                              in a 404 status code wrapped in the exception), or other server/network errors.
     */
    @GetMapping("/{id}")
    CompanyDto getCompanyById(@PathVariable("id") Long id);

}
