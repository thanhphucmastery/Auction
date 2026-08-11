package AUCTIONCODE.AuthModule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hashMatchesOriginalPassword() {
        String hash = PasswordHasher.hash("secret123");

        assertTrue(PasswordHasher.matches("secret123", hash));
    }

    @Test
    void hashRejectsWrongPassword() {
        String hash = PasswordHasher.hash("secret123");

        assertFalse(PasswordHasher.matches("wrong-password", hash));
    }

    @Test
    void hashUsesRandomSalt() {
        String firstHash = PasswordHasher.hash("secret123");
        String secondHash = PasswordHasher.hash("secret123");

        assertNotEquals(firstHash, secondHash);
        assertTrue(PasswordHasher.matches("secret123", firstHash));
        assertTrue(PasswordHasher.matches("secret123", secondHash));
    }

    @Test
    void invalidInputsDoNotMatch() {
        assertFalse(PasswordHasher.matches(null, PasswordHasher.hash("secret123")));
        assertFalse(PasswordHasher.matches("secret123", null));
        assertFalse(PasswordHasher.matches("secret123", "not-a-valid-hash"));
    }

    @Test
    void blankPasswordCannotBeHashed() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(" "));
    }
}
