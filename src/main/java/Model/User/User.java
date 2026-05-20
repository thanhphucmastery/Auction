package Model.User;

import Model.OtherInterface.Observer;
import Model.OtherInterface.Prototype;
public abstract class User implements Observer, Prototype<User> {
    private String userName;
    private UserInformation userInformation;
    private String hashpassword;
    private String userId;

    public User(String userName, UserInformation userInformation, String hashpassword,String userId){
        this.userName=userName;
        this.userInformation=userInformation;
        this.hashpassword=hashpassword;
        this.userId=userId;

    }

    public String getUserPassword(){
        return this.hashpassword;
    }

    public String getUserName(){
        return this.userName;
    }
    public String getUserId() {
        return this.userId;
    }

    public UserInformation getUserInformation() {
        return this.userInformation;
    }

    @Override
    public void update(String bidderId, double amount){}
}

