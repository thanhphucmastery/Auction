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

    @Test
    void testDeepCopy_ShouldReturnNewIndependentAdmin() {
        Admin original = new Admin("admin1", userInfo, "pass123", "AD001", "BIZ123", "ADMIN");
        User copy = original.deepCopy();

        // Kiểm tra xem bản copy có thuộc kiểu Admin không
        assertTrue(copy instanceof Admin);
        Admin clonedAdmin = (Admin) copy;

        assertNotSame(original, clonedAdmin); // Khác thực thể Admin
        assertNotSame(original.getUserInformation(), clonedAdmin.getUserInformation()); // Khác thực thể UserInformation nhờ clone()

        // Kiểm tra dữ liệu trùng khớp
        assertEquals(original.getUserName(), clonedAdmin.getUserName());
        assertEquals(original.getUserPassword(), clonedAdmin.getUserPassword());
        assertEquals(original.getUserId(), clonedAdmin.getUserId());
        assertEquals(original.getBusinessCode(), clonedAdmin.getBusinessCode());
        assertEquals(original.getUserRole(), clonedAdmin.getUserRole());
    }
}