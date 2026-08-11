package AUCTIONCODE.Network;

import AUCTIONCODE.Database.DatabaseBootstrap;
import AUCTIONCODE.Model.Auction.AuctionRoom;
import AUCTIONCODE.Model.Auction.BidTransaction;
import AUCTIONCODE.Model.Item.Item;
import AUCTIONCODE.Model.OtherInterface.AuctionStatus;
import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.User;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.UUID;

final class AuctionRequestHandler {
    private final RequestContext context;

    AuctionRequestHandler(RequestContext context) {
        this.context = context;
    }

    String handleCreateAuctionFromItem(String[] parts) {
        RequestSupport.requireLength(parts, 7, "CREATE_AUCTION_FROM_ITEM:sessionId:itemId:startPrice:stepPrice:startDelayMinutes:durationMinutes");
        User seller = RequestSupport.requireUser(context, parts[1]);
        Item item = context.itemDAO.findAvailableByOwner(parts[2], seller.getUserId());
        if (item == null) {
            return "ERROR:Item not found or already in auction";
        }

        double startPrice = RequestSupport.parsePositiveDouble(parts[3], "startPrice");
        double stepPrice = RequestSupport.parsePositiveDouble(parts[4], "stepPrice");
        long startDelayMinutes = RequestSupport.parseNonNegativeLong(parts[5], "startDelayMinutes");
        long durationMinutes = RequestSupport.parsePositiveLong(parts[6], "durationMinutes");
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
        context.auctionManager.addAuction(room);
        context.auctionDAO.saveWithExistingItem(room, item);
        context.itemDAO.markInAuction(item.getId(), seller.getUserId());
        return "OK:" + auctionId;
    }

    String handleJoinAuction(String[] parts) {
        RequestSupport.requireLength(parts, 3, "JOIN_AUCTION:sessionId:auctionId");
        User user = RequestSupport.requireUser(context, parts[1]);
        AuctionRoom room = RequestSupport.requireAuction(context, parts[2]);
        AuctionStatusService.syncAuctionStatusByClock(context, room);
        room.joinAuctionRoom(user);
        return "OK:Joined";
    }

    String handleBid(String[] parts) {
        RequestSupport.requireLength(parts, 4, "BID:sessionId:auctionId:amount");
        Player player = RequestSupport.requirePlayer(context, parts[1]);
        AuctionRoom room = RequestSupport.requireAuction(context, parts[2]);
        AuctionStatusService.syncAuctionStatusByClock(context, room);
        double amount = RequestSupport.parsePositiveDouble(parts[3], "amount");
        String previousHighestBidderId = room.getHighestBidderId();
        boolean accepted = room.placeBid(player, amount);
        if (!accepted) {
            return "ERROR:Bid rejected";
        }

        if (previousHighestBidderId != null && !previousHighestBidderId.equals(player.getUserId())) {
            User previousHighestBidder = context.userManager.getUser(previousHighestBidderId);
            if (previousHighestBidder instanceof Player previousPlayer) {
                context.userDAO.update(previousPlayer.getPlayerBalance(), previousPlayer.getUserId());
            }
        }

        BidTransaction latestTransaction = room.getLatestBidTransaction();
        if (latestTransaction != null) {
            context.bidTransactionDAO.save(room, player, latestTransaction);
        } else {
            context.auctionDAO.update(room);
            context.userDAO.update(player.getPlayerBalance(), player.getUserId());
        }
        return "OK:Bid accepted";
    }

    String handleEndAuction(String[] parts) {
        RequestSupport.requireLength(parts, 3, "END_AUCTION:sessionId:auctionId");
        User user = RequestSupport.requireUser(context, parts[1]);
        AuctionRoom room = RequestSupport.requireAuction(context, parts[2]);
        if (!user.getUserId().equals(room.getSellerId())) {
            return "ERROR:Only seller can end this auction";
        }
        room.endAuction();
        if (room.getStatus() == AuctionStatus.SUCCESSFUL) {
            AuctionStatusService.creditSeller(context, room);
        }
        context.auctionDAO.update(room);
        return "OK:Auction ended";
    }

    String handleGetAuctions(String[] parts) {
        RequestSupport.requireLength(parts, 2, "GET_AUCTIONS:sessionId");
        RequestSupport.requireUser(context, parts[1]);
        DatabaseBootstrap.reloadFromDatabase();
        RequestSupport.requireUser(context, parts[1]);
        String payload = context.auctionManager.getAllAuctions().stream()
                .peek(room -> AuctionStatusService.syncAuctionStatusByClock(context, room))
                .map(room -> room.getId() + "|" + room.getStatus() + "|"
                        + room.getCurrentPrice() + "|" + room.getEndTime() + "|"
                        + room.getItem().getName() + "|"
                        + RequestSupport.safe(RequestSupport.sellerUserName(context, room.getSellerId())) + "|"
                        + RequestSupport.safe(RequestSupport.sellerDisplayName(context, room.getSellerId())))
                .collect(Collectors.joining(";"));
        return "OK:" + payload;
    }

    String handleGetAuctionDetail(String[] parts) {
        RequestSupport.requireLength(parts, 3, "GET_AUCTION_DETAIL:sessionId:auctionId");
        User viewer = RequestSupport.requireUser(context, parts[1]);
        AuctionRoom room = RequestSupport.requireAuction(context, parts[2]);
        AuctionStatusService.syncAuctionStatusByClock(context, room);
        Item item = room.getItem();
        String highestBidderId = room.getHighestBidderId();
        String highestBidderName = RequestSupport.displayUserName(context, highestBidderId);
        String sellerName = RequestSupport.sellerDisplayName(context, room.getSellerId());
        if (sellerName.isBlank()) {
            sellerName = RequestSupport.sellerUserName(context, room.getSellerId());
        }
        String history = room.getBidHistory().stream()
                .map(transaction -> transaction.getTimestamp() + ","
                        + RequestSupport.safe(transaction.getBidderId()) + ","
                        + RequestSupport.safe(RequestSupport.displayUserName(context, transaction.getBidderId())) + ","
                        + transaction.getAmount())
                .collect(Collectors.joining(";"));
        return "OK:" + room.getId() + "|"
                + RequestSupport.safe(room.getItem().getName()) + "|"
                + room.getCurrentPrice() + "|"
                + room.getStepPrice() + "|"
                + room.getStatus() + "|"
                + room.getEndTime() + "|"
                + RequestSupport.safe(highestBidderId) + "|"
                + RequestSupport.safe(highestBidderName) + "|"
                + history + "|"
                + room.getOpenTime() + "|"
                + RequestSupport.safe(item.getImagePath()) + "|"
                + RequestSupport.safe(sellerName) + "|"
                + RequestSupport.safe(context.itemDAO.getType(item)) + "|"
                + RequestSupport.safe(item.getDescription()) + "|"
                + RequestSupport.safe(RequestSupport.itemAttributes(item)) + "|"
                + viewer.getUserId().equals(highestBidderId);
    }

    String handleGetTransactionHistory(String[] parts) {
        RequestSupport.requireLength(parts, 2, "GET_TRANSACTION_HISTORY:sessionId");
        User user = RequestSupport.requireUser(context, parts[1]);
        DatabaseBootstrap.reloadFromDatabase();
        RequestSupport.requireUser(context, parts[1]);
        return "OK:" + context.auctionManager.getAllAuctions().stream()
                .peek(room -> AuctionStatusService.syncAuctionStatusByClock(context, room))
                .flatMap(room -> AuctionStatusService.transactionRowsForUser(context, room, user).stream())
                .collect(Collectors.joining(";"));
    }
}
