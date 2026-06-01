package AUCTIONCODE.AuthModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InMemorySessionManagerTest {

    private InMemorySessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = InMemorySessionManager.getInstance();
        // Dọn dẹp sạch các session cũ trước mỗi bài test để không bị ảnh hưởng chéo
        sessionManager.removeExpiredSessions();

        // Vì sessionStore là private static và không có hàm clear() trực tiếp công khai công cộng công khai,
        // chúng ta sẽ invalidate các session cụ thể nếu cần, hoặc tận dụng logic tự nhiên của bài test.
    }

    @Test
    void testSingletonInstance() {
        InMemorySessionManager instance1 = InMemorySessionManager.getInstance();
        InMemorySessionManager instance2 = InMemorySessionManager.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    void testCreateAndGetSession_ValidScenario() {
        String userId = "USER_ABC";
        String sessionId = sessionManager.createSession(userId);

        assertNotNull(sessionId);
        assertTrue(sessionManager.isValidSession(sessionId));
        assertEquals(userId, sessionManager.getUserId(sessionId));

        Session session = sessionManager.getSession(sessionId);
        assertNotNull(session);
        assertEquals(userId, session.getUserId());
    }

    @Test
    void testIsValidSession_WithNonExistentId_ShouldReturnFalse() {
        assertFalse(sessionManager.isValidSession("INVALID_SESSION_ID"));
        assertNull(sessionManager.getUserId("INVALID_SESSION_ID"));
    }

    @Test
    void testInvalidateSession_ShouldRemoveSessionImmediately() {
        String sessionId = sessionManager.createSession("USER_ID");
        assertTrue(sessionManager.isValidSession(sessionId));

        sessionManager.invalidateSession(sessionId);

        assertFalse(sessionManager.isValidSession(sessionId));
        assertNull(sessionManager.getSession(sessionId));
    }

    @Test
    void testRemoveExpiredSessions_AndActiveCount() {
        String liveUserSession = sessionManager.createSession("LIVE_USER");
        String expiredUserSession = sessionManager.createSession("EXPIRED_USER");

        // Giả lập ép thời gian hết hạn của expiredUserSession về quá khứ
        Session expiredSessionObj = sessionManager.getSession(expiredUserSession);
        expiredSessionObj.setExpireTime(System.currentTimeMillis() - 1000); // Đã hết hạn trước 1 giây

        // Kiểm tra tính hợp lệ
        assertTrue(sessionManager.isValidSession(liveUserSession));
        assertFalse(sessionManager.isValidSession(expiredUserSession));

        // Kích hoạt dọn dẹp các session hết hạn
        sessionManager.removeExpiredSessions();

        // Session hết hạn phải bị xóa hoàn toàn khỏi store
        assertNull(sessionManager.getSession(expiredUserSession));
        assertNotNull(sessionManager.getSession(liveUserSession));
    }
}