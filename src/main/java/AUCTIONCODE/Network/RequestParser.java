package AUCTIONCODE.Network;

import AUCTIONCODE.AuthModule.AuthService;
import AUCTIONCODE.AuthModule.InMemorySessionManager;
import AUCTIONCODE.Database.AuctionDAO;
import AUCTIONCODE.Database.BidTransactionDAO;
import AUCTIONCODE.Database.DatabaseBootstrap;
import AUCTIONCODE.Database.ItemDAO;
import AUCTIONCODE.Database.USERDAO;
import AUCTIONCODE.Manager.AuctionManager;
import AUCTIONCODE.Manager.UserManager;
import AUCTIONCODE.Model.Auction.AuctionRoom;
import AUCTIONCODE.Model.Auction.BidTransaction;
import AUCTIONCODE.Model.Item.Item;
import AUCTIONCODE.Model.Item.ItemFactory;
import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.User;
import AUCTIONCODE.Model.User.UserInformation;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

public class RequestParser {
    private static final InMemorySessionManager sessionManager = InMemorySessionManager.getInstance();
    private static final AuctionManager auctionManager = AuctionManager.getInstance();
    private static final UserManager userManager = UserManager.getInstance();
    private static final USERDAO userDAO = new USERDAO();
    private static final AuctionDAO auctionDAO = new AuctionDAO();
    private static final ItemDAO itemDAO = new ItemDAO();
    private static final BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();

