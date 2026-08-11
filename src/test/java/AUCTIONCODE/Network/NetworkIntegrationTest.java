package AUCTIONCODE.Network;

import AUCTIONCODE.AuthModule.InMemorySessionManager;
import AUCTIONCODE.Database.DatabaseBootstrap;
import AUCTIONCODE.Manager.AuctionManager;
import AUCTIONCODE.Manager.UserManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class NetworkIntegrationTest {

    private ServerSocket serverSocket;
    private AuctionClient auctionClient;
    private ExecutorService serverExecutor;
    private int assignedPort;

    @BeforeAll
    static void initGlobalDatabase() {
        DatabaseBootstrap.initializeAndLoad();
    }

    @BeforeEach
    void startServerAndConnectClient() throws IOException {
        // Reset trạng thái các dịch vụ tĩnh trước mỗi ca test
        InMemorySessionManager.getInstance().removeExpiredSessions();
        UserManager.getInstance().clear();
        AuctionManager.getInstance().clear();
        DatabaseBootstrap.reloadFromDatabase();

        // Khởi tạo ServerSocket trên PORT 0 (Hệ điều hành tự chọn cổng trống bất kỳ để tránh xung đột)
        serverSocket = new ServerSocket(0);
        assignedPort = serverSocket.getLocalPort();

        // Tạo luồng chạy Server độc lập tương tự AuctionServer.main
        serverExecutor = Executors.newSingleThreadExecutor();
        serverExecutor.submit(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    new Thread(handler).start(); // Kích hoạt Worker giải quyết các luồng vào/ra dữ liệu
                }
            } catch (IOException ignored) {
                // Sẽ ném ra khi ServerSocket bị đóng chủ động tại tearDown(), an toàn để bỏ qua
            }
        });

        // Khởi tạo và thiết lập kết nối mạng thực tế từ phía Client tới Server vừa dựng
        auctionClient = new AuctionClient("127.0.0.1", assignedPort);
        auctionClient.connect();
    }

    @AfterEach
    void tearDownNetwork() throws IOException {
        // Đóng dọn dẹp các tài nguyên kết nối để giải phóng tài nguyên hệ thống
        if (auctionClient != null) {
            auctionClient.disconnect();
        }
        if (serverSocket != null) {
            serverSocket.close();
        }
        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Kiểm tra kết nối TCP thành công giữa Client và Server")
    void testPhysicalConnectionSuccess() {
        assertTrue(auctionClient.isConnected(), "Client phải duy trì trạng thái kết nối socket hợp lệ");
    }

    @Test
    @DisplayName("Gửi gói tin thô (sendRaw) qua Socket nhận phản hồi chính xác từ Server")
    void testSendRawMessageOverSocket() {
        // Gửi một chuỗi hành động không tồn tại thông qua luồng I/O mạng thực tế
        String response = auctionClient.sendRaw("ECHO_ACTION:HelloServer");
        assertEquals("ERROR:Unknown action", response, "Hạ tầng mạng phải truyền tải dữ liệu toàn vẹn và trả về đúng mã lỗi phân tích");
    }

    @Test
    @DisplayName("Luồng tích hợp mạng đầy đủ: Đăng ký -> Đăng nhập -> Xác thực Trạng thái Client")
    void testEndToEndNetworkAuthFlow() {
        String username = "net_user_" + System.currentTimeMillis();

        // 1. Thực hiện gọi hàm Đăng ký (Register) thông qua API của AuctionClient công khai
        boolean registerSuccess = auctionClient.register(
                username, "securePass123", "Net Tester", "0912345678", "net@test.com", "Da Nang"
        );
        assertTrue(registerSuccess, "Yêu cầu đăng ký tài khoản qua Socket phải thành công");

        // 2. Thực hiện gọi hàm Đăng nhập (Login) qua Socket mạng
        boolean loginSuccess = auctionClient.login(username, "securePass123");
        assertTrue(loginSuccess, "Yêu cầu đăng nhập qua Socket phải thành công");

        // 3. Kiểm tra xem Client đã lưu trữ chính xác thông tin Session cục bộ chưa
        assertTrue(auctionClient.isLoggedIn());
        assertNotNull(auctionClient.getSessionId());

        // 4. Thực hiện Đăng xuất (Logout) giải phóng Session
        boolean logoutSuccess = auctionClient.logout();
        assertTrue(logoutSuccess, "Yêu cầu đăng xuất qua mạng phải thành công");
        assertFalse(auctionClient.isLoggedIn());
        assertNull(auctionClient.getSessionId());
    }

    @Test
    @DisplayName("Luồng kho hàng: thêm vật phẩm -> xem kho -> tạo phiên từ vật phẩm")
    void testInventoryItemToAuctionFlow() {
        String suffix = String.valueOf(System.currentTimeMillis());
        String username = "seller_" + suffix;
        assertTrue(auctionClient.register(
                username, "securePass123", "Seller Tester", "0912345678", "seller" + suffix + "@test.com", "Ha Noi"
        ));
        assertTrue(auctionClient.login(username, "securePass123"));

        String itemId = auctionClient.addItem(
                "Art", "Tranh test " + suffix, "Mo ta vat pham", "Tester", "2024", "", "data/images/test.png"
        );
        assertNotNull(itemId, "ADD_ITEM phải trả về itemId khi dữ liệu hợp lệ. Lỗi cuối: " + auctionClient.getLastError());

        String itemsBeforeAuction = auctionClient.getMyItems();
        assertNotNull(itemsBeforeAuction);
        assertTrue(itemsBeforeAuction.contains(itemId), "Vật phẩm mới thêm phải xuất hiện trong kho");

        String auctionId = auctionClient.createAuctionFromItemAndReturnId(itemId, 1000.0, 100.0, 0, 30);
        assertNotNull(auctionId, "CREATE_AUCTION_FROM_ITEM phải trả về auctionId. Lỗi cuối: " + auctionClient.getLastError());

        String itemsAfterAuction = auctionClient.getMyItems();
        assertNotNull(itemsAfterAuction);
        assertFalse(itemsAfterAuction.contains(itemId), "Vật phẩm đã mở phiên không còn ở trạng thái sẵn sàng trong kho");

        String auctions = auctionClient.getAuctions();
        assertNotNull(auctions);
        assertTrue(auctions.contains(auctionId), "Phiên mới tạo phải xuất hiện trong danh sách phiên đấu giá");
    }

    @Test
    @DisplayName("Client lưu lại lỗi server cuối cùng để UI hiển thị đúng nguyên nhân")
    void testClientKeepsLastServerError() {
        String response = auctionClient.sendRaw("ECHO_ACTION:HelloServer");
        assertEquals("ERROR:Unknown action", response);
        assertEquals("Unknown action", auctionClient.getLastError());
        assertEquals(response, auctionClient.getLastResponse());
    }

    @Test
    @DisplayName("Xử lý ngoại lệ an toàn khi cố tình gửi tin sau khi Client đã ngắt kết nối mạng")
    void testSendRequestAfterClientDisconnect() {
        assertTrue(auctionClient.isConnected());

        // Chủ động ngắt kết nối Socket phía Client
        auctionClient.disconnect();
        assertFalse(auctionClient.isConnected());

        // Thực hiện gửi yêu cầu thô khi Socket đã đóng
        String response = auctionClient.sendRaw("LOGIN:test:1234");
        assertEquals("ERROR:Client is not connected", response,
                "Hệ thống phải tự bắt lỗi kết nối thay vì ném ra NullPointerException hoặc IOException làm sập ứng dụng");
    }
}
