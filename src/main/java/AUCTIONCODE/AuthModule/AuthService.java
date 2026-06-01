package AUCTIONCODE.AuthModule;

import AUCTIONCODE.Manager.UserManager;

import AUCTIONCODE.Model.OtherInterface.SessionManager;
import AUCTIONCODE.Model.User.Admin;
import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.User;
import AUCTIONCODE.Model.User.UserInformation;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class AuthService {
    private final static Map<String, User> userDatabase = new ConcurrentHashMap<>();
    private final static SessionManager sessionManager = InMemorySessionManager.getInstance();
    private static final Set<String> VALID_BUSINESS_CODES = Set.of("BIZ-001", "BIZ-002");
    private static volatile AuthService instance;
    public static boolean registerPlayer(String userName, UserInformation infor, String rawpassword){
        ///  kiểm tra đăng nhập có bị trùng tên không??
        if (!userDatabase.containsKey(userName)){
            String hashpassword= PasswordHasher.hash(rawpassword);
            String newUserId = java.util.UUID.randomUUID().toString();
            User user = new Player(userName, infor, hashpassword,newUserId,0.0,"Player");
            userDatabase.put(userName, user);
            UserManager.getInstance().addUser(user,newUserId);
            return true;
        } return false;
    } /// nếu bị trùng thì sẽ nhả ra false


    public static boolean registerAdmin(String userName, UserInformation infor, String rawpassword, String businesscode){
        ///  kiểm tra đăng nhập có bị trùng tên không??
        if (!VALID_BUSINESS_CODES.contains(businesscode)) return false;
        if (!userDatabase.containsKey(userName)){
            String hashpassword= PasswordHasher.hash(rawpassword);
            String newUserId = java.util.UUID.randomUUID().toString();
            User user = new Admin(userName, infor, hashpassword,newUserId,businesscode,"Admin");
            userDatabase.put(userName, user);
            UserManager.getInstance().addUser(user,newUserId);
            return true;
        } return false;
    } /// nếu bị trùng thì sẽ nhả ra false


    ///  login
    public static String login(String userName, String password) {
        User user = userDatabase.get(userName);
        if (user != null) {
            if (PasswordHasher.matches(password,user.getUserPassword())) {
                return InMemorySessionManager.getInstance().createSession(user.getUserId());
            }
        }
        return null;
    }

    public static void ensureDefaultPlayer() {
        if (userDatabase.containsKey("test")) {
            return;
        }
        UserInformation info = new UserInformation(
                "Ha Noi",
                "0123456789",
                "test@example.com",
                "Test User"
        );
        String userId = "user-test";
        User user = new Player("test", info, PasswordHasher.hash("1234"), userId, 0.0, "Player");
        userDatabase.put("test", user);
        UserManager.getInstance().addUser(user, userId);
    }

    public static void ensureDefaultAdmin() {
        if (userDatabase.containsKey("admin")) {
            return;
        }
        UserInformation info = new UserInformation(
                "",
                "0987654321",
                "admin@example.com",
                "Administrator"
        );
        String userId = "user-admin";
        User user = new Admin("admin", info, PasswordHasher.hash("1234"), userId, "BIZ-001", "Admin");
        userDatabase.put("admin", user);
        UserManager.getInstance().addUser(user, userId);
    }

    public static User findUserByUserName(String userName) {
        return userDatabase.get(userName);
    }

    public static boolean resetPassword(String userName, String email, String rawPassword) {
        User user = userDatabase.get(userName);
        if (user == null || user.getUserInformation() == null) {
            return false;
        }
        String storedEmail = user.getUserInformation().getEmail();
        if (storedEmail == null || !storedEmail.equalsIgnoreCase(email)) {
            return false;
        }
        user.setUserPassword(PasswordHasher.hash(rawPassword));
        return true;
    }

    public static void removeUser(String userName) {
        userDatabase.remove(userName);
    }


    ///  logout
    public static boolean logout(String sessionId){
        if(sessionManager.isValidSession(sessionId)){
            sessionManager.invalidateSession(sessionId);
            return true;
        } return  false;
    }
    public static AuthService getInstance(){
        AuthService res = instance;
        if (res == null){
            synchronized (AuthService.class){
                res= instance;
                if(res==null){
                    res = instance= new AuthService();
                }
            }
        } return instance;
    }
    public void loadUserToAuth(String userName, User user) {
        if (!userDatabase.containsKey(userName)) {
            userDatabase.put(userName, user);
        }
    }

    public static void clearLoadedUsers() {
        userDatabase.clear();
    }
}











