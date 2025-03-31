package max.iv.userservice.mapper;

import max.iv.userservice.DTO.CompanyDto;
import max.iv.userservice.DTO.CreateUserDto;
import max.iv.userservice.DTO.UpdateUserDto;
import max.iv.userservice.DTO.UserDto;
import max.iv.userservice.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "company", ignore = true)
    @Mapping(source = "id", target = "id")
    UserDto toUserDto(User user);
    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    @Mapping(source = "user.phoneNumber", target = "phoneNumber")
    @Mapping(source = "companyDto", target = "company")
    UserDto toUserDtoWithCompany(User user, CompanyDto companyDto);
    List<UserDto> toUserDtoList(List<User> users);
    @Mapping(target = "id", ignore = true)
    User createUserDtoToUser(CreateUserDto createUserDto);
    @Mapping(target = "id", ignore = true) // Не обновляем ID
    void updateUserFromDto(UpdateUserDto updateUserDto, @MappingTarget User user);
}
