package AUCTIONCODE.Model.Auction;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class BidTransactionTest {

    @Test
    void testConstructorAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        BidTransaction transaction = new BidTransaction("TX1001", "ROOM01", "USER02", 5000.0, now);

        assertEquals("TX1001", transaction.getTransactionId());
        assertEquals("ROOM01", transaction.getAuctionId());
        assertEquals("USER02", transaction.getBidderId());
        assertEquals(5000.0, transaction.getAmount());
        assertEquals(now, transaction.getTimestamp());
    }
}