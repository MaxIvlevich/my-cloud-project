package max.iv.companyservice.client;
import max.iv.companyservice.DTO.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "${services.user.name:user-service}", path = "/api/v1/users")
public interface UserServiceClient {
    @GetMapping("/by-ids")
    List<UserDto> getUsersByIds(@RequestParam("ids") List<Long> ids);
    @PutMapping("/api/v1/users/{userId}/company")
    ResponseEntity<Void> setUserCompany(@PathVariable("userId") Long userId, @RequestBody(required = false) Long companyId);
    @GetMapping("/api/v1/users/{userId}")
    ResponseEntity<UserDto> getUserById(@PathVariable("userId") Long userId);

}
