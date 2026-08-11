package AUCTIONCODE.Model.Auction;

import AUCTIONCODE.Model.Item.Item;
import AUCTIONCODE.Model.Item.ItemFactory;
import AUCTIONCODE.Model.OtherInterface.AuctionStatus;
import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.UserInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuctionRoomTest {

    private UserInformation generalInfo;
    private Player seller;
    private Player bidder1;
    private Player bidder2;
    private Item artItem;
    private Item vehicleItem;

    @BeforeEach
    void setUp() {
        // Khởi tạo thông tin người dùng hợp lệ (đủ 10 chữ số điện thoại)
        generalInfo = new UserInformation("Saigon", "0909123456", "user@test.com", "Nguyen Chinh");

        seller = new Player("seller_user", generalInfo, "hash1", "SELLER_ID", 5000.0, "PLAYER");
        bidder1 = new Player("bidder_user1", generalInfo, "hash2", "BIDDER_1", 10000.0, "PLAYER");
        bidder2 = new Player("bidder_user2", generalInfo, "hash3", "BIDDER_2", 15000.0, "PLAYER");

        // Sử dụng Item thật từ ItemFactory gửi kèm
        artItem = ItemFactory.createArt("A01", "Mona Lisa", "Bức tranh nổi tiếng", "Da Vinci", 1503);
        vehicleItem = ItemFactory.createVehicle("V01", "Tesla Model 3", "Xe điện", 15000.0, "Model 3", 2022);
    }

    @Test
    void testConstructor_ShouldInitializeProperly() {
        LocalDateTime open = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        AuctionRoom room = new AuctionRoom("ROOM_01", "SELLER_ID", 2000.0, 200.0, open, end, artItem, null);

        assertEquals("ROOM_01", room.getId());
        assertEquals("SELLER_ID", room.getSellerId());
        assertEquals(2000.0, room.getCurrentPrice());
        assertEquals(200.0, room.getStepPrice());
        assertEquals(AuctionStatus.UPCOMING, room.getStatus());
        assertEquals(artItem, room.getItem());
        assertNull(room.getHighestBidderId());
    }

    @Test
    void testStartAuctionRoom_ValidTime_ShouldSwitchToOngoing() {
        LocalDateTime open = LocalDateTime.now().minusSeconds(10); // Đã đến giờ mở
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        AuctionRoom room = new AuctionRoom("ROOM_01", "SELLER_ID", 2000.0, 200.0, open, end, artItem, null);

        room.startAuctionRoom();
        assertEquals(AuctionStatus.ONGOING, room.getStatus());
    }

    @Test
    void testEndAuction_WithNoBids_ShouldSetStatusToNoBids() {
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        AuctionRoom room = new AuctionRoom("ROOM_01", "SELLER_ID", 2000.0, 200.0, end, artItem, null);
        room.restoreStatus(AuctionStatus.ONGOING);

        room.endAuction();
        assertEquals(AuctionStatus.NO_BIDS, room.getStatus());
    }

    @Test
    void testEndAuction_WithExistingBids_ShouldSetStatusToSuccessful() {
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        AuctionRoom room = new AuctionRoom("ROOM_01", "SELLER_ID", 2000.0, 200.0, end, artItem, null);
        room.restoreStatus(AuctionStatus.ONGOING);

        // Giả lập đưa một giao dịch đặt giá vào lịch sử phòng đấu giá
        BidTransaction tx = new BidTransaction("TX_MOCK", "ROOM_01", "BIDDER_1", 2200.0, LocalDateTime.now());
        room.addBidTransaction(tx);

        room.endAuction();
        assertEquals(AuctionStatus.SUCCESSFUL, room.getStatus());
    }

    @Test
    void testCancelAuctionRoom_OngoingWithHighestBidder_ShouldRefundMoney() {
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        // Giả lập bidder1 đang giữ mức giá cao nhất hiện tại là 2000.0
        AuctionRoom room = new AuctionRoom("ROOM_01", "SELLER_ID", 2000.0, 200.0, end, vehicleItem, bidder1);
        room.restoreStatus(AuctionStatus.ONGOING);

        double currentBalanceBeforeCancel = bidder1.getPlayerBalance(); // 10000.0

        room.cancelAuctionRoom();

        assertEquals(AuctionStatus.CANCELED, room.getStatus());
        // Người đặt giá cao nhất phải được hoàn lại số tiền tương đương giá hiện tại của phòng
        assertEquals(currentBalanceBeforeCancel + 2000.0, bidder1.getPlayerBalance());
    }

    @Test
    void testJoinAuctionRoom_WhenAuctionEnded_ShouldThrowException() {
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        AuctionRoom room = new AuctionRoom("ROOM_01", "SELLER_ID", 2000.0, 200.0, end, vehicleItem, null);
        room.restoreStatus(AuctionStatus.CANCELED); // Trạng thái đã hủy

        assertThrows(IllegalStateException.class, () -> room.joinAuctionRoom(bidder1));
    }

    @Test
    void testPlaceBid_SuccessfulScenario() {
        LocalDateTime open = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        AuctionRoom room = new AuctionRoom("ROOM_01", "SELLER_ID", 1000.0, 100.0, open, end, vehicleItem, null);
        room.restoreStatus(AuctionStatus.ONGOING);

        // Người dùng bắt buộc phải join phòng trước khi đặt giá theo quy tắc hệ thống
        room.joinAuctionRoom(bidder1);

        double oldBalance = bidder1.getPlayerBalance(); // 10000.0
        // Giá hiện tại: 1000, Bước giá: 100 -> Đặt 1100 là hợp lệ
        boolean isBidPlaced = room.placeBid(bidder1, 1100.0);

        assertTrue(isBidPlaced);
        assertEquals(1100.0, room.getCurrentPrice());
        assertEquals("BIDDER_1", room.getHighestBidderId());
        // Số dư của người chơi bị trừ đi lượng tiền đã đặt giá
        assertEquals(oldBalance - 1100.0, bidder1.getPlayerBalance());

        // Kiểm tra xem lịch sử giao dịch đấu giá của phòng đã tăng lên chưa
        List<BidTransaction> history = room.getBidHistory();
        assertEquals(1, history.size());
        assertEquals(1100.0, room.getLatestBidTransaction().getAmount());
    }

    @Test
    void testPlaceBid_SellerAttemptsToBid_ShouldThrowException() {
        LocalDateTime open = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        AuctionRoom room = new AuctionRoom("ROOM_01", "SELLER_ID", 1000.0, 100.0, open, end, artItem, null);
        room.restoreStatus(AuctionStatus.ONGOING);

        room.joinAuctionRoom(seller);

        // Chủ phòng (Người bán) cố tình đặt giá cho sản phẩm của mình -> Trả về lỗi
        assertThrows(IllegalArgumentException.class, () -> room.placeBid(seller, 1200.0));
    }

    @Test
    void testPlaceBid_BidLowerThanStepPrice_ShouldReturnFalse() {
        LocalDateTime open = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        AuctionRoom room = new AuctionRoom("ROOM_01", "SELLER_ID", 1000.0, 100.0, open, end, artItem, null);
        room.restoreStatus(AuctionStatus.ONGOING);

        room.joinAuctionRoom(bidder1);

        // Giá hiện tại: 1000, Bước giá: 100 -> Yêu cầu ít nhất 1100. Đặt 1050 -> Trả về false
        boolean result = room.placeBid(bidder1, 1050.0);
        assertFalse(result);
        assertEquals(1000.0, room.getCurrentPrice()); // Giá phòng đấu giá không thay đổi
    }

    @Test
    void testPlaceBid_OutbidPreviousPlayer_ShouldRefundPreviousBidder() {
        LocalDateTime open = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        AuctionRoom room = new AuctionRoom("ROOM_01", "SELLER_ID", 1000.0, 100.0, open, end, artItem, null);
        room.restoreStatus(AuctionStatus.ONGOING);

        room.joinAuctionRoom(bidder1);
        room.joinAuctionRoom(bidder2);

        // 1. Bidder 1 đặt giá 1200.0 thành công và tạm giữ vị trí cao nhất
        room.placeBid(bidder1, 1200.0);
        double bidder1BalanceAfterFirstBid = bidder1.getPlayerBalance(); // Đã bị trừ tiền tương ứng

        // 2. Bidder 2 nhảy vào trả giá cao hơn hẳn: 1500.0
        boolean result = room.placeBid(bidder2, 1500.0);

        assertTrue(result);
        assertEquals(1500.0, room.getCurrentPrice());
        assertEquals("BIDDER_2", room.getHighestBidderId());

        // 3. Hệ thống phải tự động hoàn tiền cọc 1200.0 lại cho Bidder 1 ngay khi có người trả giá cao hơn
        assertEquals(bidder1BalanceAfterFirstBid + 1200.0, bidder1.getPlayerBalance());
    }

    @Test
    void testPlaceBid_NearEndTime_ShouldTriggerExtendAuctionRoom() {
        LocalDateTime open = LocalDateTime.now().minusMinutes(5);
        // Giả lập thời gian kết thúc phòng chỉ còn 45 giây nữa là đóng cửa (thỏa mãn yêu cầu <= 120 giây)
        LocalDateTime criticalEndTime = LocalDateTime.now().plusSeconds(45);

        AuctionRoom room = new AuctionRoom("ROOM_01", "SELLER_ID", 1000.0, 100.0, open, criticalEndTime, artItem, null);
        room.restoreStatus(AuctionStatus.ONGOING);
        room.joinAuctionRoom(bidder1);

        // Tiến hành đặt giá hợp lệ
        room.placeBid(bidder1, 1200.0);

        // Kiểm tra phòng đấu giá đã được tự động gia hạn thành công hay chưa
        assertEquals(AuctionStatus.EXTENDED, room.getStatus());
        assertTrue(room.getEndTime().isAfter(criticalEndTime));
    }

    @Test
    void testConcurrentBids_SameAmount_ShouldAcceptOnlyOneBid() throws Exception {
        LocalDateTime open = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        AuctionRoom room = new AuctionRoom("ROOM_CONCURRENT", "SELLER_ID", 1000.0, 100.0, open, end, artItem, null);
        room.restoreStatus(AuctionStatus.ONGOING);
        room.joinAuctionRoom(bidder1);
        room.joinAuctionRoom(bidder2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Boolean> bid1 = executor.submit(() -> {
            start.await();
            return room.placeBid(bidder1, 1100.0);
        });
        Future<Boolean> bid2 = executor.submit(() -> {
            start.await();
            return room.placeBid(bidder2, 1100.0);
        });

        start.countDown();
        boolean accepted1 = bid1.get(2, TimeUnit.SECONDS);
        boolean accepted2 = bid2.get(2, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertNotEquals(accepted1, accepted2);
        assertEquals(1100.0, room.getCurrentPrice());
        assertEquals(1, room.getBidHistory().size());
    }
}
