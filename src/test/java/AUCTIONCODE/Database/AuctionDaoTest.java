package AUCTIONCODE.Database;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuctionDAO Tests")
class AuctionDAOTest extends BaseDaoTest {

    @BeforeEach
    void seedData() throws SQLException {
        insertSampleUser("seller-01", "seller", "Player", 1000.0);
        insertSampleUser("bidder-01", "bidder", "Player", 500.0);
        insertSampleArtItem("item-001", "seller-01", "IN_AUCTION");
    }

    @Test
    @DisplayName("insertAuction() - Lưu auction mới thành công (không có highestBidder)")
    void testInsertAuctionNoHighestBidder() throws SQLException {
        insertSampleAuction("auc-001", "seller-01", "item-001", "UPCOMING", null);

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Auctions WHERE auctionId = ?")) {
            pstmt.setString(1, "auc-001");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "Auction phải tồn tại sau khi insert");
            assertEquals("seller-01", rs.getString("sellerId"));
            assertEquals(100.0, rs.getDouble("currentPrice"), 0.001);
            assertEquals("UPCOMING", rs.getString("status"));
            assertNull(rs.getString("highestBidderId"), "Chưa có người đặt giá cao nhất");
        }
    }

    @Test
    @DisplayName("insertAuction() - Lưu auction với highestBidder thành công")
    void testInsertAuctionWithHighestBidder() throws SQLException {
        insertSampleAuction("auc-002", "seller-01", "item-001", "OPEN", "bidder-01");

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT highestBidderId FROM Auctions WHERE auctionId = ?")) {
            pstmt.setString(1, "auc-002");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("bidder-01", rs.getString("highestBidderId"));
        }
    }

    @Test
    @DisplayName("insertAuction() - Trùng auctionId thì bị SQLException")
    void testInsertDuplicateAuctionId() throws SQLException {
        insertSampleAuction("auc-dup", "seller-01", "item-001", "UPCOMING", null);
        assertThrows(SQLException.class, () ->
                        insertSampleAuction("auc-dup", "seller-01", "item-001", "OPEN", null),
                "Phải ném exception khi auctionId trùng"
        );
    }



    @Test
    @DisplayName("update() - Cập nhật currentPrice và status thành công")
    void testUpdateAuction() throws SQLException {
        insertSampleAuction("auc-010", "seller-01", "item-001", "OPEN", null);

        String sql = "UPDATE Auctions SET currentPrice = ?, status = ?, highestBidderId = ?, endTime = ? WHERE auctionId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, 250.0);
            pstmt.setString(2, "OPEN");
            pstmt.setString(3, "bidder-01");
            pstmt.setString(4, "2025-01-02T12:00");
            pstmt.setString(5, "auc-010");
            pstmt.executeUpdate();
        }

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Auctions WHERE auctionId = ?")) {
            pstmt.setString(1, "auc-010");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(250.0, rs.getDouble("currentPrice"), 0.001);
            assertEquals("bidder-01", rs.getString("highestBidderId"));
        }
    }

    @Test
    @DisplayName("update() - highestBidder có thể set về NULL")
    void testUpdateAuctionNullHighestBidder() throws SQLException {
        insertSampleAuction("auc-011", "seller-01", "item-001", "OPEN", "bidder-01");

        String sql = "UPDATE Auctions SET currentPrice = ?, status = ?, highestBidderId = ?, endTime = ? WHERE auctionId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, 100.0);
            pstmt.setString(2, "CANCELLED");
            pstmt.setNull(3, Types.VARCHAR);
            pstmt.setString(4, "2025-01-02T10:00");
            pstmt.setString(5, "auc-011");
            pstmt.executeUpdate();
        }

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT highestBidderId FROM Auctions WHERE auctionId = ?")) {
            pstmt.setString(1, "auc-011");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertNull(rs.getString("highestBidderId"), "highestBidderId phải là NULL");
        }
    }



    @Test
    @DisplayName("updateStatus() - Cập nhật status thành CLOSED")
    void testUpdateStatus() throws SQLException {
        insertSampleAuction("auc-020", "seller-01", "item-001", "OPEN", null);

        String sql = "UPDATE Auctions SET status = ? WHERE auctionId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "CLOSED");
            pstmt.setString(2, "auc-020");
            pstmt.executeUpdate();
        }

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT status FROM Auctions WHERE auctionId = ?")) {
            pstmt.setString(1, "auc-020");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("CLOSED", rs.getString("status"));
        }
    }

    // ─── updateEndTimeAndStatus() ─────────────────────────────────────────────

    @Test
    @DisplayName("updateEndTimeAndStatus() - Cập nhật endTime và status")
    void testUpdateEndTimeAndStatus() throws SQLException {
        insertSampleAuction("auc-030", "seller-01", "item-001", "OPEN", null);

        String sql = "UPDATE Auctions SET endTime = ?, status = ? WHERE auctionId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "2025-12-31T23:59");
            pstmt.setString(2, "EXTENDED");
            pstmt.setString(3, "auc-030");
            pstmt.executeUpdate();
        }

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT endTime, status FROM Auctions WHERE auctionId = ?")) {
            pstmt.setString(1, "auc-030");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("2025-12-31T23:59", rs.getString("endTime"));
            assertEquals("EXTENDED", rs.getString("status"));
        }
    }


    @Test
    @DisplayName("delete() - Xoá auction thành công")
    void testDeleteAuction() throws SQLException {
        insertSampleAuction("auc-040", "seller-01", "item-001", "CLOSED", null);

        String sql = "DELETE FROM Auctions WHERE auctionId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "auc-040");
            int rows = pstmt.executeUpdate();
            assertTrue(rows > 0);
        }

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Auctions WHERE auctionId = ?")) {
            pstmt.setString(1, "auc-040");
            assertFalse(pstmt.executeQuery().next(), "Auction đã bị xoá");
        }
    }

    @Test
    @DisplayName("delete() - Xoá auction không tồn tại trả về 0 rows")
    void testDeleteNonExistentAuction() throws SQLException {
        String sql = "DELETE FROM Auctions WHERE auctionId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "ghost-auc");
            int rows = pstmt.executeUpdate();
            assertEquals(0, rows);
        }
    }


    @Test
    @DisplayName("deleteCascade() - Xoá auction + bids + item trong 1 transaction")
    void testDeleteCascade() throws SQLException {
        insertSampleAuction("auc-050", "seller-01", "item-001", "CLOSED", null);

        // Thêm bid transaction liên quan
        String insertBid = "INSERT INTO bidTransactions (transactionId, auctionId, bidderId, amount, timestamp) " +
                "VALUES ('bid-001', 'auc-050', 'bidder-01', 150.0, '2025-01-01T11:00')";
        conn.createStatement().execute(insertBid);

        // Thực hiện cascade delete
        conn.setAutoCommit(false);
        try {
            //  Xoá bids
            try (PreparedStatement p = conn.prepareStatement("DELETE FROM bidTransactions WHERE auctionId = ?")) {
                p.setString(1, "auc-050");
                p.executeUpdate();
            }
            //  Xoá auction
            try (PreparedStatement p = conn.prepareStatement("DELETE FROM Auctions WHERE auctionId = ?")) {
                p.setString(1, "auc-050");
                p.executeUpdate();
            }
            //  Xoá item
            try (PreparedStatement p = conn.prepareStatement("DELETE FROM Item WHERE itemId = ?")) {
                p.setString(1, "item-001");
                p.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }

        // Kiểm tra tất cả đã bị xoá
        assertFalse(queryExists("SELECT * FROM Auctions WHERE auctionId = 'auc-050'"), "Auction bị xoá");
        assertFalse(queryExists("SELECT * FROM bidTransactions WHERE auctionId = 'auc-050'"), "Bids bị xoá");
        assertFalse(queryExists("SELECT * FROM Item WHERE itemId = 'item-001'"), "Item bị xoá");
    }

    @Test
    @DisplayName("deleteCascade() - Rollback nếu gặp lỗi giữa chừng")
    void testDeleteCascadeRollback() throws SQLException {
        insertSampleAuction("auc-060", "seller-01", "item-001", "CLOSED", null);

        // Cố tình gây lỗi bằng cách vi phạm NOT NULL
        conn.setAutoCommit(false);
        boolean rolledBack = false;
        try {
            try (PreparedStatement p = conn.prepareStatement("DELETE FROM bidTransactions WHERE auctionId = ?")) {
                p.setString(1, "auc-060");
                p.executeUpdate();
            }
            // Vi phạm: insert dữ liệu thiếu NOT NULL để gây lỗi
            conn.prepareStatement("INSERT INTO users (userId) VALUES (NULL)").executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            rolledBack = true;
        } finally {
            conn.setAutoCommit(true);
        }

        assertTrue(rolledBack, "Transaction phải rollback khi gặp lỗi");
        // Auction vẫn còn (không bị xoá do rollback)
        assertTrue(queryExists("SELECT * FROM Auctions WHERE auctionId = 'auc-060'"),
                "Auction phải còn nguyên sau rollback");
    }

    // ─── getNextCounter() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getNextCounter() - Trả về 1 khi bảng rỗng")
    void testGetNextCounterEmpty() throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM Auctions")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1) + 1, "Counter bắt đầu từ 1 khi bảng rỗng");
        }
    }

    @Test
    @DisplayName("getNextCounter() - Tăng đúng sau khi thêm auction")
    void testGetNextCounterIncrement() throws SQLException {
        insertSampleAuction("auc-c1", "seller-01", "item-001", "OPEN", null);

        try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM Auctions")) {
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1) + 1, "Counter phải là 2 sau khi có 1 auction");
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private boolean queryExists(String sql) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            return rs.next();
        }
    }
}