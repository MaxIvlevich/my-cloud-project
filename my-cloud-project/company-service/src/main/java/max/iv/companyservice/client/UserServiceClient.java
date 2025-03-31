package max.iv.companyservice.client;

import max.iv.companyservice.DTO.UserDto;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

// Интерфейс для вызова user-service
public interface UserServiceClient {
    @GetExchange("/api/v1/users/by-ids")
    List<UserDto> getUsersByIds(@RequestParam("ids") List<Long> ids);
}
