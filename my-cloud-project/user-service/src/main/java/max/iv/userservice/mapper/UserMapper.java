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
/**
 * Interface defining mapping operations between {@link User} domain entities and various User DTOs
 * ({@link UserDto}, {@link CreateUserDto}, {@link UpdateUserDto}).
 * <p>
 * Uses MapStruct for generating the implementation code. The {@code componentModel = "spring"}
 * attribute ensures that the generated implementation is a Spring bean and can be injected.
 * Also handles mapping involving {@link CompanyDto}.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {
    /**
     * Maps a {@link User} entity to a basic {@link UserDto}.
     * <p>
     * The {@code company} field in the resulting {@link UserDto} is explicitly ignored
     * by this mapping and will likely be {@code null}. Use {@link #toUserDtoWithCompany(User, CompanyDto)}
     * if company information is needed.
     * The user's ID is mapped directly.
     *
     * @param user The source {@link User} entity. Must not be null.
     * @return The mapped {@link UserDto} without company information.
     */
    @Mapping(target = "company", ignore = true)
    @Mapping(source = "id", target = "id")
    UserDto toUserDto(User user);
    /**
     * Maps a {@link User} entity and a {@link CompanyDto} to a combined {@link UserDto}.
     * <p>
     * This method takes both the user entity and its corresponding company data (as a DTO)
     * and combines them into a single {@link UserDto}. It maps the basic user fields
     * (id, firstName, lastName, phoneNumber) and assigns the provided {@code companyDto}
     * directly to the {@code company} field of the target {@link UserDto}.
     *
     * @param user       The source {@link User} entity. Must not be null.
     * @param companyDto The {@link CompanyDto} representing the user's company. Can be {@code null},
     *                   in which case the {@code company} field in the resulting DTO will be {@code null}.
     * @return The mapped {@link UserDto} including company information (if provided).
     */
    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    @Mapping(source = "user.phoneNumber", target = "phoneNumber")
    @Mapping(source = "companyDto", target = "company")
    UserDto toUserDtoWithCompany(User user, CompanyDto companyDto);
    /**
     * Maps a list of {@link User} entities to a list of {@link UserDto} objects.
     * <p>
     * MapStruct will typically delegate the mapping of each element in the list
     * to the {@link #toUserDto(User)} method. Therefore, the resulting {@link UserDto}
     * objects in the list will likely *not* contain company information.
     *
     * @param users The list of {@link User} entities to map. Must not be null.
     * @return A new list containing {@link UserDto} objects corresponding to the input entities.
     *         Returns an empty list if the input list is empty.
     */
    List<UserDto> toUserDtoList(List<User> users);
    /**
     * Maps a {@link CreateUserDto} to a new {@link User} entity.
     * <p>
     * This is used when creating a new user. The {@code id} field of the target {@link User} entity
     * is explicitly ignored because the ID is typically generated by the persistence layer
     * upon saving the new entity.
     *
     * @param createUserDto The DTO containing the data for the user to be created. Must not be null.
     * @return A new {@link User} entity populated with data from the DTO, ready to be persisted.
     */
    @Mapping(target = "id", ignore = true)
    User createUserDtoToUser(CreateUserDto createUserDto);

    /**
     * Updates an existing {@link User} entity with data from an {@link UpdateUserDto}.
     * <p>
     * This method applies changes from the {@code updateUserDto} to the provided {@code user} entity.
     * The {@code @MappingTarget} annotation indicates that the {@code user} parameter is the
     * object to be updated in place, rather than creating a new instance.
     * The {@code id} field is explicitly ignored to prevent changing the entity's identifier during an update.
     *
     * @param updateUserDto The DTO containing the updated user data. Must not be null.
     * @param user          The existing {@link User} entity (annotated with {@code @MappingTarget})
     *                      to be updated. Must not be null. Note: This object will be modified directly.
     */
    @Mapping(target = "id", ignore = true) // Не обновляем ID
    void updateUserFromDto(UpdateUserDto updateUserDto, @MappingTarget User user);
}
