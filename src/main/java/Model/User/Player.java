package Model.User;

public class Player extends User {
    private double balance;
    public Player(String userName, UserInformation userInformation, String hashpassword,String userId, double balance){
        super(userName,userInformation,hashpassword,userId);
        this.balance=balance;
    }
    public void setPlayerBalance(double newBalance) {
        if (newBalance < 0) throw new IllegalArgumentException("Số dư không được âm");
        this.balance = newBalance;
    }
    public double getPlayerBalance(){
        return this.balance;
    }
    @Override
    public User deepCopy(){
        return new Player(
                this.getUserName(),
                this.getUserInformation().clone(),
                this.getUserPassword(),
                this.getUserId(),
                this.balance
        );
    }

    @Override
    public void update(String bidderId, double amount){
        System.out.println("Người chơi với ID" + bidderId + "đặt 1 giá mới" + amount);
    }
}
