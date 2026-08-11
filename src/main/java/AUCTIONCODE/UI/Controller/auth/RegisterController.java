package AUCTIONCODE.UI.Controller.auth;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import AUCTIONCODE.UI.Main;
import AUCTIONCODE.UI.FxmlResources;

public class RegisterController {
    // --- Kết nối với các field trong FXML ---
    @FXML private TextField UsernameField;
    @FXML private PasswordField PasswordField;
    @FXML private PasswordField ConfirmPasswordField; // Ô mới thêm vào đây
    @FXML private TextField FullNameField;
    @FXML private TextField PhoneField;
    @FXML private TextField EmailField;
    @FXML private TextField AddressField;
    @FXML private Label ErrorLabel;
    @FXML private TextField BusinessCodeField;

    // --- Xử lý nút Register ---
    @FXML
    private void handleRegister(ActionEvent event) {
        // Lấy dữ liệu từ các field
        String username = UsernameField.getText().trim();
        String password = PasswordField.getText();
        String confirmPassword = ConfirmPasswordField.getText(); // Lấy dữ liệu ô xác nhận
        String fullName = FullNameField.getText().trim();
        String phone = PhoneField.getText().trim();
        String email = EmailField.getText().trim();
        String address = AddressField.getText().trim();
        String businessCode = BusinessCodeField.getText().trim();
        // Kiểm tra hai mật khẩu có trùng khớp nhau không (Client-side validation)
        if (!password.equals(confirmPassword)) {
            ErrorLabel.setText("Mật khẩu xác nhận không trùng khớp!");
            return; // Dừng xử lý, không gửi lên server nữa
        }

        try {
            String response;
            if (businessCode.isEmpty()) {
                response = Main.client.sendRaw("REGISTER:" + username + ":" + password + ":"
                        + fullName + ":" + phone + ":" + email + ":" + address);
            } else {
                response = Main.client.sendRaw("REGISTER_ADMIN:" + username + ":" + password + ":"
                        + fullName + ":" + phone + ":" + email + ":" + address + ":" + businessCode);
            }
            // Phân tích kết quả trả về
            if (response != null && response.startsWith("OK")) {
                goToLogin(event);
            } else if (response != null) {
                String errorMessage = response.replace("ERROR:", "");
                ErrorLabel.setText(errorMessage);
            }
        } catch (Exception e) {
            ErrorLabel.setText("Lỗi kết nối mạng đến Server!");
            e.printStackTrace();
        }
    }

    @FXML
    private void goToLogin(ActionEvent event){
        try {
            FXMLLoader loader = FxmlResources.loader("/AUCTIONCODE/UI/view/auth/Login.fxml");
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng nhập");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
