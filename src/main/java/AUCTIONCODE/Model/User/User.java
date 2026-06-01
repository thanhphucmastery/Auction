package AUCTIONCODE.Model.User;

import AUCTIONCODE.Model.OtherInterface.Observer;
import AUCTIONCODE.Model.OtherInterface.Prototype;


public abstract class User implements Observer, Prototype<User> {
    private String userName;
    private UserInformation userInformation;
    private String hashpassword;
    private String userId;
    private String role;

    public User(String userName, UserInformation userInformation, String hashpassword, String userId,String role){
        this.userName = userName;
        this.userInformation = userInformation;
        this.hashpassword = hashpassword;
        this.userId = userId;
        this.role = role;

    }

    public String getUserPassword() {
        return this.hashpassword;
    }

    public void setUserPassword(String hashpassword) {
        this.hashpassword = hashpassword;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getUserId() {
        return this.userId;
    }

    public UserInformation getUserInformation() {
        return this.userInformation;
    }

    public void setUserInformation(UserInformation userInformation) {
        this.userInformation = userInformation;
    }

    public String getUserRole() {
        return role;
    }

    @Override
    public void update(String bidderId, double amount) {
    }
}

