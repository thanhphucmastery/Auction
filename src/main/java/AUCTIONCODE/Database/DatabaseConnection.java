package AUCTIONCODE.Database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String DB_PATH = "jdbc:sqlite:" + resolveDatabasePath();
    private static final String RESOLVED_DB_PATH = DB_PATH.substring("jdbc:sqlite:".length());
    private static volatile DatabaseConnection instance;

    public Connection newConnection() throws SQLException {
        return DriverManager.getConnection(DB_PATH);
    }

    public static DatabaseConnection getInstance() {
        DatabaseConnection res = instance;
        if (res == null) {
            synchronized (DatabaseConnection.class) {
                res = instance;
                if (res == null) {
                    res = instance = new DatabaseConnection();
                }
            }
        }
        return res;
    }

    public static String getDatabasePath() {
        return RESOLVED_DB_PATH;
    }

    public void shutdown() {
        System.out.println("All database connections have been closed");
    }

    private static String resolveDatabasePath() {
        Path current = Path.of("").toAbsolutePath();
        Path probe = current;
        while (probe != null) {
            Path projectDatabase = probe.resolve(Path.of("data", "database.db"));
            if (Files.exists(projectDatabase)) {
                return normalize(projectDatabase);
            }
            probe = probe.getParent();
        }

        Path currentDirDatabase = current.resolve("database.db");
        if (Files.exists(currentDirDatabase)) {
            return normalize(currentDirDatabase);
        }
        return normalize(currentDirDatabase);
    }

    private static String normalize(Path path) {
        return path.toAbsolutePath().toString().replace('\\', '/');
    }
}
