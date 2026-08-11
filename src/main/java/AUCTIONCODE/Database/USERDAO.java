package AUCTIONCODE.Database;

import AUCTIONCODE.AuthModule.AuthService;
import AUCTIONCODE.Model.User.Admin;
import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.User;
import AUCTIONCODE.Model.User.UserInformation;
import AUCTIONCODE.Manager.UserManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class USERDAO {//QUAN LY BEN USER

    public void save(User user) {
        String sql = """
                INSERT INTO users 
                (userId,userName,hashPassword,balance,
                role,fullname,email,phone,address,businessCode) 
                VALUES (?,?,?,?,?,?,?,?,?,?) """;
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, user.getUserId());
            pstmt.setString(2, user.getUserName());
            pstmt.setString(3, user.getUserPassword());
            if(user instanceof Player){
                pstmt.setDouble(4,((Player) user).getPlayerBalance());
            }else{
                pstmt.setDouble(4,0.0);
            }
            pstmt.setString(5, user.getUserRole());
            pstmt.setString(6, user.getUserInformation().getFullName());
            pstmt.setString(7, user.getUserInformation().getEmail());
            pstmt.setString(8, user.getUserInformation().getPhoneNumber());
            pstmt.setString(9, user.getUserInformation().getAddress());
            if (user instanceof Admin) {
                pstmt.setString(10, ((Admin) user).getBusinessCode());
            } else {
                pstmt.setNull(10, Types.VARCHAR);
            }
            int rows = pstmt.executeUpdate();
            System.out.println("USERDAO.save() rows affected: " + rows); // THÊM DÒNG NÀY

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User findByUserName(String userName) {
        String sql = """
                SELECT * FROM users WHERE
                userName= ?""";
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapToUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User findbyId(String userId) {
        String sql = """
                SELECT * FROM users 
                WHERE userId = ?""";
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
               return mapToUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void update(double balance, String userId) {
        String sql = """
                UPDATE users SET balance = ? Where userId = ?""";
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setDouble(1, balance);
            pstmt.setString(2, userId);
            int rs = pstmt.executeUpdate();
            if (rs > 0) {
                System.out.println("Cập nhật số dư thành công");
            } else {
                System.out.println("Không tìm thấy userId phù hợp");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateInformation(User user) {
        String sql = """
                UPDATE users SET fullname = ?, email = ?, phone = ?, address = ?
                WHERE userId = ?""";
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, user.getUserInformation().getFullName());
            pstmt.setString(2, user.getUserInformation().getEmail());
            pstmt.setString(3, user.getUserInformation().getPhoneNumber());
            pstmt.setString(4, user.getUserInformation().getAddress());
            pstmt.setString(5, user.getUserId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePassword(String userId, String hashPassword) {
        String sql = """
                UPDATE users SET hashPassword = ?
                WHERE userId = ?""";
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, hashPassword);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean delete(String userId) {
        String sql = """
                DELETE FROM users WHERE userId = ?""";
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void loadAll() {
        String sql = """
                SELECT * FROM users """;
        try (Connection conn = DatabaseConnection.getInstance().newConnection();
             PreparedStatement Pstmt= conn.prepareStatement(sql)){
            ResultSet rs = Pstmt.executeQuery();
            while (rs.next()) {
               User user= mapToUser(rs);
                //Nạp vào RAM
                UserManager.getInstance().addUser(user, user.getUserId());
                AuthService.getInstance().loadUserToAuth(user.getUserName(),user);

            }

        } catch (SQLException E) {
            E.printStackTrace();
        }
    }
    public List<User> findbyRole(String role){
        String sql= """
                SELECT * FROM users WHERE role=? """;
        List<User> Users= new ArrayList<>();
        try(Connection conn = DatabaseConnection.getInstance().newConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1,role);
            ResultSet rs= pstmt.executeQuery();
            while (rs.next()){
              Users.add(mapToUser(rs));
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return Users;

    }
    private User mapToUser(ResultSet rs) throws SQLException{
        UserInformation info = new UserInformation(
                rs.getString("address"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("fullname")
        );
        String role = rs.getString("role");
        if ("Admin".equals(role)){
            return new Admin(
                    rs.getString("userName"),
                    info,
                    rs.getString("hashPassword"),
                    rs.getString("userId"),
                    rs.getString("businessCode"),
                    role

            );

        }else{
            return new Player(
                    rs.getString("userName"),
                    info,
                    rs.getString("hashPassword"),
                    rs.getString("userId"),
                    rs.getDouble("balance"),
                    role
            );
        }
    }


}

