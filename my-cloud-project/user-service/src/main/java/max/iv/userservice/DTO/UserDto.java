package max.iv.userservice.DTO;

public record UserDto (
        Long id,
        String firstName,
        String lastName,
        String phoneNumber,
        CompanyDto company
) {
}
