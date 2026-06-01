package AUCTIONCODE.UI.Controller.auth;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.Node;
import AUCTIONCODE.UI.Main;
import AUCTIONCODE.UI.FxmlResources;

public class NapTienController {

    // 🎯 THÊM BIẾN TĨNH NÀY: Để lưu lại số dư đã nạp thành công hoặc số dư cũ truyền sang
    public static String latestBalance = "0.0";

    @FXML
    private TextField amount;
    @FXML
    private Button XacNhanNapTienButton;
    @FXML
    private Button BackButton;

    @FXML
    private void handleDeposit(ActionEvent event) {
        try {
            // 1. Lấy chuỗi chữ người dùng nhập và xóa khoảng trắng thừa
            String moneyText = amount.getText().trim();

            // 2. Chuyển chuỗi thành số thực double
            double moneyAmount = Double.parseDouble(moneyText);

            // 3. Tự bắn lệnh bằng sendRaw để lấy phản hồi chứa tiền từ Server
            String response = Main.client.sendRaw("DEPOSIT:" + Main.client.getSessionId() + ":" + moneyAmount);

            // 4. Kiểm tra nếu thành công (Server trả về chuỗi bắt đầu bằng OK)
            if (response != null && response.startsWith("OK")) {
                // Cắt bỏ "OK:" để lấy số dư thực tế (Ví dụ: "750000")
                String newBalance = response.substring(3);
                System.out.println("Nạp tiền thành công: " + moneyAmount + " | Số dư mới: " + newBalance);

                // 🎯 Cập nhật số dư mới nhất vào biến tĩnh trước khi chuyển cảnh
                latestBalance = newBalance;

                BackToMain(event, newBalance);
            } else {
                System.out.println("Nạp tiền thất bại từ phía Server.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Vui lòng chỉ nhập số, không nhập chữ hay ký tự đặc biệt!");
        }
    }

    // 🎯 HÀM BACKMAIN CHUẨN (Chỉ nhận 1 tham số ActionEvent để khớp 100% với file FXML của bạn)
    @FXML
    private void BackMain(ActionEvent event) {
        try {
            // Sửa đường dẫn dài đầy đủ theo cấu trúc của bạn
            FXMLLoader loader = FxmlResources.loader("/AUCTIONCODE/UI/view/auth/Main.fxml");
            Parent root = loader.load();

            // 🎯 Lấy controller màn hình chính và nạp số tiền cũ (được lưu trong biến tĩnh) sang
            MainController mainController = loader.getController();
            mainController.updateBudget(latestBalance);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hàm phụ trợ dùng riêng cho luồng nạp tiền thành công
    private void BackToMain(ActionEvent event, String newBalance) {
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
}