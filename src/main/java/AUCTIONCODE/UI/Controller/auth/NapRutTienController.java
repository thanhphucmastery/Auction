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
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class NapRutTienController {
    public static String latestBalance = "0.0";

    @FXML
    private TextField amount;
    @FXML
    private Button XacNhanNapTienButton;
    @FXML
    private Button XacNhanRutTienButton;
    @FXML
    private Button BackButton;

    @FXML
    private void quick500k() {
        amount.setText("500000");
    }

    @FXML
    private void quick1m() {
        amount.setText("1000000");
    }

    @FXML
    private void quick5m() {
        amount.setText("5000000");
    }

    @FXML
    private void quick10m() {
        amount.setText("10000000");
    }

    @FXML
    private void handleDeposit(ActionEvent event) {
        try {
            double moneyAmount = parseAmount();
            String response = Main.client.sendRaw("DEPOSIT:" + Main.client.getSessionId() + ":" + moneyAmount);

            if (response != null && response.startsWith("OK:")) {
                String newBalance = response.substring(3);
                latestBalance = newBalance;
                showInfo("Thành công", "Nạp tiền thành công. Số dư mới: " + newBalance + " VNĐ");
                backToMain(event, newBalance);
            } else {
                showError("Không thể nạp tiền", response);
            }
        } catch (NumberFormatException e) {
            showError("Số tiền không hợp lệ", "Vui lòng nhập số tiền hợp lệ.");
        }
    }

    @FXML
    private void handleWithdraw(ActionEvent event) {
        try {
            double moneyAmount = parseAmount();
            String response = Main.client.sendRaw("WITHDRAW:" + Main.client.getSessionId() + ":" + moneyAmount);

            if (response != null && response.startsWith("OK:")) {
                String newBalance = response.substring(3);
                latestBalance = newBalance;
                showInfo("Thành công", "Rút tiền thành công. Số dư mới: " + newBalance + " VNĐ");
                backToMain(event, newBalance);
            } else {
                showError("Không thể rút tiền", response);
            }
        } catch (NumberFormatException e) {
            showError("Số tiền không hợp lệ", "Vui lòng nhập số tiền hợp lệ.");
        }
    }

    @FXML
    private void BackMain(ActionEvent event) {
        backToMain(event, latestBalance);
    }

    private double parseAmount() {
        String moneyText = amount.getText()
                .trim()
                .replace(",", "")
                .replace(".", "")
                .replace(" ", "");

        if (moneyText.isEmpty()) {
            throw new NumberFormatException("Amount is empty");
        }
        return Double.parseDouble(moneyText);
    }

    private void backToMain(ActionEvent event, String newBalance) {
        try {
            FXMLLoader loader = FxmlResources.loader("/AUCTIONCODE/UI/view/auth/Main.fxml");
            Parent root = loader.load();

            MainController mainController = loader.getController();
            mainController.updateBudget(newBalance);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(header);
        alert.setContentText(content != null ? content : "Không có phản hồi từ server");
        alert.showAndWait();
    }
}
