package AUCTIONCODE.Database;

import AUCTIONCODE.Model.Auction.AuctionRoom;
import AUCTIONCODE.Model.Auction.BidTransaction;
import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Manager.AuctionManager;

import java.sql.*;
import java.time.LocalDateTime;

public class BidTransactionDAO {
    private AuctionDAO auctionDAO;
    private USERDAO userDAO;
    public BidTransactionDAO(){
        userDAO= new USERDAO();
        auctionDAO= new AuctionDAO();
    }
    public void save (AuctionRoom auction, Player user, BidTransaction transaction){
        String sql = """
                    INSERT INTO bidTransactions (transactionId,auctionId,bidderId,
                    amount ,timestamp) VALUES(?,?,?,?,?)""";

        String sql1 = """
                    UPDATE users SET balance = ? WHERE userId=?
               """;
        String sql2= """
                    UPDATE Auctions SET currentPrice = ?
                   , status =?, highestBidderId=?, endTime=? WHERE
                   auctionId= ?""";

        try(Connection conn= DatabaseConnection.getInstance().newConnection()){
            conn.setAutoCommit(false);

            try(PreparedStatement pstmt= conn.prepareStatement(sql);
                PreparedStatement pstmt1= conn.prepareStatement(sql1);
                PreparedStatement pstmt2= conn.prepareStatement(sql2)){
            pstmt.setString(1,transaction.getTransactionId());
            pstmt.setString(2, transaction.getAuctionId());
            pstmt.setString(3, transaction.getBidderId());
            pstmt.setDouble(4,transaction.getAmount());
            pstmt.setString(5,transaction.getTimestamp().toString());
            pstmt.executeUpdate();


            pstmt1.setDouble(1,user.getPlayerBalance());
            pstmt1.setString(2,user.getUserId());
            pstmt1.executeUpdate();

            pstmt2.setDouble(1,auction.getCurrentPrice());
            pstmt2.setString(2,auction.getStatus().toString());
            if(auction.getHighestBidderId() != null){
                pstmt2.setString(3,auction.getHighestBidderId());
            }else
                pstmt2.setNull(3, Types.VARCHAR);
            pstmt2.setString(4, auction.getEndTime().toString());
            pstmt2.setString(5, transaction.getAuctionId());
            pstmt2.executeUpdate();
            conn.commit();
        } catch (SQLException e){
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    } catch(SQLException e){
            e.printStackTrace();
        }
        }
    public void loadAll(){
        String sql= """
                SELECT * FROM bidTransactions""";
        try(Connection conn= DatabaseConnection.getInstance().newConnection();
            Statement stmt= conn.createStatement();
            ResultSet rs= stmt.executeQuery(sql)){
            while(rs.next()){
                BidTransaction transaction = new BidTransaction(
                        rs.getString("transactionId"),
                        rs.getString("auctionId"),
                        rs.getString("bidderId"),
                        rs.getDouble("amount"),
                        LocalDateTime.parse(rs.getString("timestamp"))

                );
                AuctionRoom auction = AuctionManager.getInstance().getAuction(rs.getString("auctionId"));
                if(auction != null){
                    auction.addBidTransaction(transaction);
                }
            }
            }catch (SQLException e){
                e.printStackTrace();
        }
    }
}
