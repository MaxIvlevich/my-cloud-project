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
    /**
     * Retrieves a list of all users, enriched with their associated company details.
     * <p>
     * Fetches all {@link User} entities from the repository and then uses
     * {@link #enrichUsersWithCompanies(List)} to fetch company information and map
     * the entities to {@link UserDto} objects.
     * This operation is performed within a read-only transaction.
     *
     * @return A list of {@link UserDto} objects representing all users.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        log.info("Fetching all users");
        List<User> users = userRepository.findAll();
        return enrichUsersWithCompanies(users);
    }
    /**
     * Enriches a list of User entities with their corresponding company information.
     * <p>
     * Collects unique company IDs from the users, fetches the corresponding {@link CompanyDto}
     * details in bulk using the {@link CompanyServiceClient}, and then maps each {@link User}
     * entity along with its fetched {@link CompanyDto} (if available) to a {@link UserDto}.
     * Handles cases where the user list is empty or no company IDs are present.
     * Logs warnings for company IDs that could not be found.
     *
     * @param users The list of {@link User} entities to enrich. Must not be null.
     * @return A list of {@link UserDto} objects, where each user is potentially enriched
     *         with {@link CompanyDto} details. Returns an empty list if the input list is empty.
     */
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
    /**
     * Retrieves a specific user by their ID, enriched with company details.
     * <p>
     * Finds the {@link User} entity by ID. If not found, throws a {@link ResourceNotFoundException}.
     * If found, uses {@link #mapUserToDtoWithCompany(User)} to fetch company details (if applicable)
     * and map the entity to a {@link UserDto}.
     * This operation is performed within a read-only transaction.
     *
     * @param id The ID of the user to retrieve.
     * @return The {@link UserDto} representing the found user.
     * @throws ResourceNotFoundException If no user exists with the provided ID.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapUserToDtoWithCompany(user);
    }
    /**
     * Maps a single User entity to a {@link UserDto}, fetching and including company details if available.
     * <p>
     * If the user has a non-null {@code companyId}, it attempts to fetch the corresponding
     * {@link CompanyDto} using the {@link CompanyServiceClient}. Any exceptions during fetching
     * are caught and logged, resulting in the company details not being included in the final DTO.
     * Finally, uses the {@link UserMapper} to convert the {@link User} and the potentially fetched
     * {@link CompanyDto} into a {@link UserDto}.
     *
     * @param user The {@link User} entity to map. Must not be null.
     * @return The corresponding {@link UserDto}, potentially including company information.
     */
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
    /**
     * Creates a new user based on the provided data.
     * <p>
     * If a {@code companyId} is provided in the {@code createUserDto}, it first verifies
     * that the company exists by calling the {@link CompanyServiceClient}. If the company
     * is not found, a {@link ResourceNotFoundException} is thrown.
     * Maps the {@link CreateUserDto} to a {@link User} entity, saves it using the repository,
     * and then maps the saved entity back to a {@link UserDto}, enriching it with company details.
     * This operation is performed within a transaction.
     *
     * @param createUserDto The DTO containing the details for the new user. Must not be null.
     * @return The {@link UserDto} representing the newly created user, including company details if applicable.
     * @throws ResourceNotFoundException If a {@code companyId} is provided in the DTO,
     *         but the corresponding company cannot be found via the {@link CompanyServiceClient}.
     */
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
        return mapUserToDtoWithCompany(savedUser);
    }
    /**
     * Updates an existing user with the provided data.
     * <p>
     * Finds the existing {@link User} by ID. If not found, throws {@link ResourceNotFoundException}.
     * If the {@code companyId} in {@code updateUserDto} is different from the existing user's
     * {@code companyId} and is not null, it validates the existence of the new company via
     * {@link CompanyServiceClient}. If the new company is not found, throws {@link ResourceNotFoundException}.
     * Updates the fields of the existing {@link User} entity using the data from {@link UpdateUserDto},
     * saves the updated entity, and maps the result back to a {@link UserDto} enriched with company details.
     * This operation is performed within a transaction.
     *
     * @param id The ID of the user to update.
     * @param updateUserDto The DTO containing the updated user details. Must not be null.
     * @return The {@link UserDto} representing the updated user.
     * @throws ResourceNotFoundException If no user exists with the provided ID, or if a new, non-null
     *         {@code companyId} is provided but the corresponding company cannot be found.
     */
    @Override
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
    /**
     * Deletes a user by their ID.
     * <p>
     * Checks if a user with the given ID exists. If not, throws {@link ResourceNotFoundException}.
     * If the user exists, deletes them from the repository.
     * This operation is performed within a transaction.
     *
     * @param id The ID of the user to delete.
     * @throws ResourceNotFoundException If no user exists with the provided ID.
     */
    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
    /**
     * Retrieves a list of users based on a list of IDs, enriched with their company details.
     * <p>
     * If the list of IDs is null or empty, returns an empty list immediately.
     * Otherwise, fetches all {@link User} entities matching the provided IDs from the repository.
     * Then uses {@link #enrichUsersWithCompanies(List)} to fetch company information and map
     * the found entities to {@link UserDto} objects.
     * This operation is performed within a read-only transaction.
     *
     * @param ids A list of user IDs to retrieve. Can be null or empty.
     * @return A list of {@link UserDto} objects for the found users, potentially empty if no IDs
     *         are provided or no users match the given IDs.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByIds(List<Long> ids) {
        log.info("Fetching users by ids: {}", ids);
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<User> users = userRepository.findByIdIn(ids);
        return enrichUsersWithCompanies(users);
    }
    /**
     * Sets or clears the associated company for a specific user.
     * <p>
     * Finds the user by {@code userId}. If not found, throws {@link ResourceNotFoundException}.
     * If a non-null {@code companyId} is provided, it validates the existence of the company
     * via {@link CompanyServiceClient}. If the company is not found, throws {@link ResourceNotFoundException}.
     * Updates the {@code companyId} field of the {@link User} entity and saves the changes.
     * If {@code companyId} is null, the user's company association is cleared.
     * This operation is performed within a transaction.
     *
     * @param userId The ID of the user whose company association is to be modified.
     * @param companyId The ID of the company to associate with the user, or {@code null} to clear the association.
     * @throws ResourceNotFoundException If the user with the given {@code userId} is not found,
     *         or if a non-null {@code companyId} is provided but the corresponding company cannot be found.
     */
    @Override
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
