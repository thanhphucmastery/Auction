package Model.Auction;

import Model.Item.Item;
import Model.OtherInterface.AuctionStatus;
import Model.OtherInterface.Observer;
import Model.User.Player;
import Model.User.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AuctionRoom {
    public static final long AUTO_EXTEND_THRESHOLD_SECONDS = 10;
    public static final long AUTO_EXTEND_SECONDS = 60;

    private String id, sellerId;
    private double currentPrice, stepPrice;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private Player highestBidder;
    private Item item;
    private final List<BidTransaction> history= new ArrayList<>();
    private final List<Observer> observers = new CopyOnWriteArrayList<>();
    private final Set<String> participantIds = new HashSet<>();
    private final AuctionScheduler auctionScheduler = AuctionScheduler.getInstance();


    public void addObserver(Observer e) {
        if (!observers.contains(e)) {
            observers.add(e);
        }
    }
    public List<Observer> getObservers() {
        return observers;
    }

    public AuctionRoom(String id, String sellerId, double currentPrice, double stepPrice, LocalDateTime endTime, Item item){
        this.id=id;
        this.sellerId=sellerId;
        this.currentPrice=currentPrice;
        this.stepPrice=stepPrice;
        this.endTime=endTime;
        this.highestBidder=null;
        this.item=item;
        this.status=AuctionStatus.UPCOMING;
    }

    /// GET/SET CỦA CLASS
    public String getId(){
        return id;
    }

    public String getSellerId(){
        return sellerId;
    }

    public double getCurrentPrice(){
        return currentPrice;
    }

    public double getStepPrice(){
        return stepPrice;
    }

    public LocalDateTime getEndTime(){
        return endTime;
    }

    public AuctionStatus getStatus(){return status;}



    /// Vòng đời của AuctionRoom
    public void startAuctionRoom(){
        if (this.status==AuctionStatus.UPCOMING){
            this.status=AuctionStatus.ONGOING;
        }
    }


    public void endAuction(){
        if (this.status != AuctionStatus.ONGOING && this.status != AuctionStatus.EXTENDED) {
            throw new IllegalStateException("Chỉ kết thúc phiên đang chạy.");
        }
        if (history.isEmpty()) {
            this.status = AuctionStatus.NO_BIDS;
        } else {
            this.status = AuctionStatus.SUCCESSFUL;
        }
        System.out.println("[AuctionService] Auction " + id + " kết thúc - Status: " + getStatus());
    }



    public void cancelAuctionRoom(){
        if (this.status==AuctionStatus.UPCOMING ){
            this.status=AuctionStatus.CANCELED;
        } else if (this.status==AuctionStatus.ONGOING || this.status==AuctionStatus.EXTENDED ){
            if (this.highestBidder!=null){
                this.highestBidder.setPlayerBalance(this.highestBidder.getPlayerBalance()+this.currentPrice);
                this.status=AuctionStatus.CANCELED;
            } else if (this.highestBidder==null){
                this.status=AuctionStatus.CANCELED;
            }
        } else {
            throw new IllegalArgumentException("Trạng thái này không thể cancel");
        }
    }

    public void extendAuctionRoom(long minutes){
        if (this.status==AuctionStatus.ONGOING || this.status==AuctionStatus.EXTENDED){
            LocalDateTime newEndTime = this.endTime.plusMinutes(minutes);
            this.endTime=newEndTime;
            this.status=AuctionStatus.EXTENDED;
            auctionScheduler.scheduleEnd(this);
        }
    }
    /// logic hệ thống đặt bid

    public void joinAuctionRoom(User user) {

        // Thêm client vào danh sách theo dõi
        if (getStatus() == AuctionStatus.SUCCESSFUL ||
                getStatus() == AuctionStatus.NO_BIDS ||
                getStatus() == AuctionStatus.CANCELED) {
            throw new IllegalStateException("Phiên đấu giá đã kết thúc.");
        }
        addObserver(user);
        participantIds.add(user.getUserId());
    }

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public boolean placeBid(Player user, double bidAmount){
        if (this.status != AuctionStatus.ONGOING && this.status != AuctionStatus.EXTENDED ) {
            throw new IllegalStateException("Chỉ có thể đặt giá khi phiên đấu giá đang diễn ra.");
        }
        if (LocalDateTime.now().isAfter(endTime)){
            throw new IllegalArgumentException("Phiên đấu giá đã kết thúc");
        }
        if (user.getUserId().equals(this.sellerId)){
            throw new IllegalArgumentException("Người bán không được đặt giá");
        }
        if (!participantIds.contains(user.getUserId())){
            throw new IllegalArgumentException("Phải vào phòng mới được đặt giá");
        }
        lock.writeLock().lock();
        try {
            if (bidAmount>=this.currentPrice+this.stepPrice && bidAmount<= user.getPlayerBalance()){
                if (this.highestBidder!=null){
                    this.highestBidder.setPlayerBalance(this.highestBidder.getPlayerBalance()+this.currentPrice);
                }
                this.currentPrice = bidAmount;
                user.setPlayerBalance(user.getPlayerBalance()-bidAmount);
                System.out.println("Số dư tài khoản bạn là"+" "+user.getPlayerBalance());
                this.highestBidder=user;
                BidTransaction newTransaction= new BidTransaction(this.id,user.getUserId(),bidAmount,LocalDateTime.now());
                history.add(newTransaction);
                autoExtendIfBidNearEnd();
                for (Observer i : observers){
                    i.update(user.getUserId(), bidAmount);
                }
                return true;
            }
            else {
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void autoExtendIfBidNearEnd() {
        long remainingSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), this.endTime);
        if (remainingSeconds >= 0 && remainingSeconds <= AUTO_EXTEND_THRESHOLD_SECONDS) {
            this.endTime = this.endTime.plusSeconds(AUTO_EXTEND_SECONDS);
            this.status = AuctionStatus.EXTENDED;
            auctionScheduler.scheduleEnd(this);
            System.out.println("[AuctionService] Auction " + id + " auto-extended to " + this.endTime);
        }
    }


}
