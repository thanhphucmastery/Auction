package AUCTIONCODE.Database;

import AUCTIONCODE.Model.Auction.AuctionRoom;
import AUCTIONCODE.Model.OtherInterface.AuctionStatus;
import AUCTIONCODE.Model.Item.Item;
import AUCTIONCODE.Manager.AuctionManager;
import AUCTIONCODE.Model.User.Player;

import java.sql.*;
import java.time.LocalDateTime;

public class AuctionDAO {
    private ItemDAO itemDAO= new ItemDAO();
    private USERDAO userDAO= new USERDAO();
    public void save(AuctionRoom auction, Item item){
        itemDAO.save(item);
        insertAuction(auction, item);
    }

    public void saveWithExistingItem(AuctionRoom auction, Item item){
        insertAuction(auction, item);
    }

    private void insertAuction(AuctionRoom auction, Item item){
        String sql= """
                INSERT INTO Auctions (auctionId,sellerId,currentPrice,stepPrice,
                openTime,endtime,status,itemId,highestBidderId)
                 VALUES (?,?,?,?,?,?,?,?,?)""";
        try(Connection conn= DatabaseConnection.getInstance().newConnection();
            PreparedStatement pstmt= conn.prepareStatement(sql)){
            pstmt.setString(1,auction.getId());
            pstmt.setString(2, auction.getSellerId());
            pstmt.setDouble (3,auction.getCurrentPrice());
            pstmt.setDouble (4,auction.getStepPrice());
            pstmt.setString(5,auction.getOpenTime().toString());
            pstmt.setString(6,auction.getEndTime().toString());
            pstmt.setString(7,auction.getStatus().toString());
            pstmt.setString(8,item.getId());

            if(auction.getHighestBidderId() != null){
                pstmt.setString(9,auction.getHighestBidderId());
            }else{
                pstmt.setNull(9, Types.VARCHAR);
            }

        pstmt.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }
    public void update(AuctionRoom auction){
        String sql= """
                UPDATE Auctions SET currentPrice= ?
               ,status = ?, HighestBidderId = ?, endTime = ? WHERE auctionId= ?""";
        try(Connection conn = DatabaseConnection.getInstance().newConnection();
            PreparedStatement pstmt= conn.prepareStatement(sql)){
            pstmt.setDouble(1,auction.getCurrentPrice());
            pstmt.setString(2,auction.getStatus().toString());
            if(auction.getHighestBidderId() !=null) {
                pstmt.setString(3, auction.getHighestBidderId());
            }else
                pstmt.setNull(3, Types.VARCHAR);
            pstmt.setString(4, auction.getEndTime().toString());
            pstmt.setString(5,auction.getId());
        pstmt.executeUpdate();
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }
    public void loadAll(){
        String sql= """
                SELECT * FROM Auctions """;
        try(Connection conn= DatabaseConnection.getInstance().newConnection();
            Statement stmt= conn.createStatement();
            ResultSet rs= stmt.executeQuery(sql);){
            while(rs.next()){
                String itemId= rs.getString("itemId");
                Item item =itemDAO.findbyID(itemId);
                String highestBidderId=rs.getString("highestBidderId");
                Player highestBidder= null;
                if(highestBidderId != null){
                    highestBidder= (Player) userDAO.findbyId(highestBidderId);
                }
                AuctionRoom auction = new AuctionRoom(
                        rs.getString("auctionId"),
                        rs.getString("sellerId"),
                        rs.getDouble ("currentPrice"),
                        rs.getDouble("stepPrice"),
                        parseOpenTime(rs),
                        LocalDateTime.parse(rs.getString("endtime")),
                        item,
                        highestBidder


                );
                auction.restoreStatus(AuctionStatus.valueOf(rs.getString("status")));
                AuctionManager.getInstance().addAuction(auction);

            }

        }catch(SQLException e){
            e.printStackTrace();
        }
    }
    public static int getNextCounter() {
        String sql = "SELECT COUNT(*) FROM Auctions";
        try
            (Connection conn = DatabaseConnection.getInstance().newConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);){
            if (rs.next()) return rs.getInt(1) + 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }
    public void updateStatus(String auctionId, AuctionStatus status){
        String sql= """
                UPDATE Auctions SET status = ?
                WHERE auctionId=?""";
        try(Connection conn= DatabaseConnection.getInstance().newConnection();
            PreparedStatement pstmt= conn.prepareStatement(sql)){
            pstmt.setString(1,status.toString());
            pstmt.setString(2,auctionId);
            pstmt.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
    }

    }

    private LocalDateTime parseOpenTime(ResultSet rs) throws SQLException {
        String openTime = rs.getString("openTime");
        if (openTime == null || openTime.isBlank()) {
            return LocalDateTime.parse(rs.getString("endtime")).minusMinutes(3);
        }
        return LocalDateTime.parse(openTime);
    }

    public void updateEndTimeAndStatus(String auctionId, LocalDateTime endTime, AuctionStatus status) {
        String sql = """
                UPDATE Auctions SET endTime = ?, status = ?
                WHERE auctionId = ?""";
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, endTime.toString());
            pstmt.setString(2, status.toString());
            pstmt.setString(3, auctionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void delete(String auctionId){
        String sql= """
                DELETE FROM Auctions WHERE auctionId=?""";
        try(Connection conn= DatabaseConnection.getInstance().newConnection();
            PreparedStatement pstmt= conn.prepareStatement(sql)){
            pstmt.setString(1,auctionId);
            int rowAffected= pstmt.executeUpdate();
            if(rowAffected >0){
                System.out.println("Đã xoá phiên dữ liệu thành công");
            }else{
                System.out.println("KHông tìm thấy phiên đấu giá");
            }
        } catch(SQLException e){
            e.printStackTrace();
        }
    }

    public boolean deleteCascade(String auctionId){
        String itemId = null;
        String selectSql = "SELECT itemId FROM Auctions WHERE auctionId = ?";
        String deleteBidsSql = "DELETE FROM bidTransactions WHERE auctionId = ?";
        String deleteAuctionSql = "DELETE FROM Auctions WHERE auctionId = ?";
        String deleteItemSql = "DELETE FROM Item WHERE itemId = ?";
        try(Connection conn= DatabaseConnection.getInstance().newConnection()){
            conn.setAutoCommit(false);
            try (PreparedStatement select = conn.prepareStatement(selectSql);
                 PreparedStatement deleteBids = conn.prepareStatement(deleteBidsSql);
                 PreparedStatement deleteAuction = conn.prepareStatement(deleteAuctionSql);
                 PreparedStatement deleteItem = conn.prepareStatement(deleteItemSql)) {
                select.setString(1, auctionId);
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next()) {
                        itemId = rs.getString("itemId");
                    }
                }

                deleteBids.setString(1, auctionId);
                deleteBids.executeUpdate();

                deleteAuction.setString(1, auctionId);
                int rowAffected = deleteAuction.executeUpdate();

                if (itemId != null && rowAffected > 0) {
                    deleteItem.setString(1, itemId);
                    deleteItem.executeUpdate();
                }
                conn.commit();
                return rowAffected > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch(SQLException e){
            e.printStackTrace();
            return false;
        }
    }
}
