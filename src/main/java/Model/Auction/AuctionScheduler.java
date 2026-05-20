package Model.Auction;

import Model.OtherInterface.AuctionStatus;

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
                }
            } catch (Exception e) {
                System.err.println("[Scheduler] Lỗi: " + room.getId() + ": " + e.getMessage());
            }
        }, delaySeconds, TimeUnit.SECONDS);
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

