package AUCTIONCODE.Model.User;

public class Admin extends User {
    private String businessCode;

    public Admin(String userName, UserInformation userInformation, String hashpassword,String userId, String businessCode,String role){
        super(userName,userInformation,hashpassword,userId,role);
        this.businessCode=businessCode;
    }

    public String getBusinessCode() {
        return businessCode;
    }
}
