package AUCTIONCODE.Database;

import AUCTIONCODE.AuthModule.AuthService;
import AUCTIONCODE.Manager.AuctionManager;
import AUCTIONCODE.Manager.UserManager;

public final class DatabaseBootstrap {
    private static boolean initialized;

    private DatabaseBootstrap() {
    }

    public static synchronized void initializeAndLoad() {
        if (initialized) {
            return;
        }

        System.out.println("SQLite database: " + DatabaseConnection.getDatabasePath());
        DatabaseIntializer.Initialize();

        System.out.println("--- Load users ---");
        new USERDAO().loadAll();

        System.out.println("--- Load auctions ---");
        new AuctionDAO().loadAll();

        System.out.println("--- Load bid transactions ---");
        new BidTransactionDAO().loadAll();

        System.out.println("=== DB SAN SANG ===");
        initialized = true;
    }

    public static synchronized void reloadFromDatabase() {
        AuthService.clearLoadedUsers();
        UserManager.getInstance().clear();
        AuctionManager.getInstance().clear();
        initialized = false;
        initializeAndLoad();
    }
}