    static {
        DatabaseBootstrap.initializeAndLoad();
    }

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
                case "RESET_PASSWORD" -> handleResetPassword(parts);
                case "RELOAD_DB" -> handleReloadDb();
                case "LOGOUT" -> handleLogout(parts);
                case "CREATE_AUCTION" -> handleCreateAuction(parts);
                case "JOIN_AUCTION" -> handleJoinAuction(parts);
                case "BID" -> handleBid(parts);
                case "END_AUCTION" -> handleEndAuction(parts);
                case "GET_AUCTIONS" -> handleGetAuctions(parts);
                case "GET_AUCTION_DETAIL" -> handleGetAuctionDetail(parts);
                case "GET_TRANSACTION_HISTORY" -> handleGetTransactionHistory(parts);
                case "ADD_ITEM" -> handleAddItem(parts);
                case "GET_MY_ITEMS" -> handleGetMyItems(parts);
                case "UPDATE_ITEM" -> handleUpdateItem(parts);
                case "DELETE_ITEM" -> handleDeleteItem(parts);
                case "CREATE_AUCTION_FROM_ITEM" -> handleCreateAuctionFromItem(parts);
                case "GET_PROFILE" -> handleGetProfile(parts);
                case "UPDATE_PROFILE" -> handleUpdateProfile(parts);
                case "DEPOSIT" -> handleDeposit(parts);
                case "WITHDRAW" -> handleWithdraw(parts);
                case "ADMIN_GET_USERS" -> handleAdminGetUsers(parts);
                case "ADMIN_END_AUCTION" -> handleAdminEndAuction(parts);
                case "ADMIN_EXTEND_AUCTION" -> handleAdminExtendAuction(parts);
                case "ADMIN_DELETE_AUCTION" -> handleAdminDeleteAuction(parts);
                case "ADMIN_DELETE_USER" -> handleAdminDeleteUser(parts);
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
        if (registered) {
            userDAO.save(AuthService.findUserByUserName(parts[1]));
        }
        return registered ? "OK:Registered" : "ERROR:Username already exists";
    }
    private static String handleRegisterAdmin(String[] parts) {
        requireLength(parts, 8, "REGISTER_ADMIN:username:password:fullName:phone:email:address:businessCode");
        UserInformation information = new UserInformation(parts[6], parts[4], parts[5], parts[3]);
        boolean registered = AuthService.registerAdmin(parts[1], information, parts[2], parts[7]);
        if (registered) {
            userDAO.save(AuthService.findUserByUserName(parts[1]));
        }
        return registered ? "OK:Registered" : "ERROR:Invalid business code or username already exists";
    }

    private static String handleLogin(String[] parts) {
        requireLength(parts, 3, "LOGIN:username:password");
        String sessionId = AuthService.login(parts[1], parts[2]);
        if (sessionId == null) {
            DatabaseBootstrap.reloadFromDatabase();
            sessionId = AuthService.login(parts[1], parts[2]);
        }
        if (sessionId == null && "test".equals(parts[1]) && "1234".equals(parts[2])) {
            AuthService.ensureDefaultPlayer();
            sessionId = AuthService.login(parts[1], parts[2]);
        }
        if (sessionId == null && "admin".equals(parts[1]) && "1234".equals(parts[2])) {
            AuthService.ensureDefaultAdmin();
            sessionId = AuthService.login(parts[1], parts[2]);
        }
        if (sessionId != null) {
            return "OK:" + sessionId;
        }
        return "ERROR:Invalid username or password";
    }

    private static String handleResetPassword(String[] parts) {
        requireLength(parts, 4, "RESET_PASSWORD:username:email:newPassword");
        if (parts[3].length() < 4) {
            return "ERROR:Password must contain at least 4 characters";
        }
        if (AuthService.findUserByUserName(parts[1]) == null) {
            DatabaseBootstrap.reloadFromDatabase();
        }
        boolean reset = AuthService.resetPassword(parts[1], parts[2], parts[3]);
        if (!reset) {
            return "ERROR:Username and email do not match";
        }
        User user = AuthService.findUserByUserName(parts[1]);
        userDAO.updatePassword(user.getUserId(), user.getUserPassword());
        return "OK:Password reset";
    }

    private static String handleReloadDb() {
        DatabaseBootstrap.reloadFromDatabase();
        return "OK:Reloaded";
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
        userDAO.update(player.getPlayerBalance(), player.getUserId());
        return "OK:" + player.getPlayerBalance();
    }
    private static String handleWithdraw(String[] parts) {
        requireLength(parts, 3, "WITHDRAW:sessionId:amount");

        Player player = requirePlayer(parts[1]);
        double amount = parsePositiveDouble(parts[2], "amount");

        if (player.getPlayerBalance() < amount) {
            return "ERROR:Insufficient balance";
        }

        player.setPlayerBalance(player.getPlayerBalance() - amount);
        userDAO.update(player.getPlayerBalance(), player.getUserId());
        return "OK:" + player.getPlayerBalance();
    }

    private static String handleGetProfile(String[] parts) {
        requireLength(parts, 2, "GET_PROFILE:sessionId");
        User user = requireUser(parts[1]);
        UserInformation info = user.getUserInformation();
        double balance = user instanceof Player player ? player.getPlayerBalance() : 0.0;
        return "OK:" + user.getUserName() + "|" + info.getFullName() + "|"
                + info.getPhoneNumber() + "|" + info.getEmail() + "|"
                + info.getAddress() + "|" + user.getUserRole() + "|" + balance;
    }

    private static String handleUpdateProfile(String[] parts) {
        requireLength(parts, 6, "UPDATE_PROFILE:sessionId:fullName:phone:email:address");
        User user = requireUser(parts[1]);
        user.setUserInformation(new UserInformation(parts[5], parts[3], parts[4], parts[2]));
        userDAO.updateInformation(user);
        return "OK:Profile updated";
    }

    private static String handleGetAuctions(String[] parts) {
        requireLength(parts, 2, "GET_AUCTIONS:sessionId");
        requireUser(parts[1]);
        DatabaseBootstrap.reloadFromDatabase();
        requireUser(parts[1]);
        String payload = auctionManager.getAllAuctions().stream()
                .peek(RequestParser::syncAuctionStatusByClock)
                .map(room -> room.getId() + "|" + room.getStatus() + "|"
                        + room.getCurrentPrice() + "|" + room.getEndTime() + "|"
                        + room.getItem().getName() + "|"
                        + safe(sellerUserName(room.getSellerId())) + "|"
                        + safe(sellerDisplayName(room.getSellerId())))
                .collect(Collectors.joining(";"));
        return "OK:" + payload;
    }

    private static String handleGetAuctionDetail(String[] parts) {
        requireLength(parts, 3, "GET_AUCTION_DETAIL:sessionId:auctionId");
        User viewer = requireUser(parts[1]);
        AuctionRoom room = requireAuction(parts[2]);
        syncAuctionStatusByClock(room);
        Item item = room.getItem();
        String highestBidderId = room.getHighestBidderId();
        String highestBidderName = displayUserName(highestBidderId);
        String sellerName = sellerDisplayName(room.getSellerId());
        if (sellerName.isBlank()) {
            sellerName = sellerUserName(room.getSellerId());
        }
        String history = room.getBidHistory().stream()
                .map(transaction -> transaction.getTimestamp() + ","
                        + safe(transaction.getBidderId()) + ","
                        + safe(displayUserName(transaction.getBidderId())) + ","
                        + transaction.getAmount())
                .collect(Collectors.joining(";"));
        return "OK:" + room.getId() + "|"
                + safe(room.getItem().getName()) + "|"
                + room.getCurrentPrice() + "|"
                + room.getStepPrice() + "|"
                + room.getStatus() + "|"
                + room.getEndTime() + "|"
                + safe(highestBidderId) + "|"
                + safe(highestBidderName) + "|"
                + history + "|"
                + room.getOpenTime() + "|"
                + safe(item.getImagePath()) + "|"
                + safe(sellerName) + "|"
                + safe(itemDAO.getType(item)) + "|"
                + safe(item.getDescription()) + "|"
                + safe(itemAttributes(item)) + "|"
                + viewer.getUserId().equals(highestBidderId);
    }

    private static String handleGetTransactionHistory(String[] parts) {
        requireLength(parts, 2, "GET_TRANSACTION_HISTORY:sessionId");
        User user = requireUser(parts[1]);
        DatabaseBootstrap.reloadFromDatabase();
        requireUser(parts[1]);
        return "OK:" + auctionManager.getAllAuctions().stream()
                .peek(RequestParser::syncAuctionStatusByClock)
                .flatMap(room -> transactionRowsForUser(room, user).stream())
                .collect(Collectors.joining(";"));
    }

    private static String handleCreateAuction(String[] parts) {
        requireLength(parts, 6, "CREATE_AUCTION:sessionId:itemName:startPrice:stepPrice:durationMinutes");
        User seller = requireUser(parts[1]);
        double startPrice = parsePositiveDouble(parts[3], "startPrice");
        double stepPrice = parsePositiveDouble(parts[4], "stepPrice");
        long startDelayMinutes = 0;
        long durationMinutes;
        if (parts.length >= 7) {
            startDelayMinutes = parseNonNegativeLong(parts[5], "startDelayMinutes");
            durationMinutes = parsePositiveLong(parts[6], "durationMinutes");
        } else {
            durationMinutes = parsePositiveLong(parts[5], "durationMinutes");
        }
        LocalDateTime openTime = LocalDateTime.now().plusMinutes(startDelayMinutes);
        LocalDateTime endTime = openTime.plusMinutes(durationMinutes);

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
                openTime,
                endTime,
                item,
                null
        );
        room.startAuctionRoom();
        auctionManager.addAuction(room);
        auctionDAO.save(room, item);
        return "OK:" + auctionId;
    }

    private static String handleAddItem(String[] parts) {
        requireLength(parts, 8, "ADD_ITEM:sessionId:type:name:description:extra1:extra2:extra3:imagePath");
        User owner = requireUser(parts[1]);
        Item item = createItemFromParts(UUID.randomUUID().toString(), parts[2], parts[3], parts[4], parts[5], parts[6], parts[7], parts.length >= 9 ? parts[8] : "");
        itemDAO.saveInventoryItem(item, owner.getUserId());
        return "OK:" + item.getId();
    }

    private static String handleGetMyItems(String[] parts) {
        requireLength(parts, 2, "GET_MY_ITEMS:sessionId");
        User owner = requireUser(parts[1]);
        String payload = itemDAO.findAvailableByOwner(owner.getUserId()).stream()
                .map(RequestParser::itemPayload)
                .collect(Collectors.joining(";"));
        return "OK:" + payload;
    }

    private static String handleUpdateItem(String[] parts) {
        requireLength(parts, 9, "UPDATE_ITEM:sessionId:itemId:type:name:description:extra1:extra2:extra3:imagePath");
        User owner = requireUser(parts[1]);
        Item current = itemDAO.findAvailableByOwner(parts[2], owner.getUserId());
        if (current == null) {
            return "ERROR:Item not found or already in auction";
        }
        Item item = createItemFromParts(parts[2], parts[3], parts[4], parts[5], parts[6], parts[7], parts[8], parts.length >= 10 ? parts[9] : "");
        itemDAO.updateInventoryItem(item, owner.getUserId());
        return "OK:Item updated";
    }

    private static String handleDeleteItem(String[] parts) {
        requireLength(parts, 3, "DELETE_ITEM:sessionId:itemId");
        User owner = requireUser(parts[1]);
        return itemDAO.deleteAvailable(parts[2], owner.getUserId()) ? "OK:Item deleted" : "ERROR:Item not found or already in auction";
    }

    private static String handleCreateAuctionFromItem(String[] parts) {
        requireLength(parts, 7, "CREATE_AUCTION_FROM_ITEM:sessionId:itemId:startPrice:stepPrice:startDelayMinutes:durationMinutes");
        User seller = requireUser(parts[1]);
        Item item = itemDAO.findAvailableByOwner(parts[2], seller.getUserId());
        if (item == null) {
            return "ERROR:Item not found or already in auction";
        }

        double startPrice = parsePositiveDouble(parts[3], "startPrice");
        double stepPrice = parsePositiveDouble(parts[4], "stepPrice");
        long startDelayMinutes = parseNonNegativeLong(parts[5], "startDelayMinutes");
        long durationMinutes = parsePositiveLong(parts[6], "durationMinutes");
        LocalDateTime openTime = LocalDateTime.now().plusMinutes(startDelayMinutes);
        LocalDateTime endTime = openTime.plusMinutes(durationMinutes);

        String auctionId = UUID.randomUUID().toString();
        AuctionRoom room = new AuctionRoom(
                auctionId,
                seller.getUserId(),
                startPrice,
                stepPrice,
                openTime,
                endTime,
                item,
                null
        );
        room.startAuctionRoom();
        auctionManager.addAuction(room);
        auctionDAO.saveWithExistingItem(room, item);
        itemDAO.markInAuction(item.getId(), seller.getUserId());
        return "OK:" + auctionId;
    }

    private static String handleJoinAuction(String[] parts) {
        requireLength(parts, 3, "JOIN_AUCTION:sessionId:auctionId");
        User user = requireUser(parts[1]);
        AuctionRoom room = requireAuction(parts[2]);
        syncAuctionStatusByClock(room);
        room.joinAuctionRoom(user);
        return "OK:Joined";
    }

    private static String handleBid(String[] parts) {
        requireLength(parts, 4, "BID:sessionId:auctionId:amount");
        Player player = requirePlayer(parts[1]);
        AuctionRoom room = requireAuction(parts[2]);
        syncAuctionStatusByClock(room);
        double amount = parsePositiveDouble(parts[3], "amount");
        String previousHighestBidderId = room.getHighestBidderId();
        boolean accepted = room.placeBid(player, amount);
        if (!accepted) {
            return "ERROR:Bid rejected";
        }

        if (previousHighestBidderId != null && !previousHighestBidderId.equals(player.getUserId())) {
            User previousHighestBidder = userManager.getUser(previousHighestBidderId);
            if (previousHighestBidder instanceof Player previousPlayer) {
                userDAO.update(previousPlayer.getPlayerBalance(), previousPlayer.getUserId());
            }
        }

        BidTransaction latestTransaction = room.getLatestBidTransaction();
        if (latestTransaction != null) {
            bidTransactionDAO.save(room, player, latestTransaction);
        } else {
            auctionDAO.update(room);
            userDAO.update(player.getPlayerBalance(), player.getUserId());
        }
        return "OK:Bid accepted";
    }

    private static String handleEndAuction(String[] parts) {
        requireLength(parts, 3, "END_AUCTION:sessionId:auctionId");
        User user = requireUser(parts[1]);
        AuctionRoom room = requireAuction(parts[2]);
        if (!user.getUserId().equals(room.getSellerId())) {
            return "ERROR:Only seller can end this auction";
        }
        room.endAuction();
        if (room.getStatus() == AUCTIONCODE.Model.OtherInterface.AuctionStatus.SUCCESSFUL) {
            creditSeller(room);
        }
        auctionDAO.update(room);
        return "OK:Auction ended";
    }

    private static String handleAdminGetUsers(String[] parts) {
        requireLength(parts, 2, "ADMIN_GET_USERS:sessionId");
        requireAdmin(parts[1]);
        String payload = userManager.getAllUsers().stream()
                .map(user -> user.getUserId() + "|"
                        + safe(user.getUserName()) + "|"
                        + safe(user.getUserInformation() == null ? "" : user.getUserInformation().getFullName()) + "|"
                        + safe(user.getUserRole()) + "|"
                        + (user instanceof Player player ? player.getPlayerBalance() : 0.0) + "|"
                        + safe(user.getUserInformation() == null ? "" : user.getUserInformation().getEmail()) + "|"
                        + safe(user.getUserInformation() == null ? "" : user.getUserInformation().getPhoneNumber()) + "|"
                        + safe(user.getUserInformation() == null ? "" : user.getUserInformation().getAddress()))
                .collect(Collectors.joining(";"));
        return "OK:" + payload;
    }

    private static String handleAdminEndAuction(String[] parts) {
        requireLength(parts, 3, "ADMIN_END_AUCTION:sessionId:auctionId");
        requireAdmin(parts[1]);
        AuctionRoom room = requireAuction(parts[2]);
        if (room.getStatus() == AUCTIONCODE.Model.OtherInterface.AuctionStatus.ONGOING
                || room.getStatus() == AUCTIONCODE.Model.OtherInterface.AuctionStatus.EXTENDED) {
            room.endAuction();
            if (room.getStatus() == AUCTIONCODE.Model.OtherInterface.AuctionStatus.SUCCESSFUL) {
                creditSeller(room);
            }
        } else {
            Player refundedPlayer = room.refundHighestBidder();
            if (refundedPlayer != null) {
                userDAO.update(refundedPlayer.getPlayerBalance(), refundedPlayer.getUserId());
            }
            room.restoreStatus(AUCTIONCODE.Model.OtherInterface.AuctionStatus.CANCELED);
        }
        auctionDAO.update(room);
        return "OK:Auction ended";
    }

    private static String handleAdminExtendAuction(String[] parts) {
        requireLength(parts, 4, "ADMIN_EXTEND_AUCTION:sessionId:auctionId:minutes");
        requireAdmin(parts[1]);
        AuctionRoom room = requireAuction(parts[2]);
        long minutes = parsePositiveLong(parts[3], "minutes");
        LocalDateTime base = LocalDateTime.now().isAfter(room.getEndTime()) ? LocalDateTime.now() : room.getEndTime();
        LocalDateTime newEndTime = base.plusMinutes(minutes);
        room.setEndTime(newEndTime);
        room.restoreStatus(AUCTIONCODE.Model.OtherInterface.AuctionStatus.ONGOING);
        auctionDAO.updateEndTimeAndStatus(room.getId(), newEndTime, room.getStatus());
        return "OK:" + newEndTime;
    }

    private static String handleAdminDeleteAuction(String[] parts) {
        requireLength(parts, 3, "ADMIN_DELETE_AUCTION:sessionId:auctionId");
        requireAdmin(parts[1]);
        AuctionRoom room = requireAuction(parts[2]);
        if (room.getStatus() != AUCTIONCODE.Model.OtherInterface.AuctionStatus.SUCCESSFUL) {
            Player refundedPlayer = room.refundHighestBidder();
            if (refundedPlayer != null) {
                userDAO.update(refundedPlayer.getPlayerBalance(), refundedPlayer.getUserId());
            }
        }
        boolean deleted = auctionDAO.deleteCascade(room.getId());
        if (!deleted) {
            return "ERROR:Auction not found";
        }
        auctionManager.removeAuction(room.getId());
        return "OK:Auction deleted";
    }

    private static String handleAdminDeleteUser(String[] parts) {
        requireLength(parts, 3, "ADMIN_DELETE_USER:sessionId:userId");
        User admin = requireAdmin(parts[1]);
        if (admin.getUserId().equals(parts[2])) {
            return "ERROR:Admin cannot delete the current account";
        }
        User target = userManager.getUser(parts[2]);
        if (target == null) {
            return "ERROR:User not found";
        }
        if ("admin".equalsIgnoreCase(target.getUserName()) || "test".equalsIgnoreCase(target.getUserName())) {
            return "ERROR:Default accounts cannot be deleted";
        }
        boolean deleted = userDAO.delete(target.getUserId());
        if (!deleted) {
            return "ERROR:User not found";
        }
        userManager.removeUser(target.getUserId());
        AuthService.removeUser(target.getUserName());
        return "OK:User deleted";
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

    private static Player requirePlayer(String sessionId) {
        User user = requireUser(sessionId);
        if (!(user instanceof Player player)) {
            throw new IllegalArgumentException("Only player accounts can do this action");
        }
        return player;
    }

    private static User requireAdmin(String sessionId) {
        User user = requireUser(sessionId);
        if (!"Admin".equalsIgnoreCase(user.getUserRole())) {
            throw new IllegalArgumentException("Only admin accounts can do this action");
        }
        return user;
    }

    private static AuctionRoom requireAuction(String auctionId) {
        AuctionRoom room = auctionManager.getAuction(auctionId);
        if (room == null) {
            throw new IllegalArgumentException("Auction not found");
        }
        return room;
    }

    private static void syncAuctionStatusByClock(AuctionRoom room) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime openTime = room.getOpenTime();
        AUCTIONCODE.Model.OtherInterface.AuctionStatus currentStatus = room.getStatus();
        AUCTIONCODE.Model.OtherInterface.AuctionStatus newStatus = currentStatus;

        if (now.isBefore(openTime)) {
            if (currentStatus == AUCTIONCODE.Model.OtherInterface.AuctionStatus.ONGOING
                    && room.getBidHistory().isEmpty()) {
                newStatus = AUCTIONCODE.Model.OtherInterface.AuctionStatus.UPCOMING;
            }
        } else if (now.isBefore(room.getEndTime())) {
            if (currentStatus == AUCTIONCODE.Model.OtherInterface.AuctionStatus.UPCOMING) {
                newStatus = AUCTIONCODE.Model.OtherInterface.AuctionStatus.ONGOING;
            }
        } else if (currentStatus == AUCTIONCODE.Model.OtherInterface.AuctionStatus.UPCOMING
                || currentStatus == AUCTIONCODE.Model.OtherInterface.AuctionStatus.ONGOING
                || currentStatus == AUCTIONCODE.Model.OtherInterface.AuctionStatus.EXTENDED) {
            newStatus = room.getBidHistory().isEmpty()
                    ? AUCTIONCODE.Model.OtherInterface.AuctionStatus.NO_BIDS
                    : AUCTIONCODE.Model.OtherInterface.AuctionStatus.SUCCESSFUL;
        }

        if (newStatus != currentStatus) {
            room.restoreStatus(newStatus);
            if (newStatus == AUCTIONCODE.Model.OtherInterface.AuctionStatus.SUCCESSFUL) {
                creditSeller(room);
            }
            auctionDAO.updateStatus(room.getId(), newStatus);
        }
    }

    private static void creditSeller(AuctionRoom room) {
        User seller = userManager.getUser(room.getSellerId());
        if (seller instanceof Player sellerPlayer) {
            sellerPlayer.setPlayerBalance(sellerPlayer.getPlayerBalance() + room.getCurrentPrice());
            userDAO.update(sellerPlayer.getPlayerBalance(), sellerPlayer.getUserId());
        }
    }

    private static java.util.List<String> transactionRowsForUser(AuctionRoom room, User user) {
        java.util.List<String> rows = new java.util.ArrayList<>();
        AUCTIONCODE.Model.OtherInterface.AuctionStatus status = room.getStatus();
        if (status != AUCTIONCODE.Model.OtherInterface.AuctionStatus.SUCCESSFUL
                && status != AUCTIONCODE.Model.OtherInterface.AuctionStatus.NO_BIDS
                && status != AUCTIONCODE.Model.OtherInterface.AuctionStatus.CANCELED) {
            return rows;
        }

        BidTransaction latestBid = room.getLatestBidTransaction();
        String itemName = safe(room.getItem().getName());
        String finishedAt = room.getEndTime().toString();
        if (status == AUCTIONCODE.Model.OtherInterface.AuctionStatus.SUCCESSFUL && latestBid != null) {
            if (user.getUserId().equals(latestBid.getBidderId())) {
                rows.add(transactionPayload(finishedAt, "WON", room.getId(), itemName,
                        "Thắng đấu giá", -latestBid.getAmount(), displayUserName(room.getSellerId())));
            }
            if (user.getUserId().equals(room.getSellerId())) {
                rows.add(transactionPayload(finishedAt, "SOLD", room.getId(), itemName,
                        "Bán thành công", latestBid.getAmount(), displayUserName(latestBid.getBidderId())));
            }
            return rows;
        }

        if (user.getUserId().equals(room.getSellerId())) {
            rows.add(transactionPayload(finishedAt, status.toString(), room.getId(), itemName,
                    status == AUCTIONCODE.Model.OtherInterface.AuctionStatus.NO_BIDS
                            ? "Không có người đặt giá"
                            : "Phiên không thành công",
                    0.0, ""));
        }

        if (status == AUCTIONCODE.Model.OtherInterface.AuctionStatus.CANCELED
                && latestBid != null
                && user.getUserId().equals(latestBid.getBidderId())) {
            rows.add(transactionPayload(finishedAt, "REFUNDED", room.getId(), itemName,
                    "Hoàn tiền phiên không thành công", latestBid.getAmount(), displayUserName(room.getSellerId())));
        }
        return rows;
    }

    private static String transactionPayload(String time, String type, String auctionId, String itemName,
                                             String note, double amount, String counterparty) {
        return safe(time) + "|" + safe(type) + "|" + safe(auctionId) + "|" + itemName + "|"
                + safe(note) + "|" + amount + "|" + safe(counterparty);
    }

    private static String displayUserName(String userId) {
        if (userId == null || userId.isBlank()) {
            return "";
        }
        User user = userManager.getUser(userId);
        if (user == null) {
            return userId;
        }
        String fullName = user.getUserInformation() == null ? "" : user.getUserInformation().getFullName();
        return fullName == null || fullName.isBlank() ? user.getUserName() : fullName;
    }

    private static String sellerUserName(String userId) {
        User user = userManager.getUser(userId);
        return user == null ? "" : user.getUserName();
    }

    private static String sellerDisplayName(String userId) {
        User user = userManager.getUser(userId);
        if (user == null || user.getUserInformation() == null) {
            return "";
        }
        return user.getUserInformation().getFullName();
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", " ")
                .replace(";", " ")
                .replace(",", " ")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }

    private static Item createItemFromParts(String itemId, String type, String name, String description,
                                            String extra1, String extra2, String extra3, String imagePath) {
        int currentYear = LocalDateTime.now().getYear();
        return switch (type) {
            case "Art" -> ItemFactory.createArt(
                    itemId,
                    name,
                    description,
                    imagePath,
                    extra1 == null || extra1.isBlank() ? "Unknown" : extra1,
                    parseOptionalInt(extra2, currentYear)
            );
            case "Electronics" -> ItemFactory.createElectronics(
                    itemId,
                    name,
                    description,
                    imagePath,
                    extra1 == null || extra1.isBlank() ? "Unknown" : extra1,
                    parseOptionalInt(extra2, currentYear),
                    parseOptionalInt(extra3, 0)
            );
            case "Vehicle" -> ItemFactory.createVehicle(
                    itemId,
                    name,
                    description,
                    imagePath,
                    parseOptionalDouble(extra3, 0),
                    extra1 == null || extra1.isBlank() ? "Unknown" : extra1,
                    parseOptionalInt(extra2, currentYear)
            );
            default -> throw new IllegalArgumentException("Invalid item type");
        };
    }

    private static String itemPayload(Item item) {
        String type = itemDAO.getType(item);
        String extra1 = "";
        String extra2 = "";
        String extra3 = "";
        if (item instanceof AUCTIONCODE.Model.Item.Art art) {
            extra1 = art.getArtist();
            extra2 = String.valueOf(art.getYearCreated());
        } else if (item instanceof AUCTIONCODE.Model.Item.Electronics electronics) {
            extra1 = electronics.getBrand();
            extra2 = String.valueOf(electronics.getYearMade());
            extra3 = String.valueOf(electronics.getWarranty());
        } else if (item instanceof AUCTIONCODE.Model.Item.Vehicle vehicle) {
            extra1 = vehicle.getModel();
            extra2 = String.valueOf(vehicle.getYearMade());
            extra3 = String.valueOf(vehicle.getMileage());
        }
        return item.getId() + "|" + type + "|" + safe(item.getName()) + "|"
                + safe(item.getDescription()) + "|" + safe(extra1) + "|"
                + safe(extra2) + "|" + safe(extra3) + "|" + safe(item.getImagePath());
    }

    private static String itemAttributes(Item item) {
        if (item instanceof AUCTIONCODE.Model.Item.Art art) {
            return "Tác giả: " + art.getArtist() + " | Năm sáng tác: " + art.getYearCreated();
        }
        if (item instanceof AUCTIONCODE.Model.Item.Electronics electronics) {
            return "Thương hiệu: " + electronics.getBrand() + " | Năm sản xuất: "
                    + electronics.getYearMade() + " | Bảo hành: " + electronics.getWarranty() + " tháng";
        }
        if (item instanceof AUCTIONCODE.Model.Item.Vehicle vehicle) {
            return "Dòng xe: " + vehicle.getModel() + " | Năm sản xuất: "
                    + vehicle.getYearMade() + " | Quãng đường: " + vehicle.getMileage() + " km";
        }
        return "";
    }

    private static int parseOptionalInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value.trim());
    }

    private static double parseOptionalDouble(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Double.parseDouble(value.trim());
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

    private static long parseNonNegativeLong(String value, String fieldName) {
        long parsed = Long.parseLong(value);
        if (parsed < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return parsed;
    }
}
