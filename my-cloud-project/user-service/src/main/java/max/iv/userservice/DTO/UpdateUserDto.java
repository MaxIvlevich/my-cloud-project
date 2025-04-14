package max.iv.userservice.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateUserDto(
        @Size(min = 3, max = 50, message = "firstName must be between 3 and 50 characters")
        String firstName,
        @Size(min = 3, max = 50, message = "lastName must be between 3 and 50 characters")
        String lastName,
        @Size(max= 11, message = "The phone number must not exceed 11 characters long")
        String phoneNumber,
        @Min(value = 1, message = "Company ID must be positive")
        Long companyId
) {
}
