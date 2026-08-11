package AUCTIONCODE.Network;

import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.User;
import AUCTIONCODE.Model.User.UserInformation;

final class ProfileRequestHandler {
    private final RequestContext context;

    ProfileRequestHandler(RequestContext context) {
        this.context = context;
    }

    String handleDeposit(String[] parts) {
        RequestSupport.requireLength(parts, 3, "DEPOSIT:sessionId:amount");
        Player player = RequestSupport.requirePlayer(context, parts[1]);
        double amount = RequestSupport.parsePositiveDouble(parts[2], "amount");
        player.setPlayerBalance(player.getPlayerBalance() + amount);
        context.userDAO.update(player.getPlayerBalance(), player.getUserId());
        return "OK:" + player.getPlayerBalance();
    }

    String handleWithdraw(String[] parts) {
        RequestSupport.requireLength(parts, 3, "WITHDRAW:sessionId:amount");
        Player player = RequestSupport.requirePlayer(context, parts[1]);
        double amount = RequestSupport.parsePositiveDouble(parts[2], "amount");

        if (player.getPlayerBalance() < amount) {
            return "ERROR:Insufficient balance";
        }

        player.setPlayerBalance(player.getPlayerBalance() - amount);
        context.userDAO.update(player.getPlayerBalance(), player.getUserId());
        return "OK:" + player.getPlayerBalance();
    }

    String handleGetProfile(String[] parts) {
        RequestSupport.requireLength(parts, 2, "GET_PROFILE:sessionId");
        User user = RequestSupport.requireUser(context, parts[1]);
        UserInformation info = user.getUserInformation();
        double balance = user instanceof Player player ? player.getPlayerBalance() : 0.0;
        return "OK:" + user.getUserName() + "|" + info.getFullName() + "|"
                + info.getPhoneNumber() + "|" + info.getEmail() + "|"
                + info.getAddress() + "|" + user.getUserRole() + "|" + balance;
    }

    String handleUpdateProfile(String[] parts) {
        RequestSupport.requireLength(parts, 6, "UPDATE_PROFILE:sessionId:fullName:phone:email:address");
        User user = RequestSupport.requireUser(context, parts[1]);
        user.setUserInformation(new UserInformation(parts[5], parts[3], parts[4], parts[2]));
        context.userDAO.updateInformation(user);
        return "OK:Profile updated";
    }
}
