package AUCTIONCODE.Model.Auction;

import java.time.LocalDateTime;

public class BidTransaction {
    private String transactionId;
    private String auctionId, bidderId;
    private double amount;
    private LocalDateTime timestamp;

    ///  giao dịch đặt giá
    public BidTransaction(String transactionId, String auctionId, String bidderId, double amount, LocalDateTime timestamp) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.transactionId= transactionId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public String getTransactionId(){ return transactionId;}
}
