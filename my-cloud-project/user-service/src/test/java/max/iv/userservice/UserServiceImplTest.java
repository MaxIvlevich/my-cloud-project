package max.iv.userservice;
import static org.assertj.core.api.Assertions.assertThat;
import max.iv.userservice.DTO.CompanyDto;
import max.iv.userservice.DTO.UserDto;
import max.iv.userservice.client.CompanyServiceClient;
import max.iv.userservice.exception.ResourceNotFoundException;
import max.iv.userservice.mapper.UserMapper;
import max.iv.userservice.models.User;
import max.iv.userservice.repository.UserRepository;
import max.iv.userservice.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CompanyServiceClient companyServiceClient;
    @InjectMocks
    private UserServiceImpl userServiceImpl;

    private User user1;
    private User user2;
    private User userNoCompany;
    private CompanyDto company10;
    private CompanyDto company20;
    private UserDto userDto1;
    private UserDto userDto2;
    private UserDto userDtoNoCompany;
    Pageable expectedPageable;
    @BeforeEach
    void setUp() {
        company10 = new CompanyDto(10L, "Company Ten", BigDecimal.valueOf(10000));
        company20 = new CompanyDto(20L, "Company Twenty", BigDecimal.valueOf(10000));

        user1 = new User();
        user1.setId(1L);
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setCompanyId(10L);
        user1.setPhoneNumber("111");

        user2 = new User();
        user2.setId(2L);
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setCompanyId(20L);
        user2.setPhoneNumber("222");

        userNoCompany = new User();
        userNoCompany.setId(3L);
        userNoCompany.setFirstName("Peter");
        userNoCompany.setLastName("Jones");
        userNoCompany.setCompanyId(null);
        userNoCompany.setPhoneNumber("333");

        userDto1 = new UserDto(1L, "Max", "Iv", "111", company10);
        userDto2 = new UserDto(2L, "Ivan", "Ivanovich", "222", company20);
        userDtoNoCompany = new UserDto(3L, "Vasya", "Petrov", "333", null);

        int pageNumber = 0;
        int pageSize = 5;
        String sortField = "username";
        Sort.Direction sortDirection = Sort.Direction.ASC;
        Pageable expectedPageable = PageRequest.of(pageNumber, pageSize, sortDirection, sortField);
    }
        @Test
        @DisplayName("getAllUsers should return list of users with company details when users and companies exist")
        void getAllUsers_ShouldReturnUsersWithCompanies() {
            // Arrange
            List<User> users = List.of(user1, user2);
            List<Long> companyIds = List.of(10L, 20L);
            List<CompanyDto> companies = List.of(company10, company20);


            // Mock repository response
            when(userRepository.findAll()).thenReturn(users);
            // Mock company service client response
            // Use argThat for more flexible list matching if order isn't guaranteed or contains duplicates originally
            when(companyServiceClient.getCompaniesByIds(argThat(list -> list.containsAll(companyIds) && list.size() == companyIds.size())))
                    .thenReturn(companies);
            // Mock mapper responses
            when(userMapper.toUserDtoWithCompany(user1, company10)).thenReturn(userDto1);
            when(userMapper.toUserDtoWithCompany(user2, company20)).thenReturn(userDto2);

            // Act
            Page<UserDto> result = userServiceImpl.getAllUsers(expectedPageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(userDto1, userDto2);

            // Verify interactions
            verify(userRepository).findAll();
            verify(companyServiceClient).getCompaniesByIds(anyList());
            verify(userMapper).toUserDtoWithCompany(user1, company10);
            verify(userMapper).toUserDtoWithCompany(user2, company20);
        }
    @Test
    @DisplayName("getAllUsers should return list of users without company details if company service fails")
    void getAllUsers_ShouldReturnUsersWithoutCompaniesWhenClientFails() {
        // Arrange
        List<User> users = List.of(user1, userNoCompany); // User with and without company ID
        List<Long> companyIds = List.of(10L); // Only one company ID to fetch

        // Mock repository response
        when(userRepository.findAll()).thenReturn(users);
        // Mock company service client to throw an exception
        when(companyServiceClient.getCompaniesByIds(anyList())).thenThrow(new RuntimeException("Service unavailable"));

        // Mock mapper responses (expecting null for companyDto when fetch fails)
        UserDto userDto1WithoutCompany = new UserDto(1L, "Max", "Iv", "111", null);
        when(userMapper.toUserDtoWithCompany(user1, null)).thenReturn(userDto1WithoutCompany);
        when(userMapper.toUserDtoWithCompany(userNoCompany, null)).thenReturn(userDtoNoCompany);


        // Act
        Page<UserDto> result = userServiceImpl.getAllUsers(expectedPageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        // Check that DTOs are returned, but company info is null where applicable
        assertThat(result).containsExactlyInAnyOrder(userDto1WithoutCompany, userDtoNoCompany);

        // Verify interactions
        verify(userRepository).findAll();
        verify(companyServiceClient).getCompaniesByIds(argThat(list -> list.contains(10L) && list.size() == 1));
        verify(userMapper).toUserDtoWithCompany(user1, null);
        verify(userMapper).toUserDtoWithCompany(userNoCompany, null);
    }

    @Test
    @DisplayName("getAllUsers should return empty list when no users found")
    void getAllUsers_ShouldReturnEmptyListWhenNoUsers() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        Page<UserDto> result = userServiceImpl.getAllUsers(expectedPageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        // Verify that dependencies for enrichment were not called
        verify(userRepository).findAll();
        verifyNoInteractions(companyServiceClient);
        verifyNoInteractions(userMapper);
    }
    @Test
    @DisplayName("getUserById should return user with company details when user and company exist")
    void getUserById_ShouldReturnUserWithCompany() {
        // Arrange
        Long userId = 1L;
        Long companyId = 10L;

        // Mock repository response
        when(userRepository.findById(userId)).thenReturn(Optional.of(user1));
        // Mock company service client response
        when(companyServiceClient.getCompanyById(companyId)).thenReturn(company10);
        // Mock mapper response
        when(userMapper.toUserDtoWithCompany(user1, company10)).thenReturn(userDto1);

        // Act
        UserDto result = userServiceImpl.getUserById(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(userDto1); // Assuming DTOs have proper equals/hashCode

        // Verify interactions
        verify(userRepository).findById(userId);
        verify(companyServiceClient).getCompanyById(companyId);
        verify(userMapper).toUserDtoWithCompany(user1, company10);
    }

    @Test
    @DisplayName("getUserById should return user without company details when user has no company ID")
    void getUserById_ShouldReturnUserWithoutCompanyWhenIdIsNull() {
        // Arrange
        Long userId = 3L;

        // Mock repository response
        when(userRepository.findById(userId)).thenReturn(Optional.of(userNoCompany));
        // Mock mapper response (expecting null companyDto)
        when(userMapper.toUserDtoWithCompany(userNoCompany, null)).thenReturn(userDtoNoCompany);

        // Act
        UserDto result = userServiceImpl.getUserById(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(userDtoNoCompany);

        // Verify interactions
        verify(userRepository).findById(userId);
        // Verify company service was NOT called
        verify(companyServiceClient, never()).getCompanyById(anyLong());
        verify(userMapper).toUserDtoWithCompany(userNoCompany, null);
    }
    @Test
    @DisplayName("getUserById should return user without company details when company fetch fails")
    void getUserById_ShouldReturnUserWithoutCompanyWhenClientFails() {
        // Arrange
        Long userId = 1L;
        Long companyId = 10L;
        UserDto userDto1WithoutCompany = new UserDto(1L, "Max", "Iv", "111", null);


        // Mock repository response
        when(userRepository.findById(userId)).thenReturn(Optional.of(user1));
        // Mock company service client to throw exception
        when(companyServiceClient.getCompanyById(companyId)).thenThrow(new RuntimeException("Service unavailable"));
        // Mock mapper response (expecting null companyDto)
        when(userMapper.toUserDtoWithCompany(user1, null)).thenReturn(userDto1WithoutCompany);

        // Act
        UserDto result = userServiceImpl.getUserById(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(userDto1WithoutCompany); // Check it's the DTO without company info

        // Verify interactions
        verify(userRepository).findById(userId);
        verify(companyServiceClient).getCompanyById(companyId); // Verify it was called
        verify(userMapper).toUserDtoWithCompany(user1, null);
    }
    @Test
    @DisplayName("getUserById should throw ResourceNotFoundException when user not found")
    void getUserById_ShouldThrowExceptionWhenUserNotFound() {
        // Arrange
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userServiceImpl.getUserById(userId);
        });

        // Optional: check exception message
        assertThat(exception.getMessage()).isEqualTo("User not found with id: " + userId);

        // Verify repository was called, but others were not
        verify(userRepository).findById(userId);
        verifyNoInteractions(companyServiceClient);
        verifyNoInteractions(userMapper);
    }

}
