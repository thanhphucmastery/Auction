package AUCTIONCODE.Network;

import AUCTIONCODE.AuthModule.AuthService;
import AUCTIONCODE.AuthModule.InMemorySessionManager;
import AUCTIONCODE.Database.DatabaseBootstrap;
import AUCTIONCODE.Manager.AuctionManager;
import AUCTIONCODE.Manager.UserManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestParserTest {

    @BeforeAll
    static void initDatabase() {
        // Khởi tạo Database và bảng cấu trúc một lần duy nhất trước khi chạy test suite
        DatabaseBootstrap.initializeAndLoad();
    }

    @BeforeEach
    void clearAndResetState() {
        // Dọn sạch session cũ và bộ nhớ đệm để các ca test không ảnh hưởng lẫn nhau
        InMemorySessionManager.getInstance().removeExpiredSessions();
        UserManager.getInstance().clear();
        AuctionManager.getInstance().clear();
        // Nạp lại các thực thể và tài khoản mặc định (test/1234, admin/1234) từ DB vào hệ thống
        DatabaseBootstrap.reloadFromDatabase();
    }

    @Test
    @DisplayName("Kiểm tra request null hoặc rỗng")
    void testNullOrEmptyRequest() {
        assertEquals("ERROR:Empty request", RequestParser.handle(null));
        assertEquals("ERROR:Empty request", RequestParser.handle(""));
        assertEquals("ERROR:Empty request", RequestParser.handle("   "));
    }

    @Test
    @DisplayName("Kiểm tra hành động (Action) không hợp lệ")
    void testUnknownAction() {
        String response = RequestParser.handle("INVALID_ACTION:param1");
        assertEquals("ERROR:Unknown action", response);
    }

    @Test
    @DisplayName("Đăng nhập thất bại - Sai tài khoản hoặc mật khẩu")
    void testLoginFailed() {
        String response = RequestParser.handle("LOGIN:wronguser:wrongpass");
        assertEquals("ERROR:Invalid username or password", response);
    }

    @Test
    @DisplayName("Đăng nhập thành công với tài khoản mặc định")
    void testLoginSuccess() {
        // Tài khoản mặc định 'test' với mật khẩu '1234' được tạo tự động bởi DatabaseInitializer
        String response = RequestParser.handle("LOGIN:test:1234");
        assertTrue(response.startsWith("OK:"), "Phản hồi hợp lệ phải bắt đầu bằng OK:");

        String sessionId = response.split(":")[1];
        assertNotNull(sessionId);
        assertTrue(InMemorySessionManager.getInstance().isValidSession(sessionId));
    }

    @Test
    @DisplayName("Đăng ký tài khoản mới thất bại do thiếu tham số")
    void testRegisterMissingParameters() {
        // REGISTER yêu cầu: username, password, fullName, phone, email, address (đủ 6 tham số)
        String response = RequestParser.handle("REGISTER:newuser:pass123:FullName");
        assertTrue(response.startsWith("ERROR:Invalid request format"), "Phải báo lỗi định dạng khi thiếu tham số");
    }

    @Test
    @DisplayName("Đăng ký tài khoản mới thành công và không cho phép trùng username")
    void testRegisterAndDuplicatePrevention() {
        String uniqueUser = "user_" + System.currentTimeMillis();
        // Truyền đầy đủ tham số mạng và đảm bảo số điện thoại đúng 10 chữ số theo Regex của UserInformation
        String request = String.format("REGISTER:%s:password123:Nguyen Van A:0987654321:test@example.com:Hanoi", uniqueUser);

        String response = RequestParser.handle(request);
        assertEquals("OK:Registered", response);

        // Cố tình đăng ký lại chính username đó lần nữa
        String duplicateResponse = RequestParser.handle(request);
        assertEquals("ERROR:Username already exists", duplicateResponse);
    }

    @Test
    @DisplayName("Yêu cầu nạp tiền (DEPOSIT) hợp lệ và bắt lỗi số tiền âm")
    void testDepositLogic() {
        // Đăng nhập để lấy SessionID hợp lệ
        String loginRes = RequestParser.handle("LOGIN:test:1234");
        String sessionId = loginRes.split(":")[1];

        // Trường hợp 1: Nạp số tiền hợp lệ
        String successDeposit = RequestParser.handle("DEPOSIT:" + sessionId + ":5000.0");
        assertTrue(successDeposit.startsWith("OK:"));

        // Trường hợp 2: Số tiền nạp không hợp lệ (nhỏ hơn hoặc bằng 0)
        String failedDeposit = RequestParser.handle("DEPOSIT:" + sessionId + ":-100.0");
        assertTrue(failedDeposit.startsWith("ERROR:"), "Phải ném lỗi khi số tiền nạp không dương");
    }

    @Test
    @DisplayName("Đặt giá (BID) vào phòng không tồn tại phải trả về lỗi hệ thống")
    void testBidOnNonExistentRoom() {
        String loginRes = RequestParser.handle("LOGIN:test:1234");
        String sessionId = loginRes.split(":")[1];

        // Gửi lệnh đặt giá lên một mã phòng ma 'fake_room_id'
        String response = RequestParser.handle("BID:" + sessionId + ":fake_room_id:1500.0");
        assertEquals("ERROR:Auction not found", response);
    }
    // =========================================================================
    // 4. KIỂM THỬ CÁC HÀNH ĐỘNG CỦA PLAYER (XEM DANH SÁCH & THÔNG TIN)
    // =========================================================================

    @Test
    @DisplayName("Xem thông tin cá nhân (GET_PROFILE)")
    void testGetProfile() {
        String loginRes = RequestParser.handle("LOGIN:test:1234");
        String sessionId = loginRes.split(":")[1];

        String response = RequestParser.handle("GET_PROFILE:" + sessionId);
        assertTrue(response.startsWith("OK:"), "Phải trả về thông tin dạng OK:username:...");
        assertTrue(response.contains("test"), "Thông tin trả về phải chứa username");
    }

    @Test
    @DisplayName("Xem danh sách vật phẩm (GET_ITEMS) và phòng đấu giá (GET_AUCTIONS)")
    void testGetLists() {
        String loginRes = RequestParser.handle("LOGIN:test:1234");
        String sessionId = loginRes.split(":")[1];

        // Lệnh lấy danh sách vật phẩm
        String itemsRes = RequestParser.handle("GET_MY_ITEMS:" + sessionId);
        assertTrue(itemsRes.startsWith("OK:"), "Phải trả về danh sách vật phẩm hoặc chuỗi rỗng");

        // Lệnh lấy danh sách các phòng đấu giá đang mở
        String auctionsRes = RequestParser.handle("GET_AUCTIONS:" + sessionId);
        assertTrue(auctionsRes.startsWith("OK:"), "Phải trả về danh sách phòng đấu giá");
    }

    @Test
    @DisplayName("Xem lịch sử đấu giá (GET_BID_HISTORY) của một phòng cụ thể")
    void testGetBidHistoryNonExistent() {
        String loginRes = RequestParser.handle("LOGIN:test:1234");
        String sessionId = loginRes.split(":")[1];

        // Xem lịch sử của một phòng không tồn tại
        String response = RequestParser.handle("GET_BID_HISTORY:" + sessionId + ":room_fake_id");
        // Tùy logic code của bạn: trả về lỗi hoặc danh sách trống (ở đây test theo hướng định dạng trả về hợp lệ)
        assertTrue(response.startsWith("OK:") || response.startsWith("ERROR:"));
    }

    // =========================================================================
    // 5. KIỂM THỬ LOGIC TẠO VẬT PHẨM VÀ ĐẤU GIÁ (ADD_ITEM, CREATE_AUCTION_FROM_ITEM)
    // =========================================================================

    @Test
    @DisplayName("Thêm vật phẩm mới thất bại khi sai loại (Item Type)")
    void testAddItemInvalidType() {
        String loginRes = RequestParser.handle("LOGIN:test:1234");
        String sessionId = loginRes.split(":")[1];

        // Giả sử định dạng: ADD_ITEM:sessionId:itemType:itemName:extra1:extra2...
        // Truyền một itemType linh tinh không có trong ItemFactory
        String response = RequestParser.handle("ADD_ITEM:" + sessionId + ":VO_TRI:TenVatPham:MoTa");
        assertTrue(response.startsWith("ERROR:"), "Phải ném lỗi khi loại vật phẩm không tồn tại");
    }

    @Test
    @DisplayName("Tạo phòng đấu giá từ vật phẩm trong kho với các ràng buộc số học")
    void testCreateAuctionFromItemValidation() {
        String loginRes = RequestParser.handle("LOGIN:test:1234");
        String sessionId = loginRes.split(":")[1];

        // Định dạng: CREATE_AUCTION_FROM_ITEM:sessionId:itemId:startPrice:stepPrice:startDelayMinutes:durationMinutes
        // Thử nghiệm truyền giá khởi điểm (startPrice) là số âm
        String response = RequestParser.handle("CREATE_AUCTION_FROM_ITEM:" + sessionId + ":item123:-500.0:1000.0:0:60");
        assertTrue(response.startsWith("ERROR:"), "Phải chặn khi giá khởi điểm không dương");
    }

    @Test
    @DisplayName("Không còn hỗ trợ tạo nhanh phòng đấu giá bằng CREATE_AUCTION")
    void testQuickCreateAuctionRemoved() {
        String loginRes = RequestParser.handle("LOGIN:test:1234");
        String sessionId = loginRes.split(":")[1];

        String response = RequestParser.handle("CREATE_AUCTION:" + sessionId + ":item123:500.0:1000.0:60");
        assertEquals("ERROR:Unknown action", response);
    }

    // =========================================================================
    // 6. KIỂM THỬ PHÂN QUYỀN ADMIN (ADMIN ACTIONS)
    // =========================================================================

    @Test
    @DisplayName("Chặn tài khoản thường (Player) gọi lệnh của Admin")
    void testBlockPlayerFromAdminActions() {
        // Đăng nhập bằng tài khoản người dùng thường 'test'
        String loginRes = RequestParser.handle("LOGIN:test:1234");
        String sessionId = loginRes.split(":")[1];

        // Cố tình gọi một lệnh của Admin (ví dụ: ADMIN_GET_USERS)
        String response = RequestParser.handle("ADMIN_GET_USERS:" + sessionId);
        assertEquals("ERROR:Only admin accounts can do this action", response,
                "Hệ thống bắt buộc phải từ chối khi Player cố truy cập quyền Admin");
    }

    @Test
    @DisplayName("Admin thực hiện lệnh xem danh sách người dùng thành công")
    void testAdminGetUsersSuccess() {
        // Đăng nhập bằng tài khoản 'admin' mặc định
        String loginRes = RequestParser.handle("LOGIN:admin:1234");
        String sessionId = loginRes.split(":")[1];

        // Gọi lệnh Admin hợp lệ
        String response = RequestParser.handle("ADMIN_GET_USERS:" + sessionId);
        assertTrue(response.startsWith("OK:"), "Admin phải lấy được danh sách người dùng");
    }

    // =========================================================================
    // 7. KIỂM THỬ ĐĂNG XUẤT VÀ HUỶ SESSION (LOGOUT)
    // =========================================================================

    @Test
    @DisplayName("Đăng xuất thành công và vô hiệu hóa Session cũ")
    void testLogoutLogic() {
        // 1. Đăng nhập lấy token
        String loginRes = RequestParser.handle("LOGIN:test:1234");
        String sessionId = loginRes.split(":")[1];

        // 2. Thực hiện đăng xuất
        String logoutRes = RequestParser.handle("LOGOUT:" + sessionId);
        assertEquals("OK:Logged out", logoutRes);

        // 3. Dùng lại token cũ để nạp tiền xem có bị chặn không
        String reuseTokenRes = RequestParser.handle("DEPOSIT:" + sessionId + ":100.0");
        assertEquals("ERROR:Invalid session", reuseTokenRes, "Token cũ phải bị vô hiệu hóa sau khi logout");
    }
}
