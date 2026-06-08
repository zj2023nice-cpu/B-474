package com.example.lab.service;

import com.example.lab.entity.User;
import com.example.lab.repository.UserRepository;
import com.example.lab.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("hashedPassword");
        testUser.setSalt("testSalt");
        testUser.setRole("ADMIN");
        testUser.setName("Test User");
    }

    @Test
    void testLogin_WithValidCredentials_ShouldReturnUser() {
        String username = "testuser";
        String hashedPassword = "inputHashedPassword";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        try (MockedStatic<PasswordUtil> passwordUtilMock = mockStatic(PasswordUtil.class)) {
            passwordUtilMock.when(() -> PasswordUtil.verifyPassword(hashedPassword, testUser.getSalt(), testUser.getPassword()))
                    .thenReturn(true);

            User result = userService.login(username, hashedPassword);

            assertNotNull(result);
            assertEquals(testUser.getId(), result.getId());
            assertEquals(testUser.getUsername(), result.getUsername());
        }
    }

    @Test
    void testLogin_WithInvalidPassword_ShouldReturnNull() {
        String username = "testuser";
        String hashedPassword = "wrongPassword";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        try (MockedStatic<PasswordUtil> passwordUtilMock = mockStatic(PasswordUtil.class)) {
            passwordUtilMock.when(() -> PasswordUtil.verifyPassword(hashedPassword, testUser.getSalt(), testUser.getPassword()))
                    .thenReturn(false);

            User result = userService.login(username, hashedPassword);

            assertNull(result);
        }
    }

    @Test
    void testLogin_WithNonExistentUser_ShouldReturnNull() {
        String username = "nonexistent";
        String hashedPassword = "password";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        User result = userService.login(username, hashedPassword);

        assertNull(result);
    }

    @Test
    void testSave_WithNewUserWithoutSalt_ShouldGenerateSaltAndHash() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("plainPassword");
        newUser.setRole("TEACHER");

        String generatedSalt = "generatedSalt";
        String generatedHash = "generatedHash";

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setUsername("newuser");
        savedUser.setPassword(generatedHash);
        savedUser.setSalt(generatedSalt);
        savedUser.setRole("TEACHER");

        try (MockedStatic<PasswordUtil> passwordUtilMock = mockStatic(PasswordUtil.class)) {
            passwordUtilMock.when(PasswordUtil::generateSalt).thenReturn(generatedSalt);
            passwordUtilMock.when(() -> PasswordUtil.hashPassword("plainPassword", generatedSalt)).thenReturn(generatedHash);

            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            User result = userService.save(newUser);

            assertNotNull(result);
            assertEquals(2L, result.getId());
            assertEquals(generatedHash, result.getPassword());
            assertEquals(generatedSalt, result.getSalt());

            passwordUtilMock.verify(PasswordUtil::generateSalt);
            passwordUtilMock.verify(() -> PasswordUtil.hashPassword("plainPassword", generatedSalt));
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void testSave_WithUserWithSalt_ShouldNotRegenerateSalt() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setPassword("existingHash");
        existingUser.setSalt("existingSalt");
        existingUser.setRole("ADMIN");

        when(userRepository.save(existingUser)).thenReturn(existingUser);

        User result = userService.save(existingUser);

        assertNotNull(result);
        assertEquals(existingUser.getPassword(), result.getPassword());
        assertEquals(existingUser.getSalt(), result.getSalt());
        verify(userRepository).save(existingUser);
    }

    @Test
    void testUpdatePassword_ShouldGenerateNewSaltAndHash() {
        String newPassword = "newPassword123";
        String newSalt = "newSalt";
        String newHash = "newHash";

        try (MockedStatic<PasswordUtil> passwordUtilMock = mockStatic(PasswordUtil.class)) {
            passwordUtilMock.when(PasswordUtil::generateSalt).thenReturn(newSalt);
            passwordUtilMock.when(() -> PasswordUtil.hashPassword(newPassword, newSalt)).thenReturn(newHash);

            User updatedUser = new User();
            updatedUser.setId(testUser.getId());
            updatedUser.setUsername(testUser.getUsername());
            updatedUser.setPassword(newHash);
            updatedUser.setSalt(newSalt);
            updatedUser.setRole(testUser.getRole());

            when(userRepository.save(any(User.class))).thenReturn(updatedUser);

            User result = userService.updatePassword(testUser, newPassword);

            assertNotNull(result);
            assertEquals(newHash, result.getPassword());
            assertEquals(newSalt, result.getSalt());

            passwordUtilMock.verify(PasswordUtil::generateSalt);
            passwordUtilMock.verify(() -> PasswordUtil.hashPassword(newPassword, newSalt));
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void testDelete_ShouldCallRepositoryDelete() {
        Long userId = 1L;

        doNothing().when(userRepository).deleteById(userId);

        userService.delete(userId);

        verify(userRepository).deleteById(userId);
    }

    @Test
    void testFindAll_ShouldReturnAllUsers() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");

        List<User> users = Arrays.asList(user1, user2);

        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals("user2", result.get(1).getUsername());
        verify(userRepository).findAll();
    }

    @Test
    void testFindById_WithExistingUser_ShouldReturnUser() {
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById(userId);

        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
        assertEquals(testUser.getUsername(), result.get().getUsername());
        verify(userRepository).findById(userId);
    }

    @Test
    void testFindById_WithNonExistentUser_ShouldReturnEmptyOptional() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(userId);

        assertFalse(result.isPresent());
        verify(userRepository).findById(userId);
    }

    @Test
    void testLogin_WithNullUsername_ShouldReturnNull() {
        User result = userService.login(null, "password");

        assertNull(result);
        verify(userRepository).findByUsername(null);
    }

    @Test
    void testLogin_WithEmptyUsername_ShouldReturnNull() {
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        User result = userService.login("", "password");

        assertNull(result);
        verify(userRepository).findByUsername("");
    }
}
