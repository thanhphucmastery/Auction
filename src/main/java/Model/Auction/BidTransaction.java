package Model.Auction;

import java.time.LocalDateTime;

public class BidTransaction {
    private String auctionId, bidderId;
    private double amount;
    private LocalDateTime timestamp;

    ///  giao dịch đặt giá
    public BidTransaction(String auctionId, String bidderId, double amount, LocalDateTime timestamp) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = timestamp;
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
}
