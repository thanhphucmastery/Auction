package AuthModule;

import Model.OtherInterface.SessionManager;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionManager implements SessionManager {
    // Lưu trữ các session với Key là sessionId, Value là đối tượng Session
    private static final Map<String, Session> sessionStore = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 18000000;
    private static InMemorySessionManager instance;
    private InMemorySessionManager(){};
    public static InMemorySessionManager getInstance(){
        if (instance == null) {
            synchronized (InMemorySessionManager.class) {
                if (instance == null) {
                    instance = new InMemorySessionManager();
                }
            }
        }
        return instance;
    }

    // Các hàm implement từ interface sẽ nằm ở đây...

    @Override
    public String createSession(String userId){
        long expireTime= System.currentTimeMillis()+SESSION_TIMEOUT;
        String sessionId = UUID.randomUUID().toString();
        Session newSession = new Session(sessionId, userId, expireTime);
        sessionStore.put(sessionId, newSession);
        return sessionId;
    }

    @Override
    public String getUserId(String sessionId){
        if (isValidSession(sessionId)){
            return sessionStore.get(sessionId).getUserId();
        } return null;
    }

    @Override
    public void refreshSession(String sessionId){
        Session session= sessionStore.get(sessionId);

        if (session!=null){
            session.setExpireTime(System.currentTimeMillis()+SESSION_TIMEOUT);
        }
    }

    @Override
    public boolean isExpired(Session session){
        if (session.getExpireTime()>System.currentTimeMillis()){
            return false;
        } return true;
    }

    @Override
    public boolean isValidSession(String sessionId){
        if (sessionStore.get(sessionId)==null){
            return false;
        } return !isExpired(sessionStore.get(sessionId));
    }

    @Override
    public void removeExpiredSessions() {
        var iterator = sessionStore.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            Session session = entry.getValue();

            if (isExpired(session)) {
                iterator.remove();
            }
        }
    }


    @Override
    public void invalidateSession(String sessionId){
        sessionStore.remove(sessionId);
    }


    @Override
    public  int getActiveSessionCount() {
        var iterator = sessionStore.entrySet().iterator();
        int count = 0;
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Session session = entry.getValue();

            if (!isExpired(session)) {
                count += 1;
            }
        }
        return count;
    }


    @Override
    public Session getSession(String sessionId){
        return sessionStore.get(sessionId);
    }

    @Override
    public Collection<Session> getAllSessions(){
        return sessionStore.values();
    }


}
