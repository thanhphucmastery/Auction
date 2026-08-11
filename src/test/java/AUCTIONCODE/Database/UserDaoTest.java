package AUCTIONCODE.Database;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("USERDAO Tests")
class USERDAOTest extends BaseDaoTest {

    @Test
    @DisplayName("save() - Lưu Player thành công vào DB")
    void testSavePlayer() throws SQLException {
        insertSampleUser("u-001", "player1", "Player", 500.0);

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE userId = ?")) {
            pstmt.setString(1, "u-001");
            ResultSet rs = pstmt.executeQuery();

            assertTrue(rs.next(), "User phải tồn tại sau khi save");
            assertEquals("player1", rs.getString("userName"));
            assertEquals("Player", rs.getString("role"));
            assertEquals(500.0, rs.getDouble("balance"), 0.001);
        }
    }

    @Test
    @DisplayName("save() - Lưu Admin thành công (balance = 0)")
    void testSaveAdmin() throws SQLException {
        String sql = "INSERT INTO users (userId, userName, hashPassword, balance, role, fullname, email, phone, address, businessCode) " +
                "VALUES ('admin-01', 'admin', 'hashed', 0.0, 'Admin', 'Admin Name', 'admin@a.com', '09xx', 'HN', 'BIZ-001')";
        conn.createStatement().execute(sql);

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE userId = ?")) {
            pstmt.setString(1, "admin-01");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("Admin", rs.getString("role"));
            assertEquals("BIZ-001", rs.getString("businessCode"));
        }
    }

    @Test
    @DisplayName("save() - Trùng userName thì bị SQLException (UNIQUE constraint)")
    void testSaveDuplicateUsername() throws SQLException {
        insertSampleUser("u-001", "duplicateUser", "Player", 100.0);

        assertThrows(SQLException.class, () ->
                        insertSampleUser("u-002", "duplicateUser", "Player", 200.0),
                "Phải ném SQLException khi userName trùng"
        );
    }


    @Test
    @DisplayName("findByUserName() - Tìm thấy user tồn tại")
    void testFindByUserNameFound() throws SQLException {
        insertSampleUser("u-010", "alice", "Player", 300.0);

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE userName = ?")) {
            pstmt.setString(1, "alice");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "Phải tìm thấy user 'alice'");
            assertEquals("u-010", rs.getString("userId"));
        }
    }

    @Test
    @DisplayName("findByUserName() - Trả về rỗng nếu không tồn tại")
    void testFindByUserNameNotFound() throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE userName = ?")) {
            pstmt.setString(1, "nonexistent");
            ResultSet rs = pstmt.executeQuery();
            assertFalse(rs.next(), "Không tìm thấy user không tồn tại");
        }
    }


    @Test
    @DisplayName("findById() - Tìm thấy user theo ID")
    void testFindByIdFound() throws SQLException {
        insertSampleUser("u-020", "bob", "Player", 750.0);

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE userId = ?")) {
            pstmt.setString(1, "u-020");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("bob", rs.getString("userName"));
            assertEquals(750.0, rs.getDouble("balance"), 0.001);
        }
    }

    @Test
    @DisplayName("findById() - Trả về rỗng nếu ID không tồn tại")
    void testFindByIdNotFound() throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE userId = ?")) {
            pstmt.setString(1, "not-exist-id");
            ResultSet rs = pstmt.executeQuery();
            assertFalse(rs.next());
        }
    }

    @Test
    @DisplayName("update() - Cập nhật balance thành công")
    void testUpdateBalance() throws SQLException {
        insertSampleUser("u-030", "charlie", "Player", 100.0);

        String updateSql = "UPDATE users SET balance = ? WHERE userId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setDouble(1, 999.0);
            pstmt.setString(2, "u-030");
            int rows = pstmt.executeUpdate();
            assertEquals(1, rows, "Phải update đúng 1 dòng");
        }

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT balance FROM users WHERE userId = ?")) {
            pstmt.setString(1, "u-030");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(999.0, rs.getDouble("balance"), 0.001);
        }
    }

    @Test
    @DisplayName("update() - Update user không tồn tại thì 0 rows bị ảnh hưởng")
    void testUpdateBalanceNotExist() throws SQLException {
        String updateSql = "UPDATE users SET balance = ? WHERE userId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setDouble(1, 500.0);
            pstmt.setString(2, "ghost-id");
            int rows = pstmt.executeUpdate();
            assertEquals(0, rows, "Không có dòng nào bị ảnh hưởng");
        }
    }



    @Test
    @DisplayName("delete() - Xoá user thành công")
    void testDeleteUser() throws SQLException {
        insertSampleUser("u-040", "deleteMe", "Player", 0.0);

        String deleteSql = "DELETE FROM users WHERE userId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setString(1, "u-040");
            int rows = pstmt.executeUpdate();
            assertTrue(rows > 0, "Phải xoá được ít nhất 1 dòng");
        }

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE userId = ?")) {
            pstmt.setString(1, "u-040");
            assertFalse(pstmt.executeQuery().next(), "User đã bị xoá, không còn trong DB");
        }
    }

    @Test
    @DisplayName("delete() - Xoá user không tồn tại trả về 0 rows")
    void testDeleteNonExistentUser() throws SQLException {
        String deleteSql = "DELETE FROM users WHERE userId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setString(1, "ghost-user");
            int rows = pstmt.executeUpdate();
            assertEquals(0, rows);
        }
    }


    @Test
    @DisplayName("updateInformation() - Cập nhật thông tin cá nhân thành công")
    void testUpdateInformation() throws SQLException {
        insertSampleUser("u-050", "diana", "Player", 0.0);

        String sql = "UPDATE users SET fullname = ?, email = ?, phone = ?, address = ? WHERE userId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "Diana Prince");
            pstmt.setString(2, "diana@new.com");
            pstmt.setString(3, "0999888777");
            pstmt.setString(4, "Ho Chi Minh");
            pstmt.setString(5, "u-050");
            pstmt.executeUpdate();
        }

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE userId = ?")) {
            pstmt.setString(1, "u-050");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("Diana Prince", rs.getString("fullname"));
            assertEquals("diana@new.com", rs.getString("email"));
            assertEquals("Ho Chi Minh", rs.getString("address"));
        }
    }

    @Test
    @DisplayName("findByRole() - Lọc đúng theo role Player")
    void testFindByRolePlayer() throws SQLException {
        insertSampleUser("u-060", "player_a", "Player", 0.0);
        insertSampleUser("u-061", "player_b", "Player", 0.0);
        insertSampleUser("u-062", "admin_a", "Admin", 0.0);

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE role = ?")) {
            pstmt.setString(1, "Player");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1), "Phải có đúng 2 Player");
        }
    }

    @Test
    @DisplayName("findByRole() - Trả về rỗng nếu không có user với role đó")
    void testFindByRoleEmpty() throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE role = ?")) {
            pstmt.setString(1, "Moderator");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }
}