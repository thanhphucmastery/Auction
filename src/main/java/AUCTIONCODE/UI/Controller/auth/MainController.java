package AUCTIONCODE.UI.Controller.auth;

import AUCTIONCODE.UI.Main;
import AUCTIONCODE.UI.FxmlResources;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainController {
    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, String> colName;
    @FXML private TableColumn<AuctionRow, String> colCurrentPrice;
    @FXML private TableColumn<AuctionRow, String> colTimeLeft;
    @FXML private TableColumn<AuctionRow, String> colStatus;
    @FXML private TableColumn<AuctionRow, String> colAction;
    @FXML private Label Budget;
    @FXML private Button logoutbutton;
    @FXML private FlowPane ItemContainer;
    @FXML private Button additembutton;
    @FXML private Button NapTienButton;
    @FXML private Button CreateBidButton;
    @FXML private Button WareHouseButton;
    @FXML private Button ProfileButton;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> sortBox;
    @FXML private Label clockLabel;
    @FXML private Label userNameLabel;
    @FXML private Label walletLabel;

    private final ObservableList<AuctionRow> allAuctions = FXCollections.observableArrayList();
    private javafx.animation.Timeline clockTimeline;
    private javafx.animation.Timeline auctionRefreshTimeline;

    public void updateBudget(String newBalance) {
        if (Budget != null) {
            Budget.setText("Ví: " + newBalance + " VND");
        }
        if (walletLabel != null) {
            walletLabel.setText(formatMoney(parseDouble(newBalance)));
        }
    }

    @FXML
    public void initialize() {
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().displayName()));
        colCurrentPrice.setCellValueFactory(data -> new SimpleStringProperty(formatMoney(data.getValue().currentPrice())));
        colTimeLeft.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().timeWindow()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(displayStatus(data.getValue().status())));
        setupActionColumn();
        setupFilters();
        loadProfileSummary();
        startClock();
        refreshAuctions();
        startAuctionAutoRefresh();
    }

    private void setupActionColumn() {
        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("Vào phòng");

            {
                button.getStyleClass().add("primary-button");
                button.setOnAction(event -> {
                    AuctionRow row = getTableView().getItems().get(getIndex());
                    openBidRoom(row, event);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                AuctionRow row = getTableView().getItems().get(getIndex());
                button.setText(isClosedStatus(row.status()) ? "Xem lại" : "Vào phòng");
                setGraphic(button);
            }
        });
    }

    private void setupFilters() {
        statusFilter.getItems().setAll("Tất cả", "Đang diễn ra", "Sắp mở", "Đã gia hạn", "Thành công", "Không có giá", "Đã hủy");
        statusFilter.getSelectionModel().select("Tất cả");
        sortBox.getItems().setAll("Mặc định", "Giá thấp đến cao", "Giá cao đến thấp", "Sắp hết giờ");
        sortBox.getSelectionModel().select("Mặc định");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        sortBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void startClock() {
        updateClock();
        clockTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), event -> updateClock())
        );
        clockTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clockTimeline.play();
    }

    private void updateClock() {
        clockLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private void startAuctionAutoRefresh() {
        auctionRefreshTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), event -> refreshAuctions())
        );
        auctionRefreshTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        auctionRefreshTimeline.play();
    }

    private void loadProfileSummary() {
        String profile = Main.client.getProfile();
        if (profile == null || profile.isBlank()) {
            userNameLabel.setText("Người dùng");
            return;
        }
        String[] parts = profile.split("\\|", -1);
        if (parts.length >= 7) {
            userNameLabel.setText(parts[1].isBlank() ? parts[0] : parts[1]);
            updateBudget(parts[6]);
        }
    }

    @FXML
    private void refreshAuctions() {
        try {
            String data = Main.client.getAuctions();
            allAuctions.setAll(parseAuctions(data));
            applyFilters();
        } catch (Exception e) {
            showError("Không tải được danh sách phiên", e.getMessage());
        }
    }

    private List<AuctionRow> parseAuctions(String data) {
        List<AuctionRow> rows = new ArrayList<>();
        if (data == null || data.trim().isEmpty()) {
            return rows;
        }

        for (String rawRoom : data.split(";")) {
            String[] details = rawRoom.split("\\|", -1);
            if (details.length >= 4) {
                String id = details[0];
                String status = details[1];
                double currentPrice = parseDouble(details[2]);
                String endTime = details[3];
                String itemName = details.length >= 5 && !details[4].isBlank() ? details[4] : "Phiên " + shortId(id);
                rows.add(new AuctionRow(id, itemName, status, currentPrice, endTime));
            }
        }
        return rows;
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedStatus = toServerStatus(statusFilter.getValue());
        String selectedSort = sortBox.getValue();

        List<AuctionRow> filtered = allAuctions.stream()
                .filter(row -> keyword.isEmpty()
                        || row.id().toLowerCase().contains(keyword)
                        || row.itemName().toLowerCase().contains(keyword))
                .filter(row -> selectedStatus == null || "ALL".equals(selectedStatus) || row.status().equals(selectedStatus))
                .toList();

        List<AuctionRow> sorted = new ArrayList<>(filtered);
        if ("Giá thấp đến cao".equals(selectedSort)) {
            sorted.sort(Comparator.comparingDouble(AuctionRow::currentPrice));
        } else if ("Giá cao đến thấp".equals(selectedSort)) {
            sorted.sort(Comparator.comparingDouble(AuctionRow::currentPrice).reversed());
        } else if ("Sắp hết giờ".equals(selectedSort)) {
            sorted.sort(Comparator.comparing(AuctionRow::endTimeValue));
        }
        auctionTable.setItems(FXCollections.observableArrayList(sorted));
    }

    private void openBidRoom(AuctionRow row, ActionEvent event) {
        try {
            if (!isClosedStatus(row.status())) {
                String joinResponse = Main.client.sendRaw("JOIN_AUCTION:" + Main.client.getSessionId() + ":" + row.id());
                if (joinResponse == null || !joinResponse.startsWith("OK")) {
                    if (!isEndedError(joinResponse)) {
                        showError("Không vào được phòng", translateServerError(joinResponse));
                        return;
                    }
                }
            }
            stopTimers();
            FXMLLoader loader = FxmlResources.loader("/AUCTIONCODE/UI/view/auth/phongdaugia.fxml");
            Parent root = loader.load();
            BidRoomController controller = loader.getController();
            controller.setAuction(row.id(), row.itemName(), row.currentPrice(), row.timeLeft());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showError("Không vào được phòng", e.getMessage());
        }
    }

    private static boolean isClosedStatus(String status) {
        return "SUCCESSFUL".equals(status) || "NO_BIDS".equals(status) || "CANCELED".equals(status);
    }

    private static boolean isEndedError(String response) {
        return response != null && response.contains("đã kết thúc");
    }

    @FXML
    private void BackToLogin(ActionEvent event) {
        switchScene(event, "/AUCTIONCODE/UI/view/auth/Login.fxml");
    }

    @FXML
    private void NapTien(ActionEvent event) {
        switchScene(event, "/AUCTIONCODE/UI/view/auth/Vitien.fxml");
    }

    @FXML
    private void CreateBid(ActionEvent event) {
        switchScene(event, "/AUCTIONCODE/UI/view/auth/CreateBid.fxml");
    }

    @FXML
    private void AddItem(ActionEvent event) {
        switchScene(event, "/AUCTIONCODE/UI/view/auth/AddItem.fxml");
    }

    @FXML
    private void OpenWareHouse(ActionEvent event) {
        switchScene(event, "/AUCTIONCODE/UI/view/auth/WareHouse.fxml");
    }

    @FXML
    private void OpenProfile(ActionEvent event) {
        switchScene(event, "/AUCTIONCODE/UI/view/auth/Profile.fxml");
    }

    @FXML
    private void OpenTransactionHistory(ActionEvent event) {
        switchScene(event, "/AUCTIONCODE/UI/view/auth/TransactionHistory.fxml");
    }

    private void switchScene(ActionEvent event, String fxml) {
        try {
            stopTimers();
            FXMLLoader loader = FxmlResources.loader(fxml);
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showError("Không mở được màn hình", e.getMessage());
        }
    }

    private String toServerStatus(String displayStatus) {
        if (displayStatus == null || "Tất cả".equals(displayStatus)) return "ALL";
        return switch (displayStatus) {
            case "Đang diễn ra" -> "ONGOING";
            case "Sắp mở" -> "UPCOMING";
            case "Đã gia hạn" -> "EXTENDED";
            case "Thành công" -> "SUCCESSFUL";
            case "Không có giá" -> "NO_BIDS";
            case "Đã hủy" -> "CANCELED";
            default -> displayStatus;
        };
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

    private void stopTimers() {
        if (clockTimeline != null) {
            clockTimeline.stop();
            clockTimeline = null;
        }
        if (auctionRefreshTimeline != null) {
            auctionRefreshTimeline.stop();
            auctionRefreshTimeline = null;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String formatMoney(double value) {
        return String.format("%,.0f VND", value);
    }

    private static String shortId(String id) {
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private String translateServerError(String response) {
        if (response == null) {
            return "Không có phản hồi từ server.";
        }
        return response
                .replace("ERROR:", "")
                .replace("Invalid session", "Phiên đăng nhập đã hết hạn.");
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(header);
        alert.setContentText(content == null ? "" : content);
        alert.showAndWait();
    }

    public record AuctionRow(String id, String itemName, String status, double currentPrice, String endTime) {
        String displayName() {
            return itemName + " (" + shortId(id) + ")";
        }

        String timeLeft() {
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

        String timeWindow() {
            return "Đóng " + closeTime() + " | " + timeLeft();
        }

        String closeTime() {
            try {
                return LocalDateTime.parse(endTime).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            } catch (Exception e) {
                return "Không rõ";
            }
        }

        LocalDateTime endTimeValue() {
            try {
                return LocalDateTime.parse(endTime);
            } catch (Exception e) {
                return LocalDateTime.MAX;
            }
        }
    }
}
