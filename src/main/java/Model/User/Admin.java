package Model.User;

public class Admin extends User {
    private String businessCode;

    public Admin(String userName, UserInformation userInformation, String hashpassword,String userId, String businessCode){
        super(userName,userInformation,hashpassword,userId);
        this.businessCode=businessCode;
    }

    @Override
    public User deepCopy(){
        return new Admin(
                this.getUserName(),
                this.getUserInformation().clone(),
                this.getUserPassword(),
                this.getUserId(),
                this.businessCode
        );
    }
}
