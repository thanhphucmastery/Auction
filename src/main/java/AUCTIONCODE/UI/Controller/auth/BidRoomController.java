package AUCTIONCODE.UI.Controller.auth;

import AUCTIONCODE.UI.Main;
import AUCTIONCODE.UI.FxmlResources;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BidRoomController {
    @FXML private Button OutRoomButton;
    @FXML private Label auctionIdLabel;
    @FXML private Label titleLabel;
    @FXML private Label timerLabel;
    @FXML private Label priceLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label stepPriceLabel;
    @FXML private Label bidCountLabel;
    @FXML private Label resultNoticeLabel;
    @FXML private Label itemTypeLabel;
    @FXML private Label itemDescriptionLabel;
    @FXML private Label itemAttributesLabel;
    @FXML private Label sellerLabel;
    @FXML private Label statusLabel;
    @FXML private Label openTimeLabel;
    @FXML private Label closeTimeLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button bidButton;
    @FXML private ListView<String> historyList;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private ImageView itemImageView;
    @FXML private Label imagePlaceholderLabel;

    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private String auctionId;
    private String endTimeValue;
    private String statusValue;
    private double currentPrice;
    private double stepPrice;
    private Timeline refreshTimeline;
    private String lastImagePath = "";
    private String lastHistoryKey = "";
    private String lastChartKey = "";
    private boolean winnerNoticeShown;

    @FXML
    private void initialize() {
        priceChart.getData().clear();
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);
    }

    public void setAuction(String auctionId, String itemName, double currentPrice, String timeLeft) {
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        auctionIdLabel.setText("PHIÊN " + shortId(auctionId));
        titleLabel.setText(itemName);
        priceLabel.setText(formatMoney(currentPrice));
        timerLabel.setText(timeLeft);
        highestBidderLabel.setText("Chưa có người dẫn đầu");
        historyList.getItems().setAll("Đã vào phòng đấu giá " + shortId(auctionId));
        addFallbackChartPoint(currentPrice);
        refreshAuctionDetail();
        startAutoRefresh();
    }

    @FXML
    private void PlaceBid() {
        if (auctionId == null || auctionId.isBlank()) {
            showError("Chưa có phiên đấu giá", "Hãy quay lại trang chủ và vào phòng từ danh sách phiên.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(bidAmountField.getText().trim().replace(",", ""));
        } catch (NumberFormatException e) {
            showError("Giá không hợp lệ", "Vui lòng nhập số tiền bằng số.");
            return;
        }

        double minimumBid = currentPrice + stepPrice;
        if (amount < minimumBid) {
            showError("Giá quá thấp", "Giá mới phải tối thiểu " + formatMoney(minimumBid) + ".");
            return;
        }

        String response = Main.client.sendRaw("BID:" + Main.client.getSessionId() + ":" + auctionId + ":" + amount);
        if (response != null && response.startsWith("OK")) {
            bidAmountField.clear();
            refreshAuctionDetail();
        } else {
            showError("Đặt giá thất bại", translateServerError(response));
        }
    }

    @FXML
    private void OutRoom(ActionEvent event) {
        stopAutoRefresh();
        try {
            FXMLLoader loader = FxmlResources.loader("/AUCTIONCODE/UI/view/auth/Main.fxml");
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showError("Không quay về được trang chủ", e.getMessage());
        }
    }

    private void refreshAuctionDetail() {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }

        String detail = Main.client.getAuctionDetail(auctionId);
        if (detail == null || detail.isBlank()) {
            return;
        }

        AuctionDetail auction = AuctionDetail.parse(detail);
        currentPrice = auction.currentPrice();
        stepPrice = auction.stepPrice();
        endTimeValue = auction.endTime();
        statusValue = auction.status();
        titleLabel.setText(auction.itemName().isBlank() ? titleLabel.getText() : auction.itemName());
        updateItemImage(auction.imagePath());
        priceLabel.setText(formatMoney(currentPrice));
        updateTimerLabel();
        stepPriceLabel.setText("Bước giá: " + formatMoney(stepPrice));
        bidCountLabel.setText(auction.bids().size() + " lượt đặt giá");
        itemTypeLabel.setText("Loại: " + displayItemType(auction.itemType()));
        itemDescriptionLabel.setText(auction.itemDescription().isBlank() ? "Chưa có mô tả." : auction.itemDescription());
        itemAttributesLabel.setText("Thông số: " + (auction.itemAttributes().isBlank() ? "--" : auction.itemAttributes()));
        sellerLabel.setText("Người bán: " + (auction.sellerName().isBlank() ? "--" : auction.sellerName()));
        statusLabel.setText("Trạng thái: " + displayStatus(auction.status()));
        updateBidControls(auction.status());
        updateResultNotice(auction);
        showWinnerNoticeIfNeeded(auction);
        openTimeLabel.setText("Mở: " + auction.openTimeText());
        closeTimeLabel.setText("Đóng: " + formatDateTime(auction.endTime()));
        highestBidderLabel.setText(auction.highestBidderName().isBlank()
                ? "Chưa có người dẫn đầu"
                : "Dẫn đầu: " + auction.highestBidderName());

        List<String> historyRows = new ArrayList<>();
        auction.bids().stream()
                .sorted(Comparator.comparing(BidRow::timestamp).reversed())
                .forEach(bid -> historyRows.add(formatTime(bid.timestamp()) + " - "
                        + bid.bidderName() + " đặt " + formatMoney(bid.amount())));
        if (historyRows.isEmpty()) {
            historyRows.add("Chưa có lượt đặt giá nào");
        }
        String historyKey = String.join("\n", historyRows);
        if (!historyKey.equals(lastHistoryKey)) {
            lastHistoryKey = historyKey;
            historyList.getItems().setAll(historyRows);
        }

        String chartKey = auction.bids().stream()
                .sorted(Comparator.comparing(BidRow::timestamp))
                .map(bid -> bid.timestamp() + "|" + bid.amount())
                .reduce("", (left, right) -> left + ";" + right);
        if (chartKey.equals(lastChartKey)) {
            return;
        }
        lastChartKey = chartKey;
        priceSeries.getData().clear();
        if (auction.bids().isEmpty()) {
            addFallbackChartPoint(currentPrice);
            return;
        }
        auction.bids().stream()
                .sorted(Comparator.comparing(BidRow::timestamp))
                .forEach(bid -> priceSeries.getData().add(
                        new XYChart.Data<>(formatTime(bid.timestamp()), bid.amount())));
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> refreshAuctionDetail()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
        }
    }

    private void addFallbackChartPoint(double value) {
        priceSeries.getData().clear();
        priceSeries.getData().add(new XYChart.Data<>(
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                value
        ));
    }

    private void updateItemImage(String imagePath) {
        String normalizedPath = imagePath == null ? "" : imagePath;
        if (normalizedPath.equals(lastImagePath)) {
            return;
        }
        lastImagePath = normalizedPath;
        if (imagePath != null && !imagePath.isBlank() && Files.exists(Path.of(imagePath))) {
            itemImageView.setImage(new Image(Path.of(imagePath).toUri().toString(), true));
            imagePlaceholderLabel.setVisible(false);
        } else {
            itemImageView.setImage(null);
            imagePlaceholderLabel.setVisible(true);
        }
    }

    private void updateTimerLabel() {
        if (endTimeValue == null || endTimeValue.isBlank()) {
            return;
        }
        try {
            LocalDateTime endTime = LocalDateTime.parse(endTimeValue);
            java.time.Duration remaining = java.time.Duration.between(LocalDateTime.now(), endTime);
            if (!remaining.isNegative() && !remaining.isZero()) {
                long totalSeconds = remaining.toSeconds();
                timerLabel.setText(String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60));
                return;
            }
        } catch (Exception ignored) {
        }
        timerLabel.setText("Đã kết thúc");
        if ("UPCOMING".equals(statusValue) || "ONGOING".equals(statusValue) || "EXTENDED".equals(statusValue)) {
            statusLabel.setText("Trạng thái: Đã kết thúc");
        }
    }

    private void updateBidControls(String status) {
        boolean active = "ONGOING".equals(status) || "EXTENDED".equals(status);
        bidAmountField.setDisable(!active);
        bidButton.setDisable(!active);
        if (!active) {
            bidAmountField.setPromptText("Phiên đã kết thúc");
        }
    }

    private void showWinnerNoticeIfNeeded(AuctionDetail auction) {
        if (winnerNoticeShown || !"SUCCESSFUL".equals(auction.status()) || !auction.currentUserWinner()) {
            return;
        }
        winnerNoticeShown = true;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chúc mừng");
        alert.setHeaderText("Bạn đã thắng phiên đấu giá");
        alert.setContentText("Bạn là người thắng vật phẩm \"" + auction.itemName() + "\" với giá "
                + formatMoney(auction.currentPrice()) + ".");
        alert.showAndWait();
    }

    private void updateResultNotice(AuctionDetail auction) {
        if ("SUCCESSFUL".equals(auction.status())) {
            resultNoticeLabel.setText(auction.currentUserWinner()
                    ? "Chúc mừng! Bạn đã thắng phiên đấu giá này."
                    : "Phiên đã kết thúc. Người thắng: " + blank(auction.highestBidderName()));
        } else if ("NO_BIDS".equals(auction.status())) {
            resultNoticeLabel.setText("Phiên đã kết thúc nhưng không có lượt đặt giá.");
        } else if ("CANCELED".equals(auction.status())) {
            resultNoticeLabel.setText("Phiên đã bị hủy. Tiền giữ giá đã được hoàn nếu có.");
        } else {
            resultNoticeLabel.setText("");
        }
    }

    private String translateServerError(String response) {
        if (response == null) {
            return "Không có phản hồi từ server.";
        }
        return response
                .replace("ERROR:", "")
                .replace("Insufficient balance", "Số dư không đủ.")
                .replace("Auction not found", "Không tìm thấy phiên đấu giá.")
                .replace("Invalid session", "Phiên đăng nhập đã hết hạn.")
                .replace("Only player accounts can do this action", "Chỉ tài khoản Player mới đặt giá được.")
                .replace("Nguoi ban khong duoc dat gia", "Người bán không được đặt giá phiên của mình.");
    }

    private static String formatMoney(double value) {
        return String.format("%,.0f VND", value);
    }

    private static String formatTime(LocalDateTime value) {
        return value.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private static String formatDateTime(String value) {
        try {
            return LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return value == null || value.isBlank() ? "--" : value;
        }
    }

    private static String displayStatus(String status) {
        return switch (status) {
            case "UPCOMING" -> "Sắp mở";
            case "ONGOING" -> "Đang diễn ra";
            case "EXTENDED" -> "Đã gia hạn";
            case "SUCCESSFUL" -> "Thành công";
            case "NO_BIDS" -> "Không có giá";
            case "CANCELED" -> "Đã hủy";
            default -> status == null || status.isBlank() ? "--" : status;
        };
    }

    private static String displayItemType(String type) {
        return switch (type) {
            case "Art" -> "Tranh / nghệ thuật";
            case "Electronics" -> "Điện tử";
            case "Vehicle" -> "Phương tiện";
            default -> type == null || type.isBlank() ? "--" : type;
        };
    }

    private static String shortId(String id) {
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(header);
        alert.setContentText(content == null ? "" : content);
        alert.showAndWait();
    }

    private record AuctionDetail(
            String id,
            String itemName,
            double currentPrice,
            double stepPrice,
            String status,
            String endTime,
            String highestBidderId,
            String highestBidderName,
            List<BidRow> bids,
            String openTime,
            String imagePath,
            String sellerName,
            String itemType,
            String itemDescription,
            String itemAttributes,
            boolean currentUserWinner
    ) {
        static AuctionDetail parse(String raw) {
            String[] parts = raw.split("\\|", -1);
            String history = parts.length >= 9 ? parts[8] : "";
            List<BidRow> bids = new ArrayList<>();
            if (!history.isBlank()) {
                for (String rawBid : history.split(";")) {
                    String[] bidParts = rawBid.split(",", -1);
                    if (bidParts.length >= 4) {
                        bids.add(new BidRow(
                                parseTimestamp(bidParts[0]),
                                bidParts[1],
                                bidParts[2].isBlank() ? bidParts[1] : bidParts[2],
                                parseDouble(bidParts[3])
                        ));
                    }
                }
            }
            return new AuctionDetail(
                    parts.length > 0 ? parts[0] : "",
                    parts.length > 1 ? parts[1] : "",
                    parts.length > 2 ? parseDouble(parts[2]) : 0.0,
                    parts.length > 3 ? parseDouble(parts[3]) : 0.0,
                    parts.length > 4 ? parts[4] : "",
                    parts.length > 5 ? parts[5] : "",
                    parts.length > 6 ? parts[6] : "",
                    parts.length > 7 ? parts[7] : "",
                    bids,
                    parts.length > 9 ? parts[9] : "",
                    parts.length > 10 ? parts[10] : "",
                    parts.length > 11 ? parts[11] : "",
                    parts.length > 12 ? parts[12] : "",
                    parts.length > 13 ? parts[13] : "",
                    parts.length > 14 ? parts[14] : "",
                    parts.length > 15 && Boolean.parseBoolean(parts[15])
            );
        }

        private static LocalDateTime parseTimestamp(String value) {
            try {
                return LocalDateTime.parse(value);
            } catch (Exception e) {
                return LocalDateTime.now();
            }
        }

        String openTimeText() {
            if (openTime != null && !openTime.isBlank()) {
                return formatDateTime(openTime);
            }
            return bids.stream()
                    .map(BidRow::timestamp)
                    .min(LocalDateTime::compareTo)
                    .map(BidRoomController::formatTime)
                    .orElse("--");
        }
    }

    private record BidRow(LocalDateTime timestamp, String bidderId, String bidderName, double amount) {
    }
}
