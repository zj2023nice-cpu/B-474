package com.example.lab.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void testGenerateSalt_ShouldGenerateNonEmptyString() {
        String salt1 = PasswordUtil.generateSalt();
        String salt2 = PasswordUtil.generateSalt();
        
        assertNotNull(salt1);
        assertFalse(salt1.isEmpty());
        assertNotNull(salt2);
        assertFalse(salt2.isEmpty());
        assertNotEquals(salt1, salt2);
    }

    @Test
    void testHashPassword_ShouldGenerateConsistentHash() {
        String password = "testPassword123";
        String salt = "testSalt";
        
        String hash1 = PasswordUtil.hashPassword(password, salt);
        String hash2 = PasswordUtil.hashPassword(password, salt);
        
        assertNotNull(hash1);
        assertFalse(hash1.isEmpty());
        assertEquals(hash1, hash2);
    }

    @Test
    void testHashPassword_ShouldGenerateDifferentHashesWithDifferentSalts() {
        String password = "testPassword123";
        String salt1 = "salt1";
        String salt2 = "salt2";
        
        String hash1 = PasswordUtil.hashPassword(password, salt1);
        String hash2 = PasswordUtil.hashPassword(password, salt2);
        
        assertNotEquals(hash1, hash2);
    }

    @Test
    void testHashPassword_ShouldGenerateDifferentHashesWithDifferentPasswords() {
        String salt = "testSalt";
        String password1 = "password1";
        String password2 = "password2";
        
        String hash1 = PasswordUtil.hashPassword(password1, salt);
        String hash2 = PasswordUtil.hashPassword(password2, salt);
        
        assertNotEquals(hash1, hash2);
    }

    @Test
    void testVerifyPassword_ShouldReturnTrueForCorrectPassword() {
        String password = "securePassword";
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);
        
        assertTrue(PasswordUtil.verifyPassword(password, salt, hash));
    }

    @Test
    void testVerifyPassword_ShouldReturnFalseForIncorrectPassword() {
        String correctPassword = "securePassword";
        String wrongPassword = "wrongPassword";
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(correctPassword, salt);
        
        assertFalse(PasswordUtil.verifyPassword(wrongPassword, salt, hash));
    }

    @Test
    void testVerifyPassword_ShouldReturnFalseForWrongSalt() {
        String password = "securePassword";
        String correctSalt = PasswordUtil.generateSalt();
        String wrongSalt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, correctSalt);
        
        assertFalse(PasswordUtil.verifyPassword(password, wrongSalt, hash));
    }

    @Test
    void testHashPasswordWithoutSalt_ShouldGenerateConsistentHash() {
        String password = "testPassword";
        
        String hash1 = PasswordUtil.hashPasswordWithoutSalt(password);
        String hash2 = PasswordUtil.hashPasswordWithoutSalt(password);
        
        assertNotNull(hash1);
        assertFalse(hash1.isEmpty());
        assertEquals(hash1, hash2);
    }

    @Test
    void testHashPasswordWithoutSalt_ShouldGenerateDifferentHashesForDifferentPasswords() {
        String password1 = "password1";
        String password2 = "password2";
        
        String hash1 = PasswordUtil.hashPasswordWithoutSalt(password1);
        String hash2 = PasswordUtil.hashPasswordWithoutSalt(password2);
        
        assertNotEquals(hash1, hash2);
    }

    @Test
    void testFullPasswordFlow_ShouldWorkCorrectly() {
        String originalPassword = "myStrongPassword123";
        String salt = PasswordUtil.generateSalt();
        String storedHash = PasswordUtil.hashPassword(originalPassword, salt);
        
        assertTrue(PasswordUtil.verifyPassword(originalPassword, salt, storedHash));
        assertFalse(PasswordUtil.verifyPassword("wrongPassword", salt, storedHash));
    }

    @Test
    void testEmptyPassword_ShouldNotThrowException() {
        String emptyPassword = "";
        String salt = PasswordUtil.generateSalt();
        
        assertDoesNotThrow(() -> {
            String hash = PasswordUtil.hashPassword(emptyPassword, salt);
            PasswordUtil.verifyPassword(emptyPassword, salt, hash);
        });
    }

    @Test
    void testNullPassword_ShouldThrowException() {
        String salt = PasswordUtil.generateSalt();
        
        assertThrows(NullPointerException.class, () -> {
            PasswordUtil.hashPassword(null, salt);
        });
    }
}
