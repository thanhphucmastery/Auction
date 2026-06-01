package AUCTIONCODE.Manager;

import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.User;
import AUCTIONCODE.Model.User.UserInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {

    private UserManager userManager;
    private UserInformation sampleInfo;

    @BeforeEach
    void setUp() {
        userManager = UserManager.getInstance();
        userManager.clear(); // Xóa dữ liệu cũ trước mỗi test case để cô lập môi trường
        sampleInfo = new UserInformation("Hanoi", "0912345678", "test@test.com", "Nguyen Van A");
    }

    @Test
    void testSingletonInstance_ShouldReturnSameInstance() {
        UserManager instance1 = UserManager.getInstance();
        UserManager instance2 = UserManager.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

    @Test
    void testAddAndGetUser_SuccessfulScenario() {
        String userId = "USER_001";
        User player = new Player("player1", sampleInfo, "hashed_pass", userId, 500.0, "PLAYER");

        userManager.addUser(player, userId);

        User retrievedUser = userManager.getUser(userId);
        assertNotNull(retrievedUser);
        assertSame(player, retrievedUser);
        assertEquals("player1", retrievedUser.getUserName());
    }

    @Test
    void testGetUser_WithNonExistentId_ShouldReturnNull() {
        User retrievedUser = userManager.getUser("NON_EXISTENT_ID");
        assertNull(retrievedUser);
    }

    @Test
    void testGetAllUsers_ShouldReturnCorrectList() {
        // Kiểm tra danh sách trống ban đầu
        assertTrue(userManager.getAllUsers().isEmpty());

        User user1 = new Player("user1", sampleInfo, "pass", "ID_01", 100.0, "PLAYER");
        User user2 = new Player("user2", sampleInfo, "pass", "ID_02", 200.0, "PLAYER");

        userManager.addUser(user1, "ID_01");
        userManager.addUser(user2, "ID_02");

        List<User> allUsers = userManager.getAllUsers();
        assertEquals(2, allUsers.size());
        assertTrue(allUsers.contains(user1));
        assertTrue(allUsers.contains(user2));
    }

    @Test
    void testRemoveUser_ShouldDeleteUserSuccessfully() {
        String userId = "ID_TO_REMOVE";
        User user = new Player("remove_me", sampleInfo, "pass", userId, 0.0, "PLAYER");

        userManager.addUser(user, userId);
        assertNotNull(userManager.getUser(userId));

        userManager.removeUser(userId);

        assertNull(userManager.getUser(userId));
        assertTrue(userManager.getAllUsers().isEmpty());
    }

    @Test
    void testClear_ShouldEmptyTheManager() {
        userManager.addUser(new Player("u1", sampleInfo, "p", "ID1", 0.0, "P"), "ID1");
        userManager.addUser(new Player("u2", sampleInfo, "p", "ID2", 0.0, "P"), "ID2");
        assertEquals(2, userManager.getAllUsers().size());

        userManager.clear();

        assertTrue(userManager.getAllUsers().isEmpty());
    }
}