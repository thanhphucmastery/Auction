package AUCTIONCODE.Network;

import AUCTIONCODE.AuthModule.AuthService;
import AUCTIONCODE.Database.DatabaseBootstrap;
import AUCTIONCODE.Model.User.User;
import AUCTIONCODE.Model.User.UserInformation;

final class AuthRequestHandler {
    private final RequestContext context;

    AuthRequestHandler(RequestContext context) {
        this.context = context;
    }

    String handleRegister(String[] parts) {
        RequestSupport.requireLength(parts, 7, "REGISTER:username:password:fullName:phone:email:address");
        UserInformation information = new UserInformation(parts[6], parts[4], parts[5], parts[3]);
        boolean registered = AuthService.registerPlayer(parts[1], information, parts[2]);
        if (registered) {
            context.userDAO.save(AuthService.findUserByUserName(parts[1]));
        }
        return registered ? "OK:Registered" : "ERROR:Username already exists";
    }

    String handleRegisterAdmin(String[] parts) {
        RequestSupport.requireLength(parts, 8, "REGISTER_ADMIN:username:password:fullName:phone:email:address:businessCode");
        UserInformation information = new UserInformation(parts[6], parts[4], parts[5], parts[3]);
        boolean registered = AuthService.registerAdmin(parts[1], information, parts[2], parts[7]);
        if (registered) {
            context.userDAO.save(AuthService.findUserByUserName(parts[1]));
        }
        return registered ? "OK:Registered" : "ERROR:Invalid business code or username already exists";
    }

    String handleLogin(String[] parts) {
        RequestSupport.requireLength(parts, 3, "LOGIN:username:password");
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

    String handleResetPassword(String[] parts) {
        RequestSupport.requireLength(parts, 4, "RESET_PASSWORD:username:email:newPassword");
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
        context.userDAO.updatePassword(user.getUserId(), user.getUserPassword());
        return "OK:Password reset";
    }

    String handleLogout(String[] parts) {
        RequestSupport.requireLength(parts, 2, "LOGOUT:sessionId");
        return AuthService.logout(parts[1]) ? "OK:Logged out" : "ERROR:Invalid session";
    }
}
