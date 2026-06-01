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