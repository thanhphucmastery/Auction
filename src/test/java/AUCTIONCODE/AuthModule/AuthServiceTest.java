package AUCTIONCODE.AuthModule;

import AUCTIONCODE.Model.User.User;
import AUCTIONCODE.Model.User.UserInformation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private UserInformation userInfor;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = AuthService.getInstance();
        // Xóa sạch dữ liệu người dùng cũ trong db tĩnh trước mỗi bài test để cô lập môi trường
        AuthService.clearLoadedUsers();

        // Chuẩn bị thông tin cá nhân (SĐT có đúng 10 chữ số)
        userInfor = new UserInformation("Hanoi", "0912345678", "nguyen@test.com", "Nguyen Van");
    }

    @AfterEach
    void tearDown() {
        AuthService.clearLoadedUsers();
    }

    @Test
    void testRegisterPlayer_SuccessAndDuplicateError() {
        // Đăng ký lần đầu -> Thành công
        boolean firstRegister = AuthService.registerPlayer("nguyen01", userInfor, "plain_password");
        assertTrue(firstRegister);

        // Đăng ký trùng tên trùng username -> Phải từ chối (trả về false)
        boolean secondRegister = AuthService.registerPlayer("nguyen01", userInfor, "another_password");
        assertFalse(secondRegister);
    }

    @Test
    void testRegisterAdmin_WithValidAndInvalidBusinessCode() {
        // Đăng ký Admin với mã doanh nghiệp hợp lệ ("BIZ-001" hoặc "BIZ-002")
        boolean validAdmin = AuthService.registerAdmin("admin01", userInfor, "admin_pass", "BIZ-001");
        assertTrue(validAdmin);

        // Đăng ký Admin với mã doanh nghiệp sai trái pháp luật hệ thống -> Phải từ chối
        boolean invalidAdmin = AuthService.registerAdmin("admin02", userInfor, "admin_pass", "BIZ-SHARK");
        assertFalse(invalidAdmin);
    }

    @Test
    void testLogin_SuccessAndFailure() {
        // 1. Tạo tài khoản mẫu
        AuthService.registerPlayer("player_test", userInfor, "my_secret_password");

        // 2. Đăng nhập đúng mật khẩu -> Thành công trả về Session ID hợp lệ
        String sessionId = AuthService.login("player_test", "my_secret_password");
        assertNotNull(sessionId);
        assertNotEquals("", sessionId);
        assertTrue(InMemorySessionManager.getInstance().isValidSession(sessionId));

        // 3. Đăng nhập sai mật khẩu -> Phải trả về null
        String wrongPassSession = AuthService.login("player_test", "wrong_pass");
        assertNull(wrongPassSession);

        // 4. Đăng nhập tài khoản không tồn tại -> Phải trả về null
        String nonExistentSession = AuthService.login("ghost_user", "any_pass");
        assertNull(nonExistentSession);
    }

    @Test
    void testResetPassword_SuccessAndFailure() {
        AuthService.registerPlayer("user_reset", userInfor, "old_pass");

        // Đổi mật khẩu đúng Username và Email khớp -> Thành công
        boolean resetSuccess = AuthService.resetPassword("user_reset", "nguyen@test.com", "new_pass_123");
        assertTrue(resetSuccess);

        // Đăng nhập thử bằng mật khẩu mới kiểm tra xem đã cập nhật chưa
        assertNotNull(AuthService.login("user_reset", "new_pass_123"));

        // Điền sai email -> Không cho phép đổi mật khẩu (trả về false)
        boolean resetWrongEmail = AuthService.resetPassword("user_reset", "wrong_email@test.com", "newer_pass");
        assertFalse(resetWrongEmail);

        // Điền sai username không tồn tại -> Trả về false
        boolean resetWrongUser = AuthService.resetPassword("ghost_user", "nguyen@test.com", "newer_pass");
        assertFalse(resetWrongUser);
    }

    @Test
    void testLogout_SuccessfulScenario() {
        AuthService.registerPlayer("logout_user", userInfor, "pass123");
        String sessionId = AuthService.login("logout_user", "pass123");

        // Thực hiện đăng xuất với Session hiện hành
        boolean logoutResult = AuthService.logout(sessionId);
        assertTrue(logoutResult);

        // Sau khi logout, session ID không còn hợp lệ trên hệ thống nữa
        assertFalse(InMemorySessionManager.getInstance().isValidSession(sessionId));

        // Thử logout lại một lần nữa bằng session cũ -> Phải trả về false do session không tồn tại nữa
        assertFalse(AuthService.logout(sessionId));
    }
}