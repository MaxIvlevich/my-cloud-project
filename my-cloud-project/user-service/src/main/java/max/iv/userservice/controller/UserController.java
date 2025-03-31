package max.iv.userservice.controller;

import lombok.RequiredArgsConstructor;
import max.iv.userservice.DTO.CreateUserDto;
import max.iv.userservice.DTO.UpdateUserDto;
import max.iv.userservice.DTO.UserDto;
import max.iv.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserDto createUserDto) {
        UserDto createdUser = userService.createUser(createUserDto);
        // Возвращаем 201 Created и ссылку на созданный ресурс (опционально)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UpdateUserDto updateUserDto) {
        return ResponseEntity.ok(userService.updateUser(id, updateUserDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build(); // Возвращаем 204 No Content
    }
    // Эндпоинт для company-service
    @GetMapping("/by-ids")
    public ResponseEntity<List<UserDto>> getUsersByIds(@RequestParam("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(userService.getUsersByIds(ids));
    }
}
