package AUCTIONCODE.Database;

import AUCTIONCODE.AuthModule.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseIntializer {
    public static void Initialize() {
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             Statement stmt = conn.createStatement()) {

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
                        businessCode TEXT)
            """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS Item(
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
                        mileage TEXT
                    )
            """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS Auctions(
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

            addColumnIfMissing(stmt, "Auctions", "openTime", "TEXT");
            addColumnIfMissing(stmt, "Item", "ownerId", "TEXT");
            addColumnIfMissing(stmt, "Item", "status", "TEXT DEFAULT 'AVAILABLE'");
            addColumnIfMissing(stmt, "Item", "imagePath", "TEXT");

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS bidTransactions(
                        transactionId TEXT PRIMARY KEY,
                        auctionId TEXT,
                        bidderId TEXT,
                        amount REAL,
                        timestamp TEXT,
                        FOREIGN KEY (auctionId) REFERENCES Auctions(auctionId),
                        FOREIGN KEY (bidderId) REFERENCES users(userId)
                    )
            """);

            resetDefaultUsers(conn);
            System.out.println("KHOI TAO DATABASE THANH CONG");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addColumnIfMissing(Statement stmt, String table, String column, String type) throws SQLException {
        try {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        } catch (SQLException e) {
            if (!e.getMessage().toLowerCase().contains("duplicate column")) {
                throw e;
            }
        }
    }

    private static void resetDefaultUsers(Connection conn) throws SQLException {
        upsertDefaultUser(
                conn,
                "user-test",
                "test",
                PasswordHasher.hash("1234"),
                0.0,
                "Player",
                "Test User",
                "test@example.com",
                "0123456789",
                "Ha Noi",
                null
        );
        upsertDefaultUser(
                conn,
                "user-admin",
                "admin",
                PasswordHasher.hash("1234"),
                0.0,
                "Admin",
                "Administrator",
                "admin@example.com",
                "0987654321",
                "",
                "BIZ-001"
        );
        System.out.println("Default accounts: test/1234 and admin/1234");
    }

    private static void upsertDefaultUser(
            Connection conn,
            String userId,
            String userName,
            String hashPassword,
            double balance,
            String role,
            String fullName,
            String email,
            String phone,
            String address,
            String businessCode
    ) throws SQLException {
        String sql = """
                INSERT INTO users (userId,userName,hashPassword,balance,role,fullname,email,phone,address,businessCode)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(userName) DO NOTHING
                """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, userName);
            pstmt.setString(3, hashPassword);
            pstmt.setDouble(4, balance);
            pstmt.setString(5, role);
            pstmt.setString(6, fullName);
            pstmt.setString(7, email);
            pstmt.setString(8, phone);
            pstmt.setString(9, address);
            pstmt.setString(10, businessCode);
            pstmt.executeUpdate();
        }
    }
}
