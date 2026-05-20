package Manager;

import Model.User.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private static UserManager instance;
    ///  String la userId
    private final Map<String, User> userMap = new ConcurrentHashMap<>();
    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager();
                }
            }
        }
        return instance;

    }
    public void addUser(User user, String userId){
        userMap.put(userId,user);
    }


    public User getUser(String userId) {
        return userMap.get(userId);

    }

    public List<User> getAllUsers() {
        return new ArrayList<>(userMap.values());
    }
}
