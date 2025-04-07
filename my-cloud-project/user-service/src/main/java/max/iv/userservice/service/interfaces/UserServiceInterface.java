package max.iv.userservice.service.interfaces;

import max.iv.userservice.DTO.CreateUserDto;
import max.iv.userservice.DTO.UpdateUserDto;
import max.iv.userservice.DTO.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserServiceInterface {
    @Transactional(readOnly = true)
    Page<UserDto> getAllUsers(Pageable pageable);

    @Transactional(readOnly = true)
    UserDto getUserById(Long id);

    @Transactional
    UserDto createUser(CreateUserDto createUserDto);

    @Transactional
    UserDto updateUser(Long id, UpdateUserDto updateUserDto);

    @Transactional
    void deleteUser(Long id);

    @Transactional(readOnly = true)
    List<UserDto> getUsersByIds(List<Long> ids);

    @Transactional
    void setUserCompany(Long userId, Long companyId);
}
