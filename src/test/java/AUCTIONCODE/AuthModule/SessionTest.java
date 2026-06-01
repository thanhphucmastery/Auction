package AUCTIONCODE.AuthModule;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    @Test
    void testConstructorAndGetters() {
        long expireTime = System.currentTimeMillis() + 5000;
        Session session = new Session("SESSION_001", "USER_123", expireTime);

        assertEquals("USER_123", session.getUserId());
        assertEquals(expireTime, session.getExpireTime());
    }

    @Test
    void testSetExpireTime() {
        Session session = new Session("SESSION_001", "USER_123", 1000L);
        session.setExpireTime(2000L);

        assertEquals(2000L, session.getExpireTime());
    }

    @Test
    void testRefresh_ShouldExtendExpireTime() {
        Session session = new Session("SESSION_001", "USER_123", System.currentTimeMillis());
        long timeout = 60000L; // 1 phút

        long timeBeforeRefresh = System.currentTimeMillis();
        session.refresh(timeout);
        long timeAfterRefresh = System.currentTimeMillis();

        // Thời gian hết hạn mới phải nằm trong khoảng (thời gian trước refresh + timeout) và (thời gian sau refresh + timeout)
        assertTrue(session.getExpireTime() >= timeBeforeRefresh + timeout);
        assertTrue(session.getExpireTime() <= timeAfterRefresh + timeout);
    }
}