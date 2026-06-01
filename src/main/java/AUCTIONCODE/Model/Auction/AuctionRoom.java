package AUCTIONCODE.Model.Auction;

import AUCTIONCODE.Model.Item.Item;
import AUCTIONCODE.Model.OtherInterface.AuctionStatus;
import AUCTIONCODE.Model.OtherInterface.Observer;
import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.User;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AuctionRoom {
    private String id, sellerId;
    private double currentPrice, stepPrice;
    private LocalDateTime openTime, endTime;
    private AuctionStatus status;
    private Player highestBidder;
    private Item item;
    private final List<BidTransaction> history= new ArrayList<>();
    private final List<Observer> observers = new CopyOnWriteArrayList<>();
    private final Set<String> participantIds = new HashSet<>();
    private final AuctionScheduler auctionScheduler = AuctionScheduler.getInstance();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();



    public void addObserver(Observer e) {
        if (!observers.contains(e)) {
            observers.add(e);
        }
    }
    public List<Observer> getObservers() {
        return observers;
    }

    public AuctionRoom(String id, String sellerId, double currentPrice, double stepPrice, LocalDateTime endTime, Item item,Player highestBidder){
        this(id, sellerId, currentPrice, stepPrice, LocalDateTime.now(), endTime, item, highestBidder);
    }

    public AuctionRoom(String id, String sellerId, double currentPrice, double stepPrice, LocalDateTime openTime, LocalDateTime endTime, Item item,Player highestBidder){
        this.id=id;
        this.sellerId=sellerId;
        this.currentPrice=currentPrice;
        this.stepPrice=stepPrice;
        this.openTime=openTime;
        this.endTime=endTime;
        this.highestBidder=highestBidder;
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

    public LocalDateTime getOpenTime(){
        return openTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus(){return status;}

    public Item getItem() {
        return item;
    }

    public String getHighestBidderId(){ return highestBidder != null ? highestBidder.getUserId(): null;}

    public Player refundHighestBidder() {
        lock.writeLock().lock();
        try {
            if (highestBidder == null) {
                return null;
            }
            highestBidder.setPlayerBalance(highestBidder.getPlayerBalance() + currentPrice);
            Player refundedPlayer = highestBidder;
            highestBidder = null;
            return refundedPlayer;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addBidTransaction(BidTransaction transaction) {
        lock.writeLock().lock();
        try {
            history.add(transaction);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<BidTransaction> getBidHistory() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(history);
        } finally {
            lock.readLock().unlock();
        }
    }

    public BidTransaction getLatestBidTransaction() {
        lock.readLock().lock();
        try {
            if (history.isEmpty()) {
                return null;
            }
            return history.get(history.size() - 1);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void restoreStatus(AuctionStatus status){
        this.status=status;
    }



    /// Vòng đời của AuctionRoom
    public void startAuctionRoom(){
        if (this.status==AuctionStatus.UPCOMING && !LocalDateTime.now().isBefore(openTime)){
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
            refundHighestBidder();
            this.status=AuctionStatus.CANCELED;
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
        if (LocalDateTime.now().isBefore(openTime)) {
            throw new IllegalStateException("Phiên đấu giá chưa mở.");
        }
        addObserver(user);
        participantIds.add(user.getUserId());
    }

    public boolean placeBid(Player user, double bidAmount){
        lock.writeLock().lock();
        try {
            if (this.status != AuctionStatus.ONGOING && this.status != AuctionStatus.EXTENDED ) {
                throw new IllegalStateException("Auction is not accepting bids.");
            }
            if (LocalDateTime.now().isBefore(openTime)) {
                throw new IllegalArgumentException("Auction has not opened.");
            }
            if (LocalDateTime.now().isAfter(endTime)){
                throw new IllegalArgumentException("Auction has ended.");
            }
            if (user.getUserId().equals(this.sellerId)){
                throw new IllegalArgumentException("Seller cannot bid on own auction.");
            }
            if (!participantIds.contains(user.getUserId())){
                throw new IllegalArgumentException("User must join auction before bidding.");
            }
            if (bidAmount>=this.currentPrice+this.stepPrice && bidAmount<= user.getPlayerBalance()){
                if (this.highestBidder!=null){
                    this.highestBidder.setPlayerBalance(this.highestBidder.getPlayerBalance()+this.currentPrice);
                }
                this.currentPrice = bidAmount;
                user.setPlayerBalance(user.getPlayerBalance()-bidAmount);
                System.out.println("Số dư tài khoản bạn là"+" "+user.getPlayerBalance());
                this.highestBidder=user;
                String transactionId= UUID.randomUUID().toString();
                BidTransaction newTransaction= new BidTransaction(transactionId,this.id,user.getUserId(),bidAmount,LocalDateTime.now());
                history.add(newTransaction);
                if (Duration.between(LocalDateTime.now(), endTime).toSeconds() <= 120) {
                    extendAuctionRoom(2);
                }
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



}
