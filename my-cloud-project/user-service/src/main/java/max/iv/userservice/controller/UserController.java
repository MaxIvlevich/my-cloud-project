package max.iv.userservice.controller;

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
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceInterface userServiceImpl;


    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {

        Page<UserDto> userPage = userServiceImpl.getAllUsers(pageable);
        return ResponseEntity.ok(userPage);
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") Long id) {
        log.info("getUserById with id {}",id);
        return ResponseEntity.ok(userServiceImpl.getUserById(id));
    }


    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserDto createUserDto) {
        UserDto createdUser = userServiceImpl.createUser(createUserDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }


    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable("id") Long id, @RequestBody UpdateUserDto updateUserDto) {
        return ResponseEntity.ok(userServiceImpl.updateUser(id, updateUserDto));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        userServiceImpl.deleteUser(id);
        return ResponseEntity.noContent().build(); // Возвращаем 204 No Content
    }


    @GetMapping("/by-ids")
    public ResponseEntity<List<UserDto>> getUsersByIds(@RequestParam("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(userServiceImpl.getUsersByIds(ids));
    }


    @PutMapping("/{userId}/company")
    public ResponseEntity<Void> setUserCompany(
            @PathVariable("userId") Long userId,
            @RequestBody(required = false) Long companyId) {
        userServiceImpl.setUserCompany(userId, companyId);
        return ResponseEntity.ok().build();
    }


}
