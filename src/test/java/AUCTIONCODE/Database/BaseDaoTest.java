package AUCTIONCODE.Database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BaseDaoTest {

    protected Connection conn;

    @BeforeEach
    void setUpDatabase() throws SQLException {
        // Tạo in-memory SQLite DB - không ảnh hưởng file thật
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        createTables();
    }

    @AfterEach
    void tearDownDatabase() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    userId TEXT PRIMARY KEY,
                    userName TEXT UNIQUE NOT NULL,
                    hashPassword TEXT NOT NULL,
                    balance REAL DEFAULT 0.0,
                    role TEXT NOT NULL,
                    fullname TEXT,
                    email TEXT,
                    phone TEXT,
                    address TEXT,
                    businessCode TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Item (
                    itemId TEXT PRIMARY KEY,
                    name TEXT,
                    description TEXT,
                    type TEXT,
                    artist TEXT,
                    yearCreated INTEGER,
                    brand TEXT,
                    yearMade INTEGER,
                    warranty INTEGER,
                    model TEXT,
                    mileage REAL,
                    ownerId TEXT,
                    status TEXT DEFAULT 'AVAILABLE',
                    imagePath TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Auctions (
                    auctionId TEXT PRIMARY KEY,
                    sellerId TEXT,
                    currentPrice REAL,
                    stepPrice REAL,
                    openTime TEXT,
                    endTime TEXT,
                    status TEXT,
                    itemId TEXT,
                    highestBidderId TEXT,
                    FOREIGN KEY (itemId) REFERENCES Item(itemId)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bidTransactions (
                    transactionId TEXT PRIMARY KEY,
                    auctionId TEXT,
                    bidderId TEXT,
                    amount REAL,
                    timestamp TEXT,
                    FOREIGN KEY (auctionId) REFERENCES Auctions(auctionId),
                    FOREIGN KEY (bidderId) REFERENCES users(userId)
                )
            """);
        }
    }


    protected void insertSampleUser(String userId, String userName, String role, double balance) throws SQLException {
        String sql = "INSERT INTO users (userId, userName, hashPassword, balance, role, fullname, email, phone, address) " +
                "VALUES (?, ?, 'hashed', ?, ?, 'Full Name', 'email@test.com', '0123', 'Hanoi')";
        try (var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, userName);
            pstmt.setDouble(3, balance);
            pstmt.setString(4, role);
            pstmt.executeUpdate();
        }
    }


    protected void insertSampleArtItem(String itemId, String ownerId, String status) throws SQLException {
        String sql = "INSERT INTO Item (itemId, name, description, type, artist, yearCreated, ownerId, status, imagePath) " +
                "VALUES (?, 'Test Art', 'A painting', 'Art', 'Van Gogh', 1889, ?, ?, NULL)";
        try (var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            pstmt.setString(2, ownerId);
            pstmt.setString(3, status);
            pstmt.executeUpdate();
        }
    }


    protected void insertSampleAuction(String auctionId, String sellerId, String itemId,
                                       String status, String highestBidderId) throws SQLException {
        String sql = "INSERT INTO Auctions (auctionId, sellerId, currentPrice, stepPrice, openTime, endTime, status, itemId, highestBidderId) " +
                "VALUES (?, ?, 100.0, 10.0, '2025-01-01T10:00', '2025-01-02T10:00', ?, ?, ?)";
        try (var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            pstmt.setString(2, sellerId);
            pstmt.setString(3, status);
            pstmt.setString(4, itemId);
            if (highestBidderId != null) pstmt.setString(5, highestBidderId);
            else pstmt.setNull(5, java.sql.Types.VARCHAR);
            pstmt.executeUpdate();
        }
    }
}