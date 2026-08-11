package AUCTIONCODE.UI.Controller.auth;

import AUCTIONCODE.UI.Main;
import AUCTIONCODE.UI.FxmlResources;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.Optional;

public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button LoginButton;

    @FXML
    private Hyperlink registerLink;

    @FXML
    private void initialize() {
        LoginButton.setDefaultButton(true);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            showError("Thiếu thông tin", "Vui lòng nhập tên đăng nhập và mật khẩu.");
            return;
        }

        try {
            ensureConnected();
            boolean isSuccess = loginWithFallback(username, password);
            if (isSuccess) {
                System.out.println("Login success. Session ID: " + Main.client.getSessionId());
                goToRoleHome(event);
            } else {
                showError("Đăng nhập thất bại", "Sai mật khẩu hoặc tên đăng nhập.");
            }
        } catch (Exception e) {
            showError("Không kết nối được server", "Hãy chạy Auction_Server trước rồi mới mở giao diện client.");
            e.printStackTrace();
        }
    }

    private void ensureConnected() throws Exception {
        if (!Main.client.isConnected()) {
            Main.client.connect();
        }
    }

    private boolean loginWithFallback(String username, String password) {
        if (Main.client.login(username, password)) {
            return true;
        }
        if (isDemoAccount(username, password)) {
            Main.client.register("test", "1234", "Test User", "0123456789", "test@example.com", "Ha Noi");
            return Main.client.login("test", "1234");
        }
        if (isDefaultAdmin(username, password)) {
            Main.client.registerAdmin("admin", "1234", "Administrator", "0987654321", "admin@example.com", "", "BIZ-001");
            return Main.client.login("admin", "1234");
        }
        return false;
    }

    private boolean isDemoAccount(String username, String password) {
        return "test".equals(username) && "1234".equals(password);
    }

    private boolean isDefaultAdmin(String username, String password) {
        return "admin".equals(username) && "1234".equals(password);
    }

    @FXML
    private void GoToMain(ActionEvent event) {
        switchScene(event, "/AUCTIONCODE/UI/view/auth/Main.fxml", "Không mở được màn hình chính");
    }

    private void goToRoleHome(ActionEvent event) {
        String profile = Main.client.getProfile();
        if (profile != null) {
            String[] parts = profile.split("\\|", -1);
            if (parts.length >= 6 && "Admin".equalsIgnoreCase(parts[5])) {
                switchScene(event, "/AUCTIONCODE/UI/view/auth/Admin.fxml", "Không mở được màn hình admin");
                return;
            }
        }
        GoToMain(event);
    }

    private void switchScene(ActionEvent event, String fxml, String errorHeader) {
        try {
            FXMLLoader loader = FxmlResources.loader(fxml);
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showError(errorHeader, e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRegister(ActionEvent event) {
        try {
            FXMLLoader loader = FxmlResources.loader("/AUCTIONCODE/UI/view/auth/Register.fxml");
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng ký");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword() {
        Dialog<ResetPasswordInput> dialog = new Dialog<>();
        dialog.setTitle("Quên mật khẩu");
        dialog.setHeaderText("Đặt lại mật khẩu bằng email đã đăng ký");

        TextField usernameInput = new TextField();
        usernameInput.setPromptText("Tên đăng nhập");
        TextField emailInput = new TextField();
        emailInput.setPromptText("Email");
        PasswordField newPasswordInput = new PasswordField();
        newPasswordInput.setPromptText("Mật khẩu mới");
        PasswordField confirmPasswordInput = new PasswordField();
        confirmPasswordInput.setPromptText("Nhập lại mật khẩu mới");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Username"), 0, 0);
        grid.add(usernameInput, 1, 0);
        grid.add(new Label("Email"), 0, 1);
        grid.add(emailInput, 1, 1);
        grid.add(new Label("Mật khẩu mới"), 0, 2);
        grid.add(newPasswordInput, 1, 2);
        grid.add(new Label("Xác nhận"), 0, 3);
        grid.add(confirmPasswordInput, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new ResetPasswordInput(
                        usernameInput.getText().trim(),
                        emailInput.getText().trim(),
                        newPasswordInput.getText(),
                        confirmPasswordInput.getText()
                );
            }
            return null;
        });

        Optional<ResetPasswordInput> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        ResetPasswordInput input = result.get();
        if (input.username().isBlank() || input.email().isBlank()
                || input.newPassword().isBlank() || input.confirmPassword().isBlank()) {
            showError("Thiếu thông tin", "Vui lòng nhập đầy đủ username, email và mật khẩu mới.");
            return;
        }
        if (!input.newPassword().equals(input.confirmPassword())) {
            showError("Mật khẩu không khớp", "Mật khẩu mới và phần xác nhận phải giống nhau.");
            return;
        }
        if (input.newPassword().length() < 4) {
            showError("Mật khẩu quá ngắn", "Mật khẩu mới cần ít nhất 4 ký tự.");
            return;
        }

        try {
            ensureConnected();
            if (Main.client.resetPassword(input.username(), input.email(), input.newPassword())) {
                showInfo("Đã đổi mật khẩu", "Bạn có thể đăng nhập bằng mật khẩu mới.");
            } else {
                showError("Không đổi được mật khẩu", "Username và email không khớp với tài khoản nào.");
            }
        } catch (Exception e) {
            showError("Không kết nối được server", "Hãy chạy Auction_Server trước rồi thử lại.");
            e.printStackTrace();
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private record ResetPasswordInput(String username, String email, String newPassword, String confirmPassword) {
    }
}
