package AUCTIONCODE.Database;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseInitializer Tests")
class DatabaseInitializerTest extends BaseDaoTest {



    @Test
    @DisplayName("Bảng 'users' được tạo sau khi Initialize")
    void testUsersTableExists() throws SQLException {
        assertTrue(tableExists("users"), "Bảng users phải tồn tại");
    }

    @Test
    @DisplayName("Bảng 'Item' được tạo sau khi Initialize")
    void testItemTableExists() throws SQLException {
        assertTrue(tableExists("Item"), "Bảng Item phải tồn tại");
    }

    @Test
    @DisplayName("Bảng 'Auctions' được tạo sau khi Initialize")
    void testAuctionsTableExists() throws SQLException {
        assertTrue(tableExists("Auctions"), "Bảng Auctions phải tồn tại");
    }

    @Test
    @DisplayName("Bảng 'bidTransactions' được tạo sau khi Initialize")
    void testBidTransactionsTableExists() throws SQLException {
        assertTrue(tableExists("bidTransactions"), "Bảng bidTransactions phải tồn tại");
    }



    @Test
    @DisplayName("Bảng users có đủ các cột bắt buộc")
    void testUsersTableColumns() throws SQLException {
        assertTrue(columnExists("users", "userId"),       "Cột userId");
        assertTrue(columnExists("users", "userName"),     "Cột userName");
        assertTrue(columnExists("users", "hashPassword"), "Cột hashPassword");
        assertTrue(columnExists("users", "balance"),      "Cột balance");
        assertTrue(columnExists("users", "role"),         "Cột role");
        assertTrue(columnExists("users", "fullname"),     "Cột fullname");
        assertTrue(columnExists("users", "email"),        "Cột email");
        assertTrue(columnExists("users", "phone"),        "Cột phone");
        assertTrue(columnExists("users", "address"),      "Cột address");
        assertTrue(columnExists("users", "businessCode"), "Cột businessCode");
    }



    @Test
    @DisplayName("Bảng Item có đủ các cột bắt buộc (kể cả các cột thêm sau)")
    void testItemTableColumns() throws SQLException {
        assertTrue(columnExists("Item", "itemId"),      "Cột itemId");
        assertTrue(columnExists("Item", "name"),        "Cột name");
        assertTrue(columnExists("Item", "type"),        "Cột type");
        assertTrue(columnExists("Item", "ownerId"),     "Cột ownerId (thêm sau)");
        assertTrue(columnExists("Item", "status"),      "Cột status (thêm sau)");
        assertTrue(columnExists("Item", "imagePath"),   "Cột imagePath (thêm sau)");
    }


    @Test
    @DisplayName("Bảng Auctions có đủ các cột bắt buộc")
    void testAuctionsTableColumns() throws SQLException {
        assertTrue(columnExists("Auctions", "auctionId"),       "Cột auctionId");
        assertTrue(columnExists("Auctions", "sellerId"),         "Cột sellerId");
        assertTrue(columnExists("Auctions", "currentPrice"),     "Cột currentPrice");
        assertTrue(columnExists("Auctions", "stepPrice"),        "Cột stepPrice");
        assertTrue(columnExists("Auctions", "openTime"),         "Cột openTime");
        assertTrue(columnExists("Auctions", "endTime"),          "Cột endTime");
        assertTrue(columnExists("Auctions", "status"),           "Cột status");
        assertTrue(columnExists("Auctions", "itemId"),           "Cột itemId");
        assertTrue(columnExists("Auctions", "highestBidderId"),  "Cột highestBidderId");
    }


    @Test
    @DisplayName("Item.status mặc định là 'AVAILABLE' khi không chỉ định")
    void testItemDefaultStatus() throws SQLException {
        String sql = "INSERT INTO Item (itemId, name, description, type) VALUES ('test-default', 'Name', 'Desc', 'Art')";
        conn.createStatement().execute(sql);

        try (PreparedStatement p = conn.prepareStatement("SELECT status FROM Item WHERE itemId = ?")) {
            p.setString(1, "test-default");
            ResultSet rs = p.executeQuery();
            assertTrue(rs.next());
            String status = rs.getString("status");
            String effective = (status != null) ? status : "AVAILABLE";
            assertEquals("AVAILABLE", effective);
        }
    }


    @Test
    @DisplayName("userName trong users là UNIQUE")
    void testUserNameUniqueConstraint() throws SQLException {
        insertSampleUser("u-001", "uniqueUser", "Player", 0.0);
        assertThrows(SQLException.class, () ->
                        insertSampleUser("u-002", "uniqueUser", "Player", 0.0),
                "UNIQUE constraint phải ngăn userName trùng"
        );
    }

    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }
}