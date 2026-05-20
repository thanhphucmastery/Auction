package Model.User;

public class UserInformation {
    private String address;
    private String phoneNumber;
    private String email;
    private String fullName;
    public UserInformation(String address, String phoneNumber, String email, String fullName){

        if (phoneNumber == null || !phoneNumber.matches("^\\d{10}$")) {
            // Nếu sai thì báo lỗi và DỪNG việc tạo object lại
            throw new IllegalArgumentException("Error: A phone number must have 10 digits");
        }

        this.address=address;
        this.phoneNumber=phoneNumber;
        this.email=email;
        this.fullName=fullName;
    }

    public String getAddress(){
        return this.address;
    }

    public String getPhoneNumber(){
        return this.phoneNumber;
    }

    public String getEmail(){
        return this.email;
    }

    public String getFullName() {return  this.fullName;}

    public UserInformation clone() {
        return new UserInformation(address, phoneNumber, email, fullName);
    }

}

