package AUCTIONCODE.Database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseConnection Tests")
class DatabaseConnectionTest {

    @Test
    @DisplayName("getInstance() trả về cùng một instance (Singleton)")
    void testSingleton() {
        DatabaseConnection instance1 = DatabaseConnection.getInstance();
        DatabaseConnection instance2 = DatabaseConnection.getInstance();
        assertSame(instance1, instance2, "getInstance() phải trả về cùng một object");
    }

    @Test
    @DisplayName("newConnection() tạo kết nối hợp lệ")
    void testNewConnection() throws SQLException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        try (Connection conn = db.newConnection()) {
            assertNotNull(conn, "Connection không được null");
            assertFalse(conn.isClosed(), "Connection phải đang mở");
        }
    }

    @Test
    @DisplayName("getDatabasePath() không trả về null hoặc rỗng")
    void testGetDatabasePath() {
        String path = DatabaseConnection.getDatabasePath();
        assertNotNull(path);
        assertFalse(path.isBlank(), "Database path không được rỗng");
    }

    @Test
    @DisplayName("Mỗi lần gọi newConnection() tạo connection độc lập")
    void testNewConnectionIndependent() throws SQLException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        try (Connection c1 = db.newConnection();
             Connection c2 = db.newConnection()) {
            assertNotSame(c1, c2, "Mỗi lần gọi phải tạo connection mới");
        }
    }
}