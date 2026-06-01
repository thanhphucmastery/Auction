package AUCTIONCODE.UI.Controller.auth;

import AUCTIONCODE.UI.Main;
import AUCTIONCODE.UI.FxmlResources;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ProfileController {
    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;

    @FXML
    private void initialize() {
        loadProfile();
    }

    private void loadProfile() {
        String profile = Main.client.getProfile();
        if (profile == null || profile.isBlank()) {
            usernameLabel.setText("--");
            roleLabel.setText("--");
            return;
        }

        String[] parts = profile.split("\\|", -1);
        if (parts.length >= 7) {
            usernameLabel.setText(parts[0]);
            fullNameField.setText(parts[1]);
            phoneField.setText(parts[2]);
            emailField.setText(parts[3]);
            addressField.setText(parts[4]);
            roleLabel.setText(parts[5]);
        }
    }

    @FXML
    private void SaveProfile(ActionEvent event) {
        String fullName = fullNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String address = addressField.getText().trim();

        if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty()) {
            showError("Thiếu thông tin", "Vui lòng nhập đầy đủ họ tên, số điện thoại, email và địa chỉ.");
            return;
        }
        if (!phone.matches("\\d{10}")) {
            showError("Số điện thoại không hợp lệ", "Số điện thoại phải có đúng 10 chữ số.");
            return;
        }

        if (Main.client.updateProfile(fullName, phone, email, address)) {
            showInfo("Đã lưu", "Thông tin hồ sơ đã được cập nhật.");
            BackToMain(event);
        } else {
            showError("Không lưu được", "Server không chấp nhận yêu cầu cập nhật hồ sơ.");
        }
    }

    @FXML
    private void BackToMain(ActionEvent event) {
        try {
            FXMLLoader loader = FxmlResources.loader("/AUCTIONCODE/UI/view/auth/Main.fxml");
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showError("Không mở được trang chủ", e.getMessage());
        }
    }

    private void showInfo(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(header);
        alert.setContentText(content == null ? "" : content);
        alert.showAndWait();
    }
}
