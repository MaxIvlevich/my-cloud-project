package max.iv.userservice.DTO;

public record UpdateUserDto(
                String firstName,
                String lastName,
                String phoneNumber,
                Long companyId
        ){
}
