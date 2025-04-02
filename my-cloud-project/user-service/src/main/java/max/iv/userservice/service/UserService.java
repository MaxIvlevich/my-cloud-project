package max.iv.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import max.iv.userservice.DTO.CompanyDto;
import max.iv.userservice.DTO.CreateUserDto;
import max.iv.userservice.DTO.UpdateUserDto;
import max.iv.userservice.DTO.UserDto;
import max.iv.userservice.client.CompanyServiceClient;
import max.iv.userservice.exception.ResourceNotFoundException;
import max.iv.userservice.mapper.UserMapper;
import max.iv.userservice.models.User;
import max.iv.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CompanyServiceClient companyServiceClient;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapUserToDtoWithCompany) // Используем хелпер метод
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapUserToDtoWithCompany(user);
    }
    private UserDto mapUserToDtoWithCompany(User user) {

        CompanyDto companyDto = null;
        try {
            companyDto = companyServiceClient.getCompanyById(user.getCompanyId());
        } catch (Exception e) {
            System.err.println("Failed to fetch company details for user " + user.getId() + ": " + e.getMessage());
        }
        return userMapper.toUserDtoWithCompany(user, companyDto);
    }


    @Transactional
    public UserDto createUser(CreateUserDto createUserDto) {
        // TODO: Добавить проверку, существует ли компания с createUserDto.companyId()
        //       Можно сделать вызов к companyServiceClient или положиться на constraints БД

        User user = userMapper.createUserDtoToUser(createUserDto);
        User savedUser = userRepository.save(user);
        return mapUserToDtoWithCompany(savedUser);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserDto updateUserDto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userMapper.updateUserFromDto(updateUserDto, existingUser);
        User updatedUser = userRepository.save(existingUser);
        return mapUserToDtoWithCompany(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByIds(List<Long> ids) {
        return userRepository.findByIdIn(ids).stream()
                .map(this::mapUserToDtoWithCompany)
                .collect(Collectors.toList());
    }
    @Transactional
    public void setUserCompany(Long userId, Long companyId) {
        log.info("Setting companyId {} for user {}", companyId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
         if (companyId != null) { try { companyServiceClient.getCompanyById(companyId);
         } catch(Exception e) {
             throw new ResourceNotFoundException("company not found:" + companyId);
         }
         }
        user.setCompanyId(companyId);
        userRepository.save(user);
        log.info("Successfully updated companyId for user {}", userId);
    }
}
