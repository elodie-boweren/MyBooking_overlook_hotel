package com.MyBooking.auth.service;

import com.MyBooking.auth.domain.NotificationPreference;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.repository.NotificationPreferenceRepository;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import com.MyBooking.common.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtService jwtService;
    
    @InjectMocks
    private AuthService authService;
    
    private User testUser;
    private User testEmployee;
    private User testAdmin;
    
    @BeforeEach
    void setUp() {
        // Setup test users
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("client@test.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPhone("+1234567890");
        testUser.setAddress("123 Test St");
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));
        testUser.setRole(Role.CLIENT);
        
        testEmployee = new User();
        testEmployee.setId(2L);
        testEmployee.setEmail("employee@test.com");
        testEmployee.setPassword("encodedPassword");
        testEmployee.setFirstName("Jane");
        testEmployee.setLastName("Smith");
        testEmployee.setPhone("+1234567891");
        testEmployee.setAddress("456 Employee St");
        testEmployee.setBirthDate(LocalDate.of(1985, 5, 15));
        testEmployee.setRole(Role.EMPLOYEE);
        
        testAdmin = new User();
        testAdmin.setId(3L);
        testAdmin.setEmail("admin@test.com");
        testAdmin.setPassword("encodedPassword");
        testAdmin.setFirstName("Admin");
        testAdmin.setLastName("User");
        testAdmin.setPhone("+1234567892");
        testAdmin.setAddress("789 Admin St");
        testAdmin.setBirthDate(LocalDate.of(1980, 10, 20));
        testAdmin.setRole(Role.ADMIN);
    }

    // ========== USER REGISTRATION TESTS ==========
    
    @Test
    void registerUser_WithValidData_ShouldCreateUserAndReturnSavedUser() {
        // Given
        String email = "newuser@test.com";
        String password = "password123";
        String firstName = "New";
        String lastName = "User";
        String phone = "+1234567893";
        String address = "999 New St";
        LocalDate birthDate = LocalDate.of(1995, 3, 10);
        
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
            .thenReturn(new NotificationPreference());
        
        // When
        User result = authService.registerUser(email, password, firstName, lastName, phone, address, birthDate);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("client@test.com");
        assertThat(result.getRole()).isEqualTo(Role.CLIENT);
        
        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
        verify(notificationPreferenceRepository, times(2)).save(any(NotificationPreference.class)); // RESERVATION + FEEDBACK
    }
    
    @Test
    void registerUser_WithExistingEmail_ShouldThrowBusinessRuleException() {
        // Given
        String email = "existing@test.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> authService.registerUser(email, "password", "First", "Last", "+1234567890", "Address", LocalDate.now()))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Email already exists: " + email);
        
        verify(userRepository).existsByEmail(email);
        verify(userRepository, never()).save(any(User.class));
    }
    

    // ========== AUTHENTICATION TESTS ==========
    
    @Test
    void authenticateUser_WithValidCredentials_ShouldReturnJwtToken() {
        // Given
        String email = "client@test.com";
        String password = "password123";
        String expectedToken = "jwt-token-123";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole().toString())).thenReturn(expectedToken);
        
        // When
        String result = authService.authenticateUser(email, password);
        
        // Then
        assertThat(result).isEqualTo(expectedToken);
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, testUser.getPassword());
        verify(jwtService).generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole().toString());
    }
    
    @Test
    void authenticateUser_WithInvalidPassword_ShouldThrowBusinessRuleException() {
        // Given
        String email = "client@test.com";
        String password = "wrongpassword";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> authService.authenticateUser(email, password))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Invalid password");
        
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, testUser.getPassword());
        verify(jwtService, never()).generateToken(any(), any(), any());
    }
    
    @Test
    void authenticateUser_WithNonExistentUser_ShouldThrowNotFoundException() {
        // Given
        String email = "nonexistent@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> authService.authenticateUser(email, "password"))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("User not found with email: " + email);
        
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    // ========== PROFILE MANAGEMENT TESTS ==========
    
    @Test
    void getUserProfile_WithValidEmail_ShouldReturnUser() {
        // Given
        String email = "client@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        
        // When
        User result = authService.getUserProfile(email);
        
        // Then
        assertThat(result).isEqualTo(testUser);
        verify(userRepository).findByEmail(email);
    }
    
    @Test
    void getUserProfile_WithInvalidEmail_ShouldThrowNotFoundException() {
        // Given
        String email = "nonexistent@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> authService.getUserProfile(email))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("User not found with email: " + email);
    }
    
    @Test
    void updateUserProfile_WithValidData_ShouldUpdateAndReturnUser() {
        // Given
        String email = "client@test.com";
        String newFirstName = "Updated";
        String newLastName = "Name";
        String newPhone = "+9876543210";
        String newAddress = "New Address";
        LocalDate newBirthDate = LocalDate.of(1992, 6, 15);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        User result = authService.updateUserProfile(email, newFirstName, newLastName, newPhone, newAddress, newBirthDate);
        
        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findByEmail(email);
        verify(userRepository).save(testUser);
    }

    // ========== PASSWORD MANAGEMENT TESTS ==========
    
    @Test
    void changePassword_WithValidCurrentPassword_ShouldUpdatePassword() {
        // Given
        String email = "client@test.com";
        String currentPassword = "currentPassword";
        String newPassword = "newPassword";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        authService.changePassword(email, currentPassword, newPassword);
        
        // Then
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(currentPassword, "encodedPassword");
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
    }
    
    @Test
    void changePassword_WithInvalidCurrentPassword_ShouldThrowBusinessRuleException() {
        // Given
        String email = "client@test.com";
        String currentPassword = "wrongPassword";
        String newPassword = "newPassword";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> authService.changePassword(email, currentPassword, newPassword))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Current password is incorrect");
        
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(currentPassword, testUser.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void resetPassword_WithValidEmail_ShouldUpdatePassword() {
        // Given
        String email = "client@test.com";
        String newPassword = "resetPassword";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("resetEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        authService.resetPassword(email, newPassword);
        
        // Then
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
    }

    // ========== NOTIFICATION PREFERENCES TESTS ==========
    
    @Test
    void getUserNotificationPreferences_WithValidUser_ShouldReturnPreferences() {
        // Given
        String email = "client@test.com";
        List<NotificationPreference> preferences = Arrays.asList(
            new NotificationPreference(),
            new NotificationPreference()
        );
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(notificationPreferenceRepository.findByUser(testUser)).thenReturn(preferences);
        
        // When
        List<NotificationPreference> result = authService.getUserNotificationPreferences(email);
        
        // Then
        assertThat(result).hasSize(2);
        verify(userRepository).findByEmail(email);
        verify(notificationPreferenceRepository).findByUser(testUser);
    }
    
    @Test
    void updateNotificationPreference_WithExistingPreference_ShouldUpdatePreference() {
        // Given
        String email = "client@test.com";
        String notificationType = "RESERVATION";
        Boolean emailEnabled = true;
        Boolean smsEnabled = false;
        Boolean pushEnabled = true;
        
        NotificationPreference existingPreference = new NotificationPreference();
        existingPreference.setEmailEnabled(false);
        existingPreference.setSmsEnabled(false);
        existingPreference.setPushEnabled(false);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(userRepository.findByIdForUpdate(testUser.getId())).thenReturn(Optional.of(testUser));
        when(notificationPreferenceRepository.findByUserAndNotificationType(testUser, notificationType))
            .thenReturn(Optional.of(existingPreference));
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
            .thenReturn(existingPreference);
        
        // When
        NotificationPreference result = authService.updateNotificationPreference(
            email, notificationType, emailEnabled, smsEnabled, pushEnabled);
        
        // Then
        assertThat(result).isNotNull();
        verify(notificationPreferenceRepository).findByUserAndNotificationType(testUser, notificationType);
        verify(notificationPreferenceRepository).save(existingPreference);
    }

    // ========== ADMIN FUNCTION TESTS ==========
    
    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Given
        List<User> allUsers = Arrays.asList(testUser, testEmployee, testAdmin);
        when(userRepository.findAll()).thenReturn(allUsers);
        
        // When
        List<User> result = authService.getAllUsers();
        
        // Then
        assertThat(result).hasSize(3);
        assertThat(result).contains(testUser, testEmployee, testAdmin);
        verify(userRepository).findAll();
    }
    
    @Test
    void getUsersByRole_WithClientRole_ShouldReturnClientUsers() {
        // Given
        List<User> clientUsers = Arrays.asList(testUser);
        when(userRepository.findByRole(Role.CLIENT)).thenReturn(clientUsers);
        
        // When
        List<User> result = authService.getUsersByRole(Role.CLIENT);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(testUser);
        verify(userRepository).findByRole(Role.CLIENT);
    }
}