package max.iv.userservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import max.iv.userservice.DTO.CreateUserDto;
import max.iv.userservice.DTO.UpdateUserDto;
import max.iv.userservice.DTO.UserDto;
import max.iv.userservice.service.interfaces.UserServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing user-related operations via the `/api/v1/users` endpoint.
 * <p>
 * Provides HTTP endpoints for creating, reading, updating, deleting users (CRUD),
 * retrieving users by multiple IDs, and managing user-company associations.
 * It delegates the business logic to the {@link UserServiceInterface}.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceInterface userServiceImpl;


    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        log.info("getAllUsers request with pageable: {}", pageable);
        Page<UserDto> userPage = userServiceImpl.getAllUsers(pageable);
        return ResponseEntity.ok(userPage);
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(
            @PathVariable("id")
            @Min(value = 1, message = "User ID must be positive")
            Long id) {
        log.info("getUserById request with id {}", id);
        return ResponseEntity.ok(userServiceImpl.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody @Valid CreateUserDto createUserDto) {
        log.info("createUser request received");
        UserDto createdUser = userServiceImpl.createUser(createUserDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable("id") @Min(value = 1, message = "User ID must be positive") Long id,
            @RequestBody @Valid UpdateUserDto updateUserDto) {
        log.info("updateUser request with id {}", id);
        return ResponseEntity.ok(userServiceImpl.updateUser(id, updateUserDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable("id") @Min(value = 1, message = "User ID must be positive") Long id) {
        log.info("deleteUser request with id {} ", id);
        userServiceImpl.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-ids")
    public ResponseEntity<List<UserDto>> getUsersByIds(
            @RequestParam("ids") @NotEmpty(message = "ID list cannot be empty")
            @Size(max = 100, message = "Cannot request more than 100 IDs at once")
            List<@Min(value = 1, message = "User ID in list must be positive") Long> ids) {
        log.info("getUsersByIds request for {} IDs", ids.size());
        return ResponseEntity.ok(userServiceImpl.getUsersByIds(ids));
    }

    @PutMapping("/{userId}/company")
    public ResponseEntity<Void> setUserCompany(
            @PathVariable("userId") @Min(value = 1, message = "User ID must be positive") Long userId,
            @RequestBody(required = false) @Min(value = 1, message = "Company ID must be positive if provided") Long companyId) {
        log.info("setUserCompany request for userId: {}, companyId: {}", userId, companyId != null ? companyId : "null (remove association)");
        userServiceImpl.setUserCompany(userId, companyId);
        return ResponseEntity.ok().build();
    }
}
