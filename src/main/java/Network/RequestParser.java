package Network;

import AuthModule.AuthService;
import AuthModule.InMemorySessionManager;
import Manager.AuctionManager;
import Manager.UserManager;
import Model.Auction.AuctionRoom;
import Model.Auction.AuctionScheduler;
import Model.Item.Item;
import Model.Item.ItemFactory;
import Model.User.Admin;
import Model.User.Player;
import Model.User.User;
import Model.User.UserInformation;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

public class RequestParser {
    private static final InMemorySessionManager sessionManager = InMemorySessionManager.getInstance();
    private static final AuctionManager auctionManager = AuctionManager.getInstance();
    private static final AuctionScheduler auctionScheduler = AuctionScheduler.getInstance();
    private static final UserManager userManager = UserManager.getInstance();

    public static String handle(String request) {
        if (request == null || request.isBlank()) {
            return "ERROR:Empty request";
        }

        String[] parts = request.split(":", -1);
        String action = parts[0];
        try {
            return switch (action) {
                case "REGISTER" -> handleRegister(parts);
                case "REGISTER_ADMIN" -> handleRegisterAdmin(parts);
                case "LOGIN" -> handleLogin(parts);
                case "LOGOUT" -> handleLogout(parts);
                case "CREATE_AUCTION" -> handleCreateAuction(parts);
                case "JOIN_AUCTION" -> handleJoinAuction(parts);
                case "BID" -> handleBid(parts);
                case "END_AUCTION" -> handleEndAuction(parts);
                case "GET_AUCTIONS" -> handleGetAuctions(parts);
                case "ADMIN_GET_USERS" -> handleAdminGetUsers(parts);
                case "DEPOSIT" -> handleDeposit(parts);
                default -> "ERROR:Unknown action";
            };
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private static String handleRegister(String[] parts) {
        requireLength(parts, 7, "REGISTER:username:password:fullName:phone:email:address");
        UserInformation information = new UserInformation(parts[6], parts[4], parts[5], parts[3]);
        boolean registered = AuthService.registerPlayer(parts[1], information, parts[2]);
        return registered ? "OK:Registered" : "ERROR:Username already exists";
    }

    private static String handleRegisterAdmin(String[] parts) {
        requireLength(parts, 8, "REGISTER_ADMIN:username:password:fullName:phone:email:address:businessCode");
        UserInformation information = new UserInformation(parts[6], parts[4], parts[5], parts[3]);
        boolean registered = AuthService.registerAdmin(parts[1], information, parts[2], parts[7]);
        return registered ? "OK:Admin registered" : "ERROR:Invalid business code or username already exists";
    }

    private static String handleLogin(String[] parts) {
        requireLength(parts, 3, "LOGIN:username:password");
        String sessionId = AuthService.login(parts[1], parts[2]);
        if (sessionId != null) {
            return "OK:" + sessionId;
        }
        return "ERROR:Invalid username or password";
    }

    private static String handleLogout(String[] parts) {
        requireLength(parts, 2, "LOGOUT:sessionId");
        return AuthService.logout(parts[1]) ? "OK:Logged out" : "ERROR:Invalid session";
    }

    private static String handleDeposit(String[] parts) {
        requireLength(parts, 3, "DEPOSIT:sessionId:amount");
        Player player = requirePlayer(parts[1]);
        double amount = parsePositiveDouble(parts[2], "amount");
        player.setPlayerBalance(player.getPlayerBalance() + amount);
        return "OK:" + player.getPlayerBalance();
    }

    private static String handleGetAuctions(String[] parts) {
        requireLength(parts, 2, "GET_AUCTIONS:sessionId");
        requireUser(parts[1]);
        String payload = auctionManager.getAllAuctions().stream()
                .map(room -> room.getId() + "|" + room.getStatus() + "|"
                        + room.getCurrentPrice() + "|" + room.getEndTime())
                .collect(Collectors.joining(";"));
        return "OK:" + payload;
    }

    private static String handleAdminGetUsers(String[] parts) {
        requireLength(parts, 2, "ADMIN_GET_USERS:sessionId");
        requireAdmin(parts[1]);
        String payload = userManager.getAllUsers().stream()
                .map(user -> user.getUserId() + "|" + roleOf(user) + "|" + user.getUserName() + "|"
                        + user.getUserInformation().getFullName())
                .collect(Collectors.joining(";"));
        return "OK:" + payload;
    }

    private static String handleCreateAuction(String[] parts) {
        requireLength(parts, 6, "CREATE_AUCTION:sessionId:itemName:startPrice:stepPrice:durationMinutes");
        User seller = requireUser(parts[1]);
        double startPrice = parsePositiveDouble(parts[3], "startPrice");
        double stepPrice = parsePositiveDouble(parts[4], "stepPrice");
        long durationMinutes = parsePositiveLong(parts[5], "durationMinutes");

        String auctionId = UUID.randomUUID().toString();
        Item item = ItemFactory.createArt(
                UUID.randomUUID().toString(),
                parts[2],
                "Created from network request",
                seller.getUserName(),
                LocalDateTime.now().getYear()
        );
        AuctionRoom room = new AuctionRoom(
                auctionId,
                seller.getUserId(),
                startPrice,
                stepPrice,
                LocalDateTime.now().plusMinutes(durationMinutes),
                item
        );
        room.startAuctionRoom();
        auctionManager.addAuction(room);
        auctionScheduler.scheduleEnd(room);
        return "OK:" + auctionId;
    }

    private static String handleJoinAuction(String[] parts) {
        requireLength(parts, 3, "JOIN_AUCTION:sessionId:auctionId");
        User user = requireUser(parts[1]);
        AuctionRoom room = requireAuction(parts[2]);
        room.joinAuctionRoom(user);
        return "OK:Joined";
    }

    private static String handleBid(String[] parts) {
        requireLength(parts, 4, "BID:sessionId:auctionId:amount");
        Player player = requirePlayer(parts[1]);
        AuctionRoom room = requireAuction(parts[2]);
        double amount = parsePositiveDouble(parts[3], "amount");
        return room.placeBid(player, amount) ? "OK:Bid accepted" : "ERROR:Bid rejected";
    }

    private static String handleEndAuction(String[] parts) {
        requireLength(parts, 3, "END_AUCTION:sessionId:auctionId");
        User user = requireUser(parts[1]);
        AuctionRoom room = requireAuction(parts[2]);
        if (!user.getUserId().equals(room.getSellerId()) && !(user instanceof Admin)) {
            return "ERROR:Only seller or admin can end this auction";
        }
        room.endAuction();
        return "OK:Auction ended";
    }

    private static User requireUser(String sessionId) {
        if (!sessionManager.isValidSession(sessionId)) {
            throw new IllegalArgumentException("Invalid session");
        }
        User user = userManager.getUser(sessionManager.getUserId(sessionId));
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }

    private static Admin requireAdmin(String sessionId) {
        User user = requireUser(sessionId);
        if (!(user instanceof Admin admin)) {
            throw new IllegalArgumentException("Only admin accounts can do this action");
        }
        return admin;
    }

    private static String roleOf(User user) {
        if (user instanceof Admin) {
            return "ADMIN";
        }
        if (user instanceof Player) {
            return "PLAYER";
        }
        return "USER";
    }

    private static Player requirePlayer(String sessionId) {
        User user = requireUser(sessionId);
        if (!(user instanceof Player player)) {
            throw new IllegalArgumentException("Only player accounts can do this action");
        }
        return player;
    }

    private static AuctionRoom requireAuction(String auctionId) {
        AuctionRoom room = auctionManager.getAuction(auctionId);
        if (room == null) {
            throw new IllegalArgumentException("Auction not found");
        }
        return room;
    }

    private static void requireLength(String[] parts, int expected, String format) {
        if (parts.length < expected) {
            throw new IllegalArgumentException("Invalid request format. Expected " + format);
        }
    }

    private static double parsePositiveDouble(String value, String fieldName) {
        double parsed = Double.parseDouble(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return parsed;
    }

    private static long parsePositiveLong(String value, String fieldName) {
        long parsed = Long.parseLong(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return parsed;
    }
}
