package max.iv.userservice.DTO;

public record CreateUserDto (
    String firstName,
    String lastName,
    String phoneNumber,
    Long companyId

        ){
}
