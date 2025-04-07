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
import max.iv.userservice.service.interfaces.UserServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service class responsible for managing user data.
 * It interacts with the {@link UserRepository} for persistence,
 * uses {@link UserMapper} for DTO conversions, and communicates
 * with {@link CompanyServiceClient} to enrich user data with company information.
 * <p>
 * Logging is provided by SLF4J via Lombok's {@code @Slf4j}.
 * Dependencies are injected via constructor using Lombok's {@code @RequiredArgsConstructor}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserServiceInterface {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CompanyServiceClient companyServiceClient;

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        log.info("Fetching users page. Page number: {}, Page size: {}, Sort: {}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        Page<User> userPage = userRepository.findAll(pageable);
        List<User> usersOnPage = userPage.getContent();
        List<UserDto> enrichedUserDTOs = enrichUsersWithCompanies(usersOnPage);
        log.info("Successfully enriched {} users for page {}.", enrichedUserDTOs.size(), pageable.getPageNumber());

        return new PageImpl<>(enrichedUserDTOs, pageable, userPage.getTotalElements());
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
                        .collect(Collectors.toMap(CompanyDto::id, Function.identity()));

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

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            log.info("User found with ID: {}", id);
            return mapUserToDtoWithCompany(user);
        } else {
            log.warn("User not found with ID: {}", id);
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
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

    @Override
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
        log.info("Created new user with id {} and  name {}",savedUser.getId(),savedUser.getFirstName() );
        return mapUserToDtoWithCompany(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UpdateUserDto updateUserDto) {
        User existingUser =  findUserByIdOrThrow(id);
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

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Attempting to delete user with id: {}", id);
        User user = findUserByIdOrThrow(id);
        userRepository.delete(user);
        log.info("User with id {} successfully deleted", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByIds(List<Long> ids) {
        log.debug("Attempting to get users by IDs. Requested count: {}", ids == null ? "null" : ids.size());
        if (CollectionUtils.isEmpty(ids)) {
            log.info("Input ID list is empty or null. Returning empty list.");
            return Collections.emptyList();
        }
        log.debug("Fetching User entities from repository for {} IDs.", ids.size());
        List<User> users = userRepository.findByIdIn(ids);
        log.debug("Found {} User entities for {} requested IDs.", users.size(), ids.size());
        return enrichUsersWithCompanies(users);
    }

    @Override
    @Transactional
    public void setUserCompany(Long userId, Long companyId) {
        log.info("Setting companyId {} for user {}", companyId, userId);
        User user = findUserByIdOrThrow(userId);
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

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });
    }
}
