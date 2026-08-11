package AUCTIONCODE.Network;

import AUCTIONCODE.Model.Auction.AuctionRoom;
import AUCTIONCODE.Model.Auction.BidTransaction;
import AUCTIONCODE.Model.OtherInterface.AuctionStatus;
import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class AuctionStatusService {
    private AuctionStatusService() {
    }

    static void syncAuctionStatusByClock(RequestContext context, AuctionRoom room) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime openTime = room.getOpenTime();
        AuctionStatus currentStatus = room.getStatus();
        AuctionStatus newStatus = currentStatus;

        if (now.isBefore(openTime)) {
            if (currentStatus == AuctionStatus.ONGOING && room.getBidHistory().isEmpty()) {
                newStatus = AuctionStatus.UPCOMING;
            }
        } else if (now.isBefore(room.getEndTime())) {
            if (currentStatus == AuctionStatus.UPCOMING) {
                newStatus = AuctionStatus.ONGOING;
            }
        } else if (currentStatus == AuctionStatus.UPCOMING
                || currentStatus == AuctionStatus.ONGOING
                || currentStatus == AuctionStatus.EXTENDED) {
            newStatus = room.getBidHistory().isEmpty() ? AuctionStatus.NO_BIDS : AuctionStatus.SUCCESSFUL;
        }

        if (newStatus != currentStatus) {
            room.restoreStatus(newStatus);
            if (newStatus == AuctionStatus.SUCCESSFUL) {
                creditSeller(context, room);
            }
            context.auctionDAO.updateStatus(room.getId(), newStatus);
        }
    }

    static void creditSeller(RequestContext context, AuctionRoom room) {
        User seller = context.userManager.getUser(room.getSellerId());
        if (seller instanceof Player sellerPlayer) {
            sellerPlayer.setPlayerBalance(sellerPlayer.getPlayerBalance() + room.getCurrentPrice());
            context.userDAO.update(sellerPlayer.getPlayerBalance(), sellerPlayer.getUserId());
        }
    }

    static List<String> transactionRowsForUser(RequestContext context, AuctionRoom room, User user) {
        List<String> rows = new ArrayList<>();
        AuctionStatus status = room.getStatus();
        if (status != AuctionStatus.SUCCESSFUL
                && status != AuctionStatus.NO_BIDS
                && status != AuctionStatus.CANCELED) {
            return rows;
        }

        BidTransaction latestBid = room.getLatestBidTransaction();
        String itemName = RequestSupport.safe(room.getItem().getName());
        String finishedAt = room.getEndTime().toString();
        if (status == AuctionStatus.SUCCESSFUL && latestBid != null) {
            if (user.getUserId().equals(latestBid.getBidderId())) {
                rows.add(RequestSupport.transactionPayload(finishedAt, "WON", room.getId(), itemName,
                        "Thắng đấu giá", -latestBid.getAmount(),
                        RequestSupport.displayUserName(context, room.getSellerId())));
            }
            if (user.getUserId().equals(room.getSellerId())) {
                rows.add(RequestSupport.transactionPayload(finishedAt, "SOLD", room.getId(), itemName,
                        "Bán thành công", latestBid.getAmount(),
                        RequestSupport.displayUserName(context, latestBid.getBidderId())));
            }
            return rows;
        }

        if (user.getUserId().equals(room.getSellerId())) {
            rows.add(RequestSupport.transactionPayload(finishedAt, status.toString(), room.getId(), itemName,
                    status == AuctionStatus.NO_BIDS
                            ? "Không có người đặt giá"
                            : "Phiên không thành công",
                    0.0, ""));
        }

        if (status == AuctionStatus.CANCELED
                && latestBid != null
                && user.getUserId().equals(latestBid.getBidderId())) {
            rows.add(RequestSupport.transactionPayload(finishedAt, "REFUNDED", room.getId(), itemName,
                    "Hoàn tiền phiên không thành công", latestBid.getAmount(),
                    RequestSupport.displayUserName(context, room.getSellerId())));
        }
        return rows;
    }
}
