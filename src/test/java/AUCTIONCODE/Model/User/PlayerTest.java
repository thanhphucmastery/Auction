package AUCTIONCODE.Model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    private UserInformation userInfo;

    @BeforeEach
    void setUp() {
        userInfo = new UserInformation("Danang", "0111222333", "player@test.com", "Nguyen Player");
    }

    @Test
    void testConstructorAndBalance() {
        Player player = new Player("player1", userInfo, "pass", "PL001", 500.0, "PLAYER");
        assertEquals(500.0, player.getPlayerBalance());
    }

    @Test
    void testSetPlayerBalance_ValidValue() {
        Player player = new Player("player1", userInfo, "pass", "PL001", 500.0, "PLAYER");
        player.setPlayerBalance(1000.5);
        assertEquals(1000.5, player.getPlayerBalance());

        player.setPlayerBalance(0);
        assertEquals(0, player.getPlayerBalance());
    }

    @Test
    void testSetPlayerBalance_NegativeValue_ShouldThrowException() {
        Player player = new Player("player1", userInfo, "pass", "PL001", 500.0, "PLAYER");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            player.setPlayerBalance(-10.0);
        });
        assertEquals("Số dư không được âm", exception.getMessage());
    }

}