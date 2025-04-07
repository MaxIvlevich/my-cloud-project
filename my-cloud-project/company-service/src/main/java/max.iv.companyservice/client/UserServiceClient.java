package max.iv.companyservice.client;
import max.iv.companyservice.DTO.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * A Feign client interface for interacting with the User Service API.
 * <p>
 * Defines methods to consume endpoints exposed by the user service, specifically under the
 * base path {@code /api/v1/users}. The actual service name (`user-service`) is resolved
 * from the configuration property `services.user.name`, with a default value of "user-service"
 * if the property is not set.
 * <p>
 * Feign automatically creates implementations for these methods to handle HTTP communication.
 */
@FeignClient(name = "${services.user.name:user-service}", path = "/api/v1/users")
public interface UserServiceClient {
    /**
     * Retrieves a list of user details based on a list of user IDs.
     * <p>
     * Sends a GET request to the {@code /api/v1/users/by-ids} endpoint of the user service.
     * The list of IDs is passed as a request parameter named "ids".
     *
     * @param ids A list of user IDs for which details are requested. Must not be null.
     *            An empty list will likely result in an empty list response from the user service.
     * @return A {@link List} of {@link UserDto} objects corresponding to the provided IDs.
     *         The list may be smaller than the input list if some IDs were not found by the user service.
     *         Returns an empty list if the input list is empty or no matching users are found.
     * @throws feign.FeignException If the downstream service call fails (e.g., network error, server error).
     */
    @GetMapping("/by-ids")
    List<UserDto> getUsersByIds(@RequestParam("ids") List<Long> ids);
    /**
     * Sets or clears the company associated with a specific user in the User Service.
     * <p>
     * Sends a PUT request to the {@code /api/v1/users/{userId}/company} endpoint.
     * The target user is identified by {@code userId} in the path.
     * The {@code companyId} is sent in the request body. The body is optional;
     * sending {@code null} or an empty body should signal the user service to clear the association.
     * <p>
     * Note: The original path annotation {@code @PutMapping("/api/v1/users/{userId}/company")}
     * contains the base path again. Assumed intended relative path is {@code "/{userId}/company"}.
     *
     * @param userId    The ID of the user whose company association is being modified.
     * @param companyId The ID of the company to associate with the user (sent in the request body).
     *                  Pass {@code null} to clear the user's company association. The request body is optional.
     * @return A {@link ResponseEntity<Void>}. The HTTP status code indicates the outcome
     *         (e.g., 200 OK for success, 4xx/5xx for errors like user not found or service issues).
     * @throws feign.FeignException If the downstream service call fails.
     */
    @PutMapping("/{userId}/company")
    ResponseEntity<Void> setUserCompany(@PathVariable("userId") Long userId, @RequestBody(required = false) Long companyId);
    /**
     * Retrieves the details of a single user by their unique ID from the User Service.
     * <p>
     * Sends a GET request to the {@code /api/v1/users/{userId}} endpoint.
     * Wrapping the result in {@link ResponseEntity} allows the caller to check the HTTP status code
     * (e.g., to handle 404 Not Found gracefully) before attempting to access the {@link UserDto}.
     * <p>
     * Note: The original path annotation {@code @GetMapping("/api/v1/users/{userId}")}
     * contains the base path again. Assumed intended relative path is {@code "/{userId}"}.
     *
     * @param userId The unique identifier of the user to retrieve.
     * @return A {@link ResponseEntity} containing the {@link UserDto} if the user is found (status 200 OK),
     *         or an appropriate error status (e.g., 404 Not Found) if the user does not exist or another error occurs.
     * @throws feign.FeignException If the downstream service call fails due to network or non-404 server issues.
     */
    @GetMapping("/{userId}")
    ResponseEntity<UserDto> getUserById(@PathVariable("userId") Long userId);

}
