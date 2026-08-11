package AUCTIONCODE.Network;

import AUCTIONCODE.AuthModule.AuthService;
import AUCTIONCODE.Model.Auction.AuctionRoom;
import AUCTIONCODE.Model.OtherInterface.AuctionStatus;
import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.User;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

final class AdminRequestHandler {
    private final RequestContext context;

    AdminRequestHandler(RequestContext context) {
        this.context = context;
    }

    String handleAdminGetUsers(String[] parts) {
        RequestSupport.requireLength(parts, 2, "ADMIN_GET_USERS:sessionId");
        RequestSupport.requireAdmin(context, parts[1]);
        String payload = context.userManager.getAllUsers().stream()
                .map(user -> user.getUserId() + "|"
                        + RequestSupport.safe(user.getUserName()) + "|"
                        + RequestSupport.safe(user.getUserInformation() == null ? "" : user.getUserInformation().getFullName()) + "|"
                        + RequestSupport.safe(user.getUserRole()) + "|"
                        + (user instanceof Player player ? player.getPlayerBalance() : 0.0) + "|"
                        + RequestSupport.safe(user.getUserInformation() == null ? "" : user.getUserInformation().getEmail()) + "|"
                        + RequestSupport.safe(user.getUserInformation() == null ? "" : user.getUserInformation().getPhoneNumber()) + "|"
                        + RequestSupport.safe(user.getUserInformation() == null ? "" : user.getUserInformation().getAddress()))
                .collect(Collectors.joining(";"));
        return "OK:" + payload;
    }

    String handleAdminEndAuction(String[] parts) {
        RequestSupport.requireLength(parts, 3, "ADMIN_END_AUCTION:sessionId:auctionId");
        RequestSupport.requireAdmin(context, parts[1]);
        AuctionRoom room = RequestSupport.requireAuction(context, parts[2]);
        if (room.getStatus() == AuctionStatus.ONGOING || room.getStatus() == AuctionStatus.EXTENDED) {
            room.endAuction();
            if (room.getStatus() == AuctionStatus.SUCCESSFUL) {
                AuctionStatusService.creditSeller(context, room);
            }
        } else {
            Player refundedPlayer = room.refundHighestBidder();
            if (refundedPlayer != null) {
                context.userDAO.update(refundedPlayer.getPlayerBalance(), refundedPlayer.getUserId());
            }
            room.restoreStatus(AuctionStatus.CANCELED);
        }
        context.auctionDAO.update(room);
        return "OK:Auction ended";
    }

    String handleAdminExtendAuction(String[] parts) {
        RequestSupport.requireLength(parts, 4, "ADMIN_EXTEND_AUCTION:sessionId:auctionId:minutes");
        RequestSupport.requireAdmin(context, parts[1]);
        AuctionRoom room = RequestSupport.requireAuction(context, parts[2]);
        long minutes = RequestSupport.parsePositiveLong(parts[3], "minutes");
        LocalDateTime base = LocalDateTime.now().isAfter(room.getEndTime()) ? LocalDateTime.now() : room.getEndTime();
        LocalDateTime newEndTime = base.plusMinutes(minutes);
        room.setEndTime(newEndTime);
        room.restoreStatus(AuctionStatus.ONGOING);
        context.auctionDAO.updateEndTimeAndStatus(room.getId(), newEndTime, room.getStatus());
        return "OK:" + newEndTime;
    }

    String handleAdminDeleteAuction(String[] parts) {
        RequestSupport.requireLength(parts, 3, "ADMIN_DELETE_AUCTION:sessionId:auctionId");
        RequestSupport.requireAdmin(context, parts[1]);
        AuctionRoom room = RequestSupport.requireAuction(context, parts[2]);
        if (room.getStatus() != AuctionStatus.SUCCESSFUL) {
            Player refundedPlayer = room.refundHighestBidder();
            if (refundedPlayer != null) {
                context.userDAO.update(refundedPlayer.getPlayerBalance(), refundedPlayer.getUserId());
            }
        }
        boolean deleted = context.auctionDAO.deleteCascade(room.getId());
        if (!deleted) {
            return "ERROR:Auction not found";
        }
        context.auctionManager.removeAuction(room.getId());
        return "OK:Auction deleted";
    }

    String handleAdminDeleteUser(String[] parts) {
        RequestSupport.requireLength(parts, 3, "ADMIN_DELETE_USER:sessionId:userId");
        User admin = RequestSupport.requireAdmin(context, parts[1]);
        if (admin.getUserId().equals(parts[2])) {
            return "ERROR:Admin cannot delete the current account";
        }
        User target = context.userManager.getUser(parts[2]);
        if (target == null) {
            return "ERROR:User not found";
        }
        if ("admin".equalsIgnoreCase(target.getUserName()) || "test".equalsIgnoreCase(target.getUserName())) {
            return "ERROR:Default accounts cannot be deleted";
        }
        boolean deleted = context.userDAO.delete(target.getUserId());
        if (!deleted) {
            return "ERROR:User not found";
        }
        context.userManager.removeUser(target.getUserId());
        AuthService.removeUser(target.getUserName());
        return "OK:User deleted";
    }
}
