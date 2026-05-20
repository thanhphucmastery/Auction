package Model.OtherInterface;

import AuthModule.Session;

import java.util.Collection;

public interface SessionManager {
    String createSession(String userId);
    String getUserId(String sessionId);
    void invalidateSession(String sessionId);
    boolean isValidSession(String sessionId);
    boolean isExpired(Session session);
    void refreshSession(String sessionId);
    Session getSession(String sessionId);
    void removeExpiredSessions();
    int getActiveSessionCount();
    Collection<Session> getAllSessions();

}
