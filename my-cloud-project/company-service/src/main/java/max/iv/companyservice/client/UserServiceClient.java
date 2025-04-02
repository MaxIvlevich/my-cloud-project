package max.iv.companyservice.client;

import max.iv.companyservice.DTO.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient(name = "${services.user.name:user-service}", path = "/api/v1/users")
public interface UserServiceClient {
    @GetMapping("/by-ids")
    List<UserDto> getUsersByIds(@RequestParam("ids") List<Long> ids);


}
