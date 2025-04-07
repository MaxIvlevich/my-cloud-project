package max.iv.userservice.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserDto(
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String firstName,
        @NotBlank(message = "lastName cannot be blank")
        @Size(min = 3, max = 50, message = "lastName must be between 3 and 50 characters")
        String lastName,
        @NotBlank(message = "phoneNumber cannot be blank")
        @Size(max= 11, message = "The phone number must not exceed 11 characters long")
        String phoneNumber,
        @Min(value = 1, message = "Company ID must be positive")
        Long companyId

) {
}
