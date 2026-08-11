package AUCTIONCODE.Model.Auction;

import AUCTIONCODE.Database.AuctionDAO;
import AUCTIONCODE.Database.USERDAO;
import AUCTIONCODE.Manager.UserManager;
import AUCTIONCODE.Model.OtherInterface.AuctionStatus;
import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/// Concept: Khi auction được tạo, bạn tính xem còn bao nhiêu giây đến endTime,
/// rồi đặt hẹn giờ — đúng lúc đó tự động gọi endAuction().
public class AuctionScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4, daemonThreadFactory());
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final USERDAO userDAO = new USERDAO();
    private static AuctionScheduler instance;

    public static AuctionScheduler getInstance() {
        if (instance == null) {
            synchronized (AuctionScheduler.class) {
                if (instance == null) {
                    instance = new AuctionScheduler();
                }
            }
        }
        return instance;
    }
    public void scheduleEnd(AuctionRoom room) {
        long delaySeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), room.getEndTime());
        if (delaySeconds <= 0) {
            room.endAuction();
            persistFinishedAuction(room);
            return;
        }
        scheduler.schedule(() -> {
            try {
                if (room.getStatus() == AuctionStatus.ONGOING
                        || room.getStatus() == AuctionStatus.EXTENDED) {
                    if (LocalDateTime.now().isBefore(room.getEndTime())) {
                        scheduleEnd(room);
                        return;
                    }
                    room.endAuction();
                    persistFinishedAuction(room);
                }
            } catch (Exception e) {
                System.err.println("[Scheduler] Lỗi: " + room.getId() + ": " + e.getMessage());
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private void persistFinishedAuction(AuctionRoom room) {
        if (room.getStatus() == AuctionStatus.SUCCESSFUL) {
            User seller = UserManager.getInstance().getUser(room.getSellerId());
            if (seller instanceof Player sellerPlayer) {
                sellerPlayer.setPlayerBalance(sellerPlayer.getPlayerBalance() + room.getCurrentPrice());
                userDAO.update(sellerPlayer.getPlayerBalance(), sellerPlayer.getUserId());
            }
        }
        auctionDAO.update(room);
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    private static ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "auction-scheduler");
            thread.setDaemon(true);
            return thread;
        };
    }

}

