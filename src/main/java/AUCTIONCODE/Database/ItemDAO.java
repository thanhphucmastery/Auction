package AUCTIONCODE.Database;
import AUCTIONCODE.Model.Item.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_IN_AUCTION = "IN_AUCTION";

    public void save(Item item) {
        save(item, null, STATUS_IN_AUCTION);
    }

    public void saveInventoryItem(Item item, String ownerId) {
        save(item, ownerId, STATUS_AVAILABLE);
    }

    private void save(Item item, String ownerId, String status) {
        String sql = """
                INSERT INTO Item (itemId,name,description,type,artist,yearCreated,brand,yearMade,warranty,
                model,mileage,ownerId,status,imagePath) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)""";
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, item.getId());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());

            if (item instanceof Art) {
                pstmt.setString(4, "Art");
                pstmt.setString(5, ((Art) item).getArtist());
                pstmt.setInt(6, ((Art) item).getYearCreated());
                pstmt.setNull(7, Types.VARCHAR);
                pstmt.setNull(8, Types.INTEGER);
                pstmt.setNull(9, Types.INTEGER);
                pstmt.setNull(10, Types.VARCHAR);
                pstmt.setNull(11, Types.REAL);

            } else if (item instanceof Electronics) {
                pstmt.setString(4, "Electronics");
                pstmt.setNull(5, Types.VARCHAR);
                pstmt.setNull(6, Types.INTEGER);
                pstmt.setString(7, ((Electronics) item).getBrand());
                pstmt.setInt(8, ((Electronics) item).getYearMade());
                pstmt.setInt(9, ((Electronics) item).getWarranty());
                pstmt.setNull(10, Types.VARCHAR);
                pstmt.setNull(11, Types.REAL);

            } else if (item instanceof Vehicle) {
                pstmt.setString(4, "Vehicle");
                pstmt.setNull(5, Types.VARCHAR);
                pstmt.setNull(6, Types.INTEGER);
                pstmt.setNull(7, Types.VARCHAR);
                pstmt.setInt(8, ((Vehicle) item).getYearMade());
                pstmt.setNull(9, Types.INTEGER);
                pstmt.setString(10, ((Vehicle) item).getModel());
                pstmt.setDouble(11, ((Vehicle) item).getMileage());

            }
            pstmt.setString(12, ownerId);
            pstmt.setString(13, status);
            pstmt.setString(14, item.getImagePath());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Item findbyID(String itemId) {
        String sql = """
                SELECT * FROM Item WHERE itemId=?""";
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                 return switch (rs.getString("type")) {
                    case "Art" -> ItemFactory.createArt(
                            rs.getString("itemId"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("imagePath"),
                            rs.getString("artist"),
                            rs.getInt("yearCreated")
                    );
                    case "Electronics" -> ItemFactory.createElectronics(
                            rs.getString("itemId"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("imagePath"),
                            rs.getString("brand"),
                            rs.getInt("yearMade"),
                            rs.getInt("warranty")
                    );
                    case "Vehicle" -> ItemFactory.createVehicle(
                            rs.getString("itemId"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("imagePath"),
                            rs.getDouble("mileage"),
                            rs.getString("model"),
                            rs.getInt("yearMade")


                    );
                    default-> throw new IllegalArgumentException("Unknown Item Type");
                };
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    return null;
    }
    public void update(Item item){
        update(item, null);
    }

    public void updateInventoryItem(Item item, String ownerId){
        update(item, ownerId);
    }

    private void update(Item item, String ownerId){
        String sql = """
                UPDATE Item SET name = ?, description = ?, type = ?, imagePath = ?
                ,artist = ?, yearCreated = ?
                , brand = ?,yearMade= ?, warranty= ?,model= ?,
                mileage=? Where itemId= ?""" + (ownerId == null ? "" : " AND ownerId = ? AND COALESCE(status,'AVAILABLE') = 'AVAILABLE'");
        try(Connection conn = DatabaseConnection.getInstance().newConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1,item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setString(3, getType(item));
            pstmt.setString(4, item.getImagePath());

            if(item instanceof Art art){
                pstmt.setString(5, art.getArtist());
                pstmt.setInt(6, art.getYearCreated());
                pstmt.setNull(7,Types.VARCHAR);
                pstmt.setNull(8,Types.INTEGER);
                pstmt.setNull(9,Types.INTEGER);
                pstmt.setNull(10,Types.VARCHAR);
                pstmt.setNull(11,Types.REAL);

            }else if (item instanceof Electronics){
                pstmt.setNull(5,Types.VARCHAR);
                pstmt.setNull(6,Types.INTEGER);
                pstmt.setString(7,((Electronics) item).getBrand());
                pstmt.setInt(8,((Electronics) item).getYearMade());
                pstmt.setInt(9,((Electronics) item).getWarranty());
                pstmt.setNull(10,Types.VARCHAR);
                pstmt.setNull(11,Types.REAL);

            }else if(item instanceof Vehicle){
                pstmt.setNull(5,Types.VARCHAR);
                pstmt.setNull(6,Types.INTEGER);
                pstmt.setNull(7,Types.VARCHAR);
                pstmt.setInt(8,((Vehicle) item).getYearMade());
                pstmt.setNull(9,Types.INTEGER);
                pstmt.setString(10,((Vehicle) item).getModel());
                pstmt.setDouble(11,((Vehicle) item).getMileage());
            }
            pstmt.setString(12,item.getId());
            if (ownerId != null) {
                pstmt.setString(13, ownerId);
            }
            pstmt.executeUpdate();


        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public List<Item> findAvailableByOwner(String ownerId) {
        String sql = """
                SELECT * FROM Item
                WHERE ownerId = ? AND COALESCE(status,'AVAILABLE') = 'AVAILABLE'
                ORDER BY name COLLATE NOCASE""";
        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public Item findAvailableByOwner(String itemId, String ownerId) {
        String sql = """
                SELECT * FROM Item
                WHERE itemId = ? AND ownerId = ? AND COALESCE(status,'AVAILABLE') = 'AVAILABLE'""";
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, itemId);
            pstmt.setString(2, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return fromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteAvailable(String itemId, String ownerId){
        String sql = """
                DELETE FROM Item
                WHERE itemId = ? AND ownerId = ? AND COALESCE(status,'AVAILABLE') = 'AVAILABLE'""";
        try(Connection conn = DatabaseConnection.getInstance().newConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1,itemId);
            pstmt.setString(2,ownerId);
            return pstmt.executeUpdate() > 0;
        }catch(SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean markInAuction(String itemId, String ownerId){
        String sql = """
                UPDATE Item SET status = ?
                WHERE itemId = ? AND ownerId = ? AND COALESCE(status,'AVAILABLE') = 'AVAILABLE'""";
        try(Connection conn = DatabaseConnection.getInstance().newConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, STATUS_IN_AUCTION);
            pstmt.setString(2,itemId);
            pstmt.setString(3,ownerId);
            return pstmt.executeUpdate() > 0;
        }catch(SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    public void delete(Item item){
        String sql= """
                DELETE FROM Item WHERE itemId=?""";
        try(Connection conn = DatabaseConnection.getInstance().newConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1,item.getId());
            int rowAffected= pstmt.executeUpdate();
            if(rowAffected>0){
                System.out.println("ĐÃ XOÁ ITEM THÀNH CÔNG");
            }else{
                System.out.println("KHÔNG TÌM THẤY SẢN PHẨM");
            }

        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    private Item fromResultSet(ResultSet rs) throws SQLException {
        return switch (rs.getString("type")) {
            case "Art" -> ItemFactory.createArt(
                    rs.getString("itemId"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("imagePath"),
                    rs.getString("artist"),
                    rs.getInt("yearCreated")
            );
            case "Electronics" -> ItemFactory.createElectronics(
                    rs.getString("itemId"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("imagePath"),
                    rs.getString("brand"),
                    rs.getInt("yearMade"),
                    rs.getInt("warranty")
            );
            case "Vehicle" -> ItemFactory.createVehicle(
                    rs.getString("itemId"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("imagePath"),
                    rs.getDouble("mileage"),
                    rs.getString("model"),
                    rs.getInt("yearMade")
            );
            default -> throw new IllegalArgumentException("Unknown Item Type");
        };
    }

    public String getType(Item item) {
        if (item instanceof Art) {
            return "Art";
        }
        if (item instanceof Electronics) {
            return "Electronics";
        }
        if (item instanceof Vehicle) {
            return "Vehicle";
        }
        throw new IllegalArgumentException("Unknown Item Type");
    }

}

