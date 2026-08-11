package AUCTIONCODE.Model.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserInformationTest {

        @Test
        void testConstructor_ValidData_ShouldCreateObject() {
            UserInformation info = new UserInformation("Hanoi", "0123456789", "test@gmail.com", "Nguyen Van A");

            assertEquals("Hanoi", info.getAddress());
            assertEquals("0123456789", info.getPhoneNumber());
            assertEquals("test@gmail.com", info.getEmail());
            assertEquals("Nguyen Van A", info.getFullName());
        }

        @Test
        void testConstructor_InvalidPhoneNumber_ShouldThrowException() {
            // Số điện thoại ít hơn 10 số
            Exception exception1 = assertThrows(IllegalArgumentException.class, () -> {
                new UserInformation("Hanoi", "12345", "test@gmail.com", "Nguyen Van A");
            });
            assertEquals("Error: A phone number must have 10 digits", exception1.getMessage());

            // Số điện thoại chứa ký tự chữ
            assertThrows(IllegalArgumentException.class, () -> {
                new UserInformation("Hanoi", "012345678a", "test@gmail.com", "Nguyen Van A");
            });

            // Số điện thoại bằng null
            assertThrows(IllegalArgumentException.class, () -> {
                new UserInformation("Hanoi", null, "test@gmail.com", "Nguyen Van A");
            });
        }

        @Test
        void testClone_ShouldCreateDeepCopy() {
            UserInformation original = new UserInformation("Hanoi", "0123456789", "test@gmail.com", "Nguyen Van A");
            UserInformation cloned = original.clone();

            assertNotNull(cloned);
            assertNotSame(original, cloned); // 2 object khác vùng nhớ
            assertEquals(original.getAddress(), cloned.getAddress());
            assertEquals(original.getPhoneNumber(), cloned.getPhoneNumber());
            assertEquals(original.getEmail(), cloned.getEmail());
            assertEquals(original.getFullName(), cloned.getFullName());
        }
    }

