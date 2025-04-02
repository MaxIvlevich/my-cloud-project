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
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
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
        log.info("Fetching all users");
        List<User> users = userRepository.findAll();
        return enrichUsersWithCompanies(users);
    }

    private List<UserDto> enrichUsersWithCompanies(List<User> users) {
        if (users.isEmpty()) {
            log.debug("User list is empty, returning empty DTO list.");
            return Collections.emptyList();
        }
        Set<Long> companyIds = users.stream()
                .map(User::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        log.debug("Collected unique company IDs to fetch: {}", companyIds);
        Map<Long, CompanyDto> companyMap = new HashMap<>();
        if (!companyIds.isEmpty()) {
            try {
                log.info("Fetching company details for IDs: {}", companyIds);
                List<CompanyDto> companies = companyServiceClient.getCompaniesByIds(new ArrayList<>(companyIds));
                companyMap = companies.stream()
                        .collect(Collectors.toMap(CompanyDto::id, Function.identity())); // Ключ - ID компании, Значение - CompanyDto

                log.info("Successfully fetched details for {} companies out of {} requested.", companyMap.size(), companyIds.size());
                if (companyMap.size() != companyIds.size()) {
                    Set<Long> foundIds = companyMap.keySet();
                    companyIds.removeIf(foundIds::contains);
                    log.warn("Could not find company details for IDs: {}", companyIds);
                }

            } catch (Exception e) {
                log.error("Failed to fetch company details for IDs {}: {}", companyIds, e.getMessage(), e);
            }
        } else {
            log.debug("No company IDs to fetch details for.");
        }

        Map<Long, CompanyDto> finalCompanyMap = companyMap;
        return users.stream()
                .map(user -> {
                    CompanyDto companyDto = finalCompanyMap.get(user.getCompanyId());

                    return userMapper.toUserDtoWithCompany(user, companyDto);
                })
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
        if (user.getCompanyId() != null) {
            try {
                log.debug("Fetching company details for single user {} with companyId {}", user.getId(), user.getCompanyId());
                companyDto = companyServiceClient.getCompanyById(user.getCompanyId());
            } catch (Exception e) {
                log.error("Failed to fetch company details for user {}: {}", user.getId(), e.getMessage(), e);
            }
        }
        return userMapper.toUserDtoWithCompany(user, companyDto);
    }

    @Transactional
    public UserDto createUser(CreateUserDto createUserDto) {
        if (createUserDto.companyId() != null) {
            try {
                companyServiceClient.getCompanyById(createUserDto.companyId());
            } catch (Exception e) {
                throw new ResourceNotFoundException("Cannot create user, company not found: " + createUserDto.companyId());
            }
        }
        User user = userMapper.createUserDtoToUser(createUserDto);
        User savedUser = userRepository.save(user);
        return mapUserToDtoWithCompany(savedUser);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserDto updateUserDto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (updateUserDto.companyId() != null && !updateUserDto.companyId().equals(existingUser.getCompanyId())) {
            try {
                companyServiceClient.getCompanyById(updateUserDto.companyId());
            } catch (Exception e) {
                throw new ResourceNotFoundException("Cannot update user, new company not found: " + updateUserDto.companyId());
            }
        }
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
        log.info("Fetching users by ids: {}", ids);
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<User> users = userRepository.findByIdIn(ids);
        return enrichUsersWithCompanies(users);
    }

    @Transactional
    public void setUserCompany(Long userId, Long companyId) {
        log.info("Setting companyId {} for user {}", companyId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (companyId != null) {
            try {
                companyServiceClient.getCompanyById(companyId);
                log.debug("Verified company {} exists.", companyId);
            } catch (Exception e) {
                log.error("Cannot set company for user {}, company {} not found or service error: {}", userId, companyId, e.getMessage());
                throw new ResourceNotFoundException("Cannot set user company, target company not found: " + companyId);
            }
        }
        user.setCompanyId(companyId);
        userRepository.save(user);
        log.info("Successfully updated companyId for user {}", userId);
    }
}
