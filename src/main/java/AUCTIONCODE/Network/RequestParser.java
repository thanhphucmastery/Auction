package AUCTIONCODE.Network;

import AUCTIONCODE.Database.DatabaseBootstrap;

import java.util.Map;

public class RequestParser {
    private static final RequestContext context = new RequestContext();
    private static final Map<String, RequestHandler> handlers = createHandlers();

    static {
        DatabaseBootstrap.initializeAndLoad();
    }

    public static String handle(String request) {
        if (request == null || request.isBlank()) {
            return "ERROR:Empty request";
        }

        String[] parts = request.split(":", -1);
        RequestHandler handler = handlers.get(parts[0]);
        if (handler == null) {
            return "ERROR:Unknown action";
        }

        try {
            return handler.handle(parts);
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private static Map<String, RequestHandler> createHandlers() {
        AuthRequestHandler auth = new AuthRequestHandler(context);
        AuctionRequestHandler auction = new AuctionRequestHandler(context);
        ItemRequestHandler item = new ItemRequestHandler(context);
        ProfileRequestHandler profile = new ProfileRequestHandler(context);
        AdminRequestHandler admin = new AdminRequestHandler(context);

        return Map.ofEntries(
                Map.entry("REGISTER", auth::handleRegister),
                Map.entry("REGISTER_ADMIN", auth::handleRegisterAdmin),
                Map.entry("LOGIN", auth::handleLogin),
                Map.entry("RESET_PASSWORD", auth::handleResetPassword),
                Map.entry("RELOAD_DB", parts -> handleReloadDb()),
                Map.entry("LOGOUT", auth::handleLogout),
                Map.entry("JOIN_AUCTION", auction::handleJoinAuction),
                Map.entry("BID", auction::handleBid),
                Map.entry("END_AUCTION", auction::handleEndAuction),
                Map.entry("GET_AUCTIONS", auction::handleGetAuctions),
                Map.entry("GET_AUCTION_DETAIL", auction::handleGetAuctionDetail),
                Map.entry("GET_TRANSACTION_HISTORY", auction::handleGetTransactionHistory),
                Map.entry("ADD_ITEM", item::handleAddItem),
                Map.entry("GET_MY_ITEMS", item::handleGetMyItems),
                Map.entry("UPDATE_ITEM", item::handleUpdateItem),
                Map.entry("DELETE_ITEM", item::handleDeleteItem),
                Map.entry("CREATE_AUCTION_FROM_ITEM", auction::handleCreateAuctionFromItem),
                Map.entry("GET_PROFILE", profile::handleGetProfile),
                Map.entry("UPDATE_PROFILE", profile::handleUpdateProfile),
                Map.entry("DEPOSIT", profile::handleDeposit),
                Map.entry("WITHDRAW", profile::handleWithdraw),
                Map.entry("ADMIN_GET_USERS", admin::handleAdminGetUsers),
                Map.entry("ADMIN_END_AUCTION", admin::handleAdminEndAuction),
                Map.entry("ADMIN_EXTEND_AUCTION", admin::handleAdminExtendAuction),
                Map.entry("ADMIN_DELETE_AUCTION", admin::handleAdminDeleteAuction),
                Map.entry("ADMIN_DELETE_USER", admin::handleAdminDeleteUser)
        );
    }

    private static String handleReloadDb() {
        DatabaseBootstrap.reloadFromDatabase();
        return "OK:Reloaded";
    }
}
