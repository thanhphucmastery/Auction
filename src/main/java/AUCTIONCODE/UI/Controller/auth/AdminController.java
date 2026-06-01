package AUCTIONCODE.UI.Controller.auth;

import AUCTIONCODE.UI.FxmlResources;
import AUCTIONCODE.UI.Main;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminController {
    @FXML private Label adminNameLabel;
    @FXML private Label userCountLabel;
    @FXML private Label auctionCountLabel;
    @FXML private TableView<UserRow> userTable;
    @FXML private TableColumn<UserRow, String> colUserName;
    @FXML private TableColumn<UserRow, String> colFullName;
    @FXML private TableColumn<UserRow, String> colRole;
    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, String> colAuctionName;
    @FXML private TableColumn<AuctionRow, String> colSeller;
    @FXML private TableColumn<AuctionRow, String> colAuctionPrice;
    @FXML private TableColumn<AuctionRow, String> colAuctionTime;
    @FXML private TableColumn<AuctionRow, String> colAuctionStatus;
    @FXML private Button refreshButton;
    @FXML private Button extendButton;
    @FXML private Button endButton;
    @FXML private Button deleteAuctionButton;
    @FXML private Button deleteUserButton;
    @FXML private Spinner<Integer> extendMinutesSpinner;

    @FXML
    private void initialize() {
        colUserName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().userName()));
        colUserName.setCellFactory(column -> new TableCell<>() {
            private final Hyperlink link = new Hyperlink();

            {
                link.setOnAction(event -> {
                    UserRow row = getTableRow() == null ? null : getTableRow().getItem();
                    if (row != null) {
                        showUserDetail(row);
                    }
                });
                link.getStyleClass().add("table-link");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    link.setText(item);
                    setGraphic(link);
                }
                setText(null);
            }
        });
        colFullName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fullName()));
        colRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().role()));

        colAuctionName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().itemName()
                + " (" + shortId(data.getValue().id()) + ")"));
        colSeller.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().sellerLabel()));
        colAuctionPrice.setCellValueFactory(data -> new SimpleStringProperty(formatMoney(data.getValue().currentPrice())));
        colAuctionTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().timeLeft()));
        colAuctionStatus.setCellValueFactory(data -> new SimpleStringProperty(displayStatus(data.getValue().status())));

        extendMinutesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1440, 20));
        extendMinutesSpinner.setEditable(true);
        loadAdminName();
        refreshData();
    }

    @FXML
    private void refreshData() {
        List<UserRow> users = parseUsers(Main.client.adminGetUsers());
        List<AuctionRow> auctions = parseAuctions(Main.client.getAuctions());
        userTable.setItems(FXCollections.observableArrayList(users));
        auctionTable.setItems(FXCollections.observableArrayList(auctions));
        userCountLabel.setText(users.size() + " tài khoản");
        auctionCountLabel.setText(auctions.size() + " phiên đấu giá");
    }

    @FXML
    private void extendSelectedAuction() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Chưa chọn phiên", "Chọn một phiên đấu giá trước khi gia hạn.");
            return;
        }
        int minutes = extendMinutesSpinner.getValue();
        if (Main.client.adminExtendAuction(selected.id(), minutes)) {
            refreshData();
            showInfo("Đã gia hạn", "Phiên đã được gia hạn thêm " + minutes + " phút.");
        } else {
            showError("Gia hạn thất bại", "Server không chấp nhận yêu cầu gia hạn.");
        }
    }

    @FXML
    private void endSelectedAuction() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Chưa chọn phiên", "Chọn một phiên đấu giá trước khi kết thúc.");
            return;
        }
        if (!confirm("Kết thúc phiên?", "Phiên " + selected.itemName() + " sẽ được cập nhật trạng thái kết thúc.")) {
            return;
        }
        if (Main.client.adminEndAuction(selected.id())) {
            refreshData();
            showInfo("Đã kết thúc", "Phiên đã được cập nhật trạng thái.");
        } else {
            showError("Kết thúc thất bại", "Server không chấp nhận yêu cầu kết thúc phiên.");
        }
    }

    @FXML
    private void deleteSelectedAuction() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Chưa chọn phiên", "Chọn một phiên đấu giá trước khi xóa.");
            return;
        }
        if (!confirm("Xóa phiên đấu giá?", "Phiên " + selected.itemName() + " và lịch sử đặt giá liên quan sẽ bị xóa.")) {
            return;
        }
        if (Main.client.adminDeleteAuction(selected.id())) {
            refreshData();
            showInfo("Đã xóa", "Phiên đấu giá đã được xóa.");
        } else {
            showError("Xóa thất bại", "Server không chấp nhận yêu cầu xóa phiên.");
        }
    }

    @FXML
    private void deleteSelectedUser() {
        UserRow selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Chưa chọn tài khoản", "Chọn một tài khoản trước khi xóa.");
            return;
        }
        if (!confirm("Xóa tài khoản?", "Tài khoản " + selected.userName() + " sẽ bị xóa khỏi hệ thống.")) {
            return;
        }
        if (Main.client.adminDeleteUser(selected.id())) {
            refreshData();
            showInfo("Đã xóa", "Tài khoản đã được xóa.");
        } else {
            showError("Xóa thất bại", "Không thể xóa tài khoản này.");
        }
    }

    @FXML
    private void logout() {
        Main.client.logout();
        try {
            FXMLLoader loader = FxmlResources.loader("/AUCTIONCODE/UI/view/auth/Login.fxml");
            Parent root = loader.load();
            Stage stage = (Stage) userTable.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showError("Không đăng xuất được", e.getMessage());
        }
    }

    private void loadAdminName() {
        String profile = Main.client.getProfile();
        if (profile == null || profile.isBlank()) {
            adminNameLabel.setText("Admin");
            return;
        }
        String[] parts = profile.split("\\|", -1);
        adminNameLabel.setText(parts.length >= 2 && !parts[1].isBlank() ? parts[1] : parts[0]);
    }

    private List<UserRow> parseUsers(String data) {
        List<UserRow> rows = new ArrayList<>();
        if (data == null || data.isBlank()) {
            return rows;
        }
        for (String raw : data.split(";", -1)) {
            String[] parts = raw.split("\\|", -1);
            if (parts.length >= 5) {
                rows.add(new UserRow(
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[3],
                        parseDouble(parts[4]),
                        parts.length > 5 ? parts[5] : "",
                        parts.length > 6 ? parts[6] : "",
                        parts.length > 7 ? parts[7] : ""
                ));
            }
        }
        return rows;
    }

    private List<AuctionRow> parseAuctions(String data) {
        List<AuctionRow> rows = new ArrayList<>();
        if (data == null || data.isBlank()) {
            return rows;
        }
        for (String raw : data.split(";", -1)) {
            String[] parts = raw.split("\\|", -1);
            if (parts.length >= 5) {
                rows.add(new AuctionRow(
                        parts[0],
                        parts[4],
                        parts[1],
                        parseDouble(parts[2]),
                        parts[3],
                        parts.length > 5 ? parts[5] : "",
                        parts.length > 6 ? parts[6] : ""
                ));
            }
        }
        return rows;
    }

    private void showUserDetail(UserRow user) {
        String detail = """
                Username: %s
                Họ tên: %s
                Role: %s
                Email: %s
                Số điện thoại: %s
                Địa chỉ: %s
                Số dư: %s
                """.formatted(
                user.userName(),
                blank(user.fullName()),
                user.role(),
                blank(user.email()),
                blank(user.phone()),
                blank(user.address()),
                formatMoney(user.balance())
        );
        showInfo("Thông tin tài khoản", detail);
    }

    private static String displayStatus(String status) {
        return switch (status) {
            case "ONGOING" -> "Đang diễn ra";
            case "UPCOMING" -> "Sắp mở";
            case "EXTENDED" -> "Đã gia hạn";
            case "SUCCESSFUL" -> "Thành công";
            case "NO_BIDS" -> "Không có giá";
            case "CANCELED" -> "Đã hủy";
            default -> status;
        };
    }

    private static String formatMoney(double value) {
        return String.format("%,.0f VND", value);
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String shortId(String id) {
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private static String timeLeft(String endTime) {
        try {
            Duration duration = Duration.between(LocalDateTime.now(), LocalDateTime.parse(endTime));
            long minutes = duration.toMinutes();
            if (minutes <= 0) {
                return "Đã kết thúc";
            }
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return hours > 0 ? hours + "h " + remainingMinutes + "m" : remainingMinutes + " phút";
        } catch (Exception e) {
            return "Không rõ";
        }
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "Chưa cập nhật" : value;
    }

    private boolean confirm(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(header);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(header);
        alert.setContentText(content == null ? "" : content);
        alert.showAndWait();
    }

    private void showInfo(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(header);
        alert.setContentText(content == null ? "" : content);
        alert.showAndWait();
    }

    public record UserRow(String id, String userName, String fullName, String role,
                          double balance, String email, String phone, String address) {
    }

    public record AuctionRow(String id, String itemName, String status, double currentPrice,
                             String endTime, String sellerUserName, String sellerFullName) {
        String timeLeft() {
            return AdminController.timeLeft(endTime);
        }

        String sellerLabel() {
            if ((sellerUserName == null || sellerUserName.isBlank())
                    && (sellerFullName == null || sellerFullName.isBlank())) {
                return "Không rõ";
            }
            if (sellerFullName == null || sellerFullName.isBlank()) {
                return sellerUserName;
            }
            if (sellerUserName == null || sellerUserName.isBlank()) {
                return sellerFullName;
            }
            return sellerUserName + " - " + sellerFullName;
        }
    }
}
