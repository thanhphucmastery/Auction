package AUCTIONCODE.Model.Auction;

import AUCTIONCODE.Model.Item.Item;
import AUCTIONCODE.Model.Item.ItemFactory;
import AUCTIONCODE.Model.OtherInterface.AuctionStatus;
import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.UserInformation;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class AuctionSchedulerTest {

    @Test
    void testSingletonInstance_ShouldReturnSameInstance() {
        AuctionScheduler instance1 = AuctionScheduler.getInstance();
        AuctionScheduler instance2 = AuctionScheduler.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2); // Đảm bảo tính duy nhất của Singleton
    }

    @Test
    void testScheduleEnd_WhenEndTimeHasPassed_ShouldEndAuctionImmediately() {
        AuctionScheduler scheduler = AuctionScheduler.getInstance();

        // Khởi tạo thông tin người tham gia và sản phẩm thật (Electronics)
        UserInformation info = new UserInformation("Hanoi", "0912345678", "seller@test.com", "Seller Name");
        Player highestBidder = new Player("bidder1", info, "pass", "B01", 50000.0, "PLAYER");
        Item item = ItemFactory.createElectronics("E01", "Laptop Dell", "Core i7 16GB", "Dell", 2023, 12);

        // Đặt thời gian kết thúc ở quá khứ (đã quá hạn 30 phút)
        LocalDateTime pastEndTime = LocalDateTime.now().minusMinutes(30);
        AuctionRoom room = new AuctionRoom("ROOM_EXPIRED", "SELLER_01", 1000.0, 100.0, pastEndTime, item, highestBidder);
        room.restoreStatus(AuctionStatus.ONGOING);

        // Kích hoạt scheduler
        scheduler.scheduleEnd(room);

        // Vì delaySeconds <= 0, hệ thống phải thực hiện đóng phòng ngay lập tức.
        // Do danh sách giao dịch (history) trống, trạng thái sẽ chuyển thành NO_BIDS
        assertEquals(AuctionStatus.NO_BIDS, room.getStatus());
    }
}