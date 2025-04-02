package max.iv.userservice.controller;

import lombok.RequiredArgsConstructor;
import max.iv.userservice.DTO.CreateUserDto;
import max.iv.userservice.DTO.UpdateUserDto;
import max.iv.userservice.DTO.UserDto;
import max.iv.userservice.service.interfaces.UserServiceInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * REST controller for managing user-related operations via the `/api/v1/users` endpoint.
 * <p>
 * Provides HTTP endpoints for creating, reading, updating, deleting users (CRUD),
 * retrieving users by multiple IDs, and managing user-company associations.
 * It delegates the business logic to the {@link UserServiceInterface}.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceInterface userServiceImpl;
    /**
     * Handles GET requests to retrieve all users.
     * <p>
     * Delegates to {@link UserServiceInterface#getAllUsers()} and wraps the result
     * in a {@link ResponseEntity} with HTTP status 200 OK.
     *
     * @return A {@link ResponseEntity} containing a list of {@link UserDto} and HTTP status 200 (OK).
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userServiceImpl.getAllUsers());
    }
    /**
     * Handles GET requests to retrieve a specific user by their ID.
     * <p>
     * Delegates to {@link UserServiceInterface#getUserById(Long)}. If the user is found,
     * wraps the {@link UserDto} in a {@link ResponseEntity} with HTTP status 200 OK.
     * (e.g., by a {@code @ControllerAdvice}) to return an appropriate error response (e.g., 404 Not Found).
     *
     * @param id The ID of the user to retrieve, extracted from the path variable.
     * @return A {@link ResponseEntity} containing the {@link UserDto} if found (HTTP status 200 OK),
     *         or an error response if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userServiceImpl.getUserById(id));
    }
    /**
     * Handles POST requests to create a new user.
     * <p>
     * Expects a {@link CreateUserDto} in the request body. Delegates user creation
     * to {@link UserServiceImpl#createUser(CreateUserDto)}. Returns the created
     * user's details ({@link UserDto}) in the response body with HTTP status 201 Created.
     * If creation fails (e.g., invalid company ID), the service layer should handle it,
     * potentially throwing an exception leading to an error response.
     *
     * @param createUserDto The DTO containing the details for the new user, deserialized from the request body.
     * @return A {@link ResponseEntity} containing the created {@link UserDto} and HTTP status 201 (Created).
     */
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserDto createUserDto) {
        UserDto createdUser = userServiceImpl.createUser(createUserDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
    /**
     * Handles PUT requests to update an existing user identified by ID.
     * <p>
     * Expects an {@link UpdateUserDto} in the request body containing the fields to update.
     * Delegates the update logic to {@link UserServiceImpl#updateUser(Long, UpdateUserDto)}.
     * Returns the updated user's details ({@link UserDto}) in the response body with
     * HTTP status 200 OK. Handles potential exceptions (e.g., user not found, invalid new company ID)
     * from the service layer, typically resulting in error responses.
     *
     * @param id            The ID of the user to update, extracted from the path variable.
     * @param updateUserDto The DTO containing the updated user data, deserialized from the request body.
     * @return A {@link ResponseEntity} containing the updated {@link UserDto} and HTTP status 200 (OK),
     *         or an error response if the update fails.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable("id") Long id, @RequestBody UpdateUserDto updateUserDto) {
        return ResponseEntity.ok(userServiceImpl.updateUser(id, updateUserDto));
    }
    /**
     * Handles DELETE requests to remove a user by their ID.
     * <p>
     * Delegates deletion to {@link UserServiceInterface#deleteUser(Long)}. Returns an empty
     * {@link ResponseEntity} with HTTP status 204 No Content upon successful deletion.
     * If the user to be deleted is not found, the service layer should throw an exception,
     * leading to an appropriate error response (e.g., 404 Not Found).
     *
     * @param id The ID of the user to delete, extracted from the path variable.
     * @return A {@link ResponseEntity} with HTTP status 204 (No Content) on success,
     *         or an error response if the user is not found.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        userServiceImpl.deleteUser(id);
        return ResponseEntity.noContent().build(); // Возвращаем 204 No Content
    }
    /**
     * Handles GET requests to retrieve multiple users based on a list of IDs.
     * <p>
     * This endpoint is useful for fetching details for a batch of users, potentially
     * used by other services (like a company service needing details of its employees).
     * Expects a list of user IDs as a request parameter (e.g., `/api/v1/users/by-ids?ids=1,2,3`).
     * Delegates fetching to {@link UserServiceInterface#getUsersByIds(List)}.
     * Returns a list of {@link UserDto} in a {@link ResponseEntity} with HTTP status 200 OK.
     * Returns an empty list if the input 'ids' parameter is null, empty, or no users match the IDs.
     *
     * @param ids A list of user IDs passed as a request parameter named "ids".
     * @return A {@link ResponseEntity} containing a list of found {@link UserDto}s and HTTP status 200 (OK).
     */
    @GetMapping("/by-ids")
    public ResponseEntity<List<UserDto>> getUsersByIds(@RequestParam("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(userServiceImpl.getUsersByIds(ids));
    }
    /**
     * Handles PUT requests to set or clear the company associated with a specific user.
     * <p>
     * The target user is identified by {@code userId} in the path.
     * The {@code companyId} is expected in the request body. The body is optional;
     * sending {@code null} or an empty body will clear the user's company association.
     * Delegates the logic to {@link UserServiceInterface#setUserCompany(Long, Long)}.
     * Returns an empty {@link ResponseEntity} with HTTP status 200 OK on success.
     * Handles potential exceptions (e.g., user not found, target company not found)
     * from the service layer, typically resulting in error responses.
     *
     * @param userId    The ID of the user whose company association is being modified.
     * @param companyId The ID of the company to associate with the user (from the request body).
     *                  This parameter is optional; {@code null} clears the association.
     * @return A {@link ResponseEntity} with HTTP status 200 (OK) on success, or an error response.
     */
    @PutMapping("/{userId}/company")
    public ResponseEntity<Void> setUserCompany(
            @PathVariable("userId") Long userId,
            @RequestBody(required = false) Long companyId) {
        userServiceImpl.setUserCompany(userId, companyId);
        return ResponseEntity.ok().build();
    }


}
