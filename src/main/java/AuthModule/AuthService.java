package AuthModule;

import Manager.UserManager;

import Model.OtherInterface.SessionManager;
import Model.User.Admin;
import Model.User.Player;
import Model.User.User;
import Model.User.UserInformation;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class AuthService {
    private final static Map<String, User> userDatabase = new ConcurrentHashMap<>();
    private final static SessionManager sessionManager = InMemorySessionManager.getInstance();
    private static final Set<String> VALID_BUSINESS_CODES = Set.of("BIZ-001", "BIZ-002");
    public static boolean registerPlayer(String userName, UserInformation infor, String rawpassword){
        ///  kiểm tra đăng nhập có bị trùng tên không??
        if (!userDatabase.containsKey(userName)){
            String hashpassword= PasswordHasher.hash(rawpassword);
            String newUserId = java.util.UUID.randomUUID().toString();
            User user = new Player(userName, infor, hashpassword,newUserId,0.0);
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
            User user = new Admin(userName, infor, hashpassword,newUserId,businesscode);
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


    ///  logout
    public static boolean logout(String sessionId){
        if(sessionManager.isValidSession(sessionId)){
            sessionManager.invalidateSession(sessionId);
            return true;
        } return  false;
    }
}











