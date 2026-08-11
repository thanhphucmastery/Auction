package AUCTIONCODE.Model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminTest {

    private UserInformation userInfo;

    @BeforeEach
    void setUp() {
        userInfo = new UserInformation("Hanoi", "0987654321", "admin@test.com", "Admin Code");
    }

    @Test
    void testConstructorAndGetters() {
        Admin admin = new Admin("admin1", userInfo, "hashed_pass", "AD001", "BIZ123", "ADMIN_ROLE");

        assertEquals("admin1", admin.getUserName());
        assertEquals("hashed_pass", admin.getUserPassword());
        assertEquals("AD001", admin.getUserId());
        assertEquals("BIZ123", admin.getBusinessCode());
        assertEquals("ADMIN_ROLE", admin.getUserRole());
        assertSame(userInfo, admin.getUserInformation());
    }

    @Test
    void testSetPassword() {
        Admin admin = new Admin("admin1", userInfo, "old_pass", "AD001", "BIZ123", "ADMIN_ROLE");
        admin.setUserPassword("new_pass");
        assertEquals("new_pass", admin.getUserPassword());
    }
}