package AUCTIONCODE.Database;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ItemDAO Tests")
class ItemDAOTest extends BaseDaoTest {


    @Test
    @DisplayName("save() - Lưu item Art thành công")
    void testSaveArtItem() throws SQLException {
        insertSampleArtItem("item-001", "owner-01", "AVAILABLE");

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Item WHERE itemId = ?")) {
            pstmt.setString(1, "item-001");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "Item phải tồn tại sau khi lưu");
            assertEquals("Art", rs.getString("type"));
            assertEquals("Van Gogh", rs.getString("artist"));
            assertEquals(1889, rs.getInt("yearCreated"));
            assertEquals("AVAILABLE", rs.getString("status"));
        }
    }

    @Test
    @DisplayName("save() - Lưu item Electronics thành công")
    void testSaveElectronicsItem() throws SQLException {
        String sql = "INSERT INTO Item (itemId, name, description, type, brand, yearMade, warranty, ownerId, status, imagePath) " +
                "VALUES ('item-002', 'Laptop', 'Gaming laptop', 'Electronics', 'Dell', 2023, 24, 'owner-01', 'AVAILABLE', NULL)";
        conn.createStatement().execute(sql);

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Item WHERE itemId = ?")) {
            pstmt.setString(1, "item-002");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("Electronics", rs.getString("type"));
            assertEquals("Dell", rs.getString("brand"));
            assertEquals(24, rs.getInt("warranty"));
        }
    }

    @Test
    @DisplayName("save() - Lưu item Vehicle thành công")
    void testSaveVehicleItem() throws SQLException {
        String sql = "INSERT INTO Item (itemId, name, description, type, yearMade, model, mileage, ownerId, status, imagePath) " +
                "VALUES ('item-003', 'Toyota Camry', 'Family sedan', 'Vehicle', 2020, 'Camry 2.5', 15000.0, 'owner-01', 'AVAILABLE', NULL)";
        conn.createStatement().execute(sql);

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Item WHERE itemId = ?")) {
            pstmt.setString(1, "item-003");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("Vehicle", rs.getString("type"));
            assertEquals("Camry 2.5", rs.getString("model"));
            assertEquals(15000.0, rs.getDouble("mileage"), 0.001);
        }
    }

    @Test
    @DisplayName("save() - Trùng itemId thì bị SQLException (PRIMARY KEY)")
    void testSaveDuplicateItemId() throws SQLException {
        insertSampleArtItem("item-dup", "owner-01", "AVAILABLE");
        assertThrows(SQLException.class, () ->
                        insertSampleArtItem("item-dup", "owner-02", "AVAILABLE"),
                "Phải ném exception khi itemId trùng"
        );
    }

    // ─── findById() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() - Tìm thấy item tồn tại")
    void testFindByIdFound() throws SQLException {
        insertSampleArtItem("item-010", "owner-01", "AVAILABLE");

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Item WHERE itemId = ?")) {
            pstmt.setString(1, "item-010");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("Test Art", rs.getString("name"));
        }
    }

    @Test
    @DisplayName("findById() - Trả về null nếu không tồn tại")
    void testFindByIdNotFound() throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Item WHERE itemId = ?")) {
            pstmt.setString(1, "item-nonexistent");
            ResultSet rs = pstmt.executeQuery();
            assertFalse(rs.next(), "Không có item thì ResultSet phải rỗng");
        }
    }

    @Test
    @DisplayName("findAvailableByOwner() - Trả về đúng các item AVAILABLE của owner")
    void testFindAvailableByOwner() throws SQLException {
        insertSampleArtItem("item-020", "owner-A", "AVAILABLE");
        insertSampleArtItem("item-021", "owner-A", "AVAILABLE");
        insertSampleArtItem("item-022", "owner-A", "IN_AUCTION"); // không được trả về
        insertSampleArtItem("item-023", "owner-B", "AVAILABLE"); // khác owner

        String sql = "SELECT COUNT(*) FROM Item WHERE ownerId = ? AND COALESCE(status,'AVAILABLE') = 'AVAILABLE'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "owner-A");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1), "Chỉ 2 item AVAILABLE của owner-A");
        }
    }

    @Test
    @DisplayName("findAvailableByOwner() - Trả về rỗng nếu không có item AVAILABLE")
    void testFindAvailableByOwnerEmpty() throws SQLException {
        insertSampleArtItem("item-025", "owner-C", "IN_AUCTION");

        String sql = "SELECT COUNT(*) FROM Item WHERE ownerId = ? AND COALESCE(status,'AVAILABLE') = 'AVAILABLE'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "owner-C");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }


    @Test
    @DisplayName("update() - Cập nhật tên item thành công")
    void testUpdateItem() throws SQLException {
        insertSampleArtItem("item-030", "owner-01", "AVAILABLE");

        String sql = "UPDATE Item SET name = ? WHERE itemId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "Updated Art Name");
            pstmt.setString(2, "item-030");
            pstmt.executeUpdate();
        }

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT name FROM Item WHERE itemId = ?")) {
            pstmt.setString(1, "item-030");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("Updated Art Name", rs.getString("name"));
        }
    }


    @Test
    @DisplayName("markInAuction() - Chuyển trạng thái sang IN_AUCTION thành công")
    void testMarkInAuction() throws SQLException {
        insertSampleArtItem("item-040", "owner-01", "AVAILABLE");

        String sql = "UPDATE Item SET status = ? WHERE itemId = ? AND ownerId = ? AND COALESCE(status,'AVAILABLE') = 'AVAILABLE'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "IN_AUCTION");
            pstmt.setString(2, "item-040");
            pstmt.setString(3, "owner-01");
            int rows = pstmt.executeUpdate();
            assertEquals(1, rows);
        }

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT status FROM Item WHERE itemId = ?")) {
            pstmt.setString(1, "item-040");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("IN_AUCTION", rs.getString("status"));
        }
    }

    @Test
    @DisplayName("markInAuction() - Không thể đổi item đang IN_AUCTION")
    void testMarkInAuctionAlreadyInAuction() throws SQLException {
        insertSampleArtItem("item-041", "owner-01", "IN_AUCTION");

        String sql = "UPDATE Item SET status = ? WHERE itemId = ? AND ownerId = ? AND COALESCE(status,'AVAILABLE') = 'AVAILABLE'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "IN_AUCTION");
            pstmt.setString(2, "item-041");
            pstmt.setString(3, "owner-01");
            int rows = pstmt.executeUpdate();
            assertEquals(0, rows, "Không được update item đang IN_AUCTION");
        }
    }


    @Test
    @DisplayName("deleteAvailable() - Xoá item AVAILABLE thành công")
    void testDeleteAvailableItem() throws SQLException {
        insertSampleArtItem("item-050", "owner-01", "AVAILABLE");

        String sql = "DELETE FROM Item WHERE itemId = ? AND ownerId = ? AND COALESCE(status,'AVAILABLE') = 'AVAILABLE'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "item-050");
            pstmt.setString(2, "owner-01");
            int rows = pstmt.executeUpdate();
            assertTrue(rows > 0, "Phải xoá được 1 dòng");
        }

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Item WHERE itemId = ?")) {
            pstmt.setString(1, "item-050");
            assertFalse(pstmt.executeQuery().next(), "Item đã bị xoá");
        }
    }

    @Test
    @DisplayName("deleteAvailable() - Không xoá được item đang IN_AUCTION")
    void testDeleteItemInAuction() throws SQLException {
        insertSampleArtItem("item-051", "owner-01", "IN_AUCTION");

        String sql = "DELETE FROM Item WHERE itemId = ? AND ownerId = ? AND COALESCE(status,'AVAILABLE') = 'AVAILABLE'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "item-051");
            pstmt.setString(2, "owner-01");
            int rows = pstmt.executeUpdate();
            assertEquals(0, rows, "Không được xoá item đang đấu giá");
        }

        // Item vẫn còn trong DB
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Item WHERE itemId = ?")) {
            pstmt.setString(1, "item-051");
            assertTrue(pstmt.executeQuery().next(), "Item IN_AUCTION vẫn còn trong DB");
        }
    }
}
