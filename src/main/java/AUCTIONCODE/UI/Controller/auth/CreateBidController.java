package AUCTIONCODE.UI.Controller.auth;

import AUCTIONCODE.UI.FxmlResources;
import AUCTIONCODE.UI.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class CreateBidController {
    @FXML private DatePicker startDatePicker;
    @FXML private Spinner<Integer> startHourSpinner;
    @FXML private Spinner<Integer> startMinuteSpinner;
    @FXML private DatePicker endDatePicker;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private TextField bidStepField;
    @FXML private TextField startPriceField;
    @FXML private ComboBox<ItemOption> itemComboBox;
    @FXML private ImageView selectedItemImageView;
    @FXML private javafx.scene.control.Label selectedImagePlaceholderLabel;
    @FXML private Button DeclineButton;

    private final List<ItemOption> itemOptions = new ArrayList<>();

    @FXML
    public void initialize() {
        LocalDateTime defaultStart = LocalDateTime.now().plusMinutes(1);
        LocalDateTime defaultEnd = defaultStart.plusMinutes(30);

        startDatePicker.setValue(defaultStart.toLocalDate());
        endDatePicker.setValue(defaultEnd.toLocalDate());
        startHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, defaultStart.getHour()));
        startMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, defaultStart.getMinute()));
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, defaultEnd.getHour()));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, defaultEnd.getMinute()));
        loadWarehouseItems();
        itemComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updateSelectedImage(newValue));
    }

    public void setPreselectedItem(String itemId) {
        if (itemId == null) {
            return;
        }
        itemOptions.stream()
                .filter(item -> item.id().equals(itemId))
                .findFirst()
                .ifPresent(item -> itemComboBox.getSelectionModel().select(item));
    }

    @FXML
    private void handleActivateAuction(ActionEvent event) {
        try {
            ItemOption selectedItem = itemComboBox.getSelectionModel().getSelectedItem();
            String startPriceText = startPriceField.getText().trim();
            String bidStepText = bidStepField.getText().trim();

            if (selectedItem == null || startPriceText.isEmpty() || bidStepText.isEmpty()) {
                showError("Thiếu thông tin", "Vui lòng chọn vật phẩm trong kho, nhập giá khởi điểm và bước giá.");
                return;
            }

            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (startDate == null || endDate == null) {
                showError("Thiếu thời gian", "Vui lòng chọn đủ ngày bắt đầu và ngày kết thúc.");
                return;
            }

            LocalDateTime startDateTime = LocalDateTime.of(
                    startDate,
                    LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue())
            );
            LocalDateTime endDateTime = LocalDateTime.of(
                    endDate,
                    LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue())
            );
            LocalDateTime now = LocalDateTime.now();
            long startDelaySeconds = Duration.between(now, startDateTime).getSeconds();
            long startDelayMinutes = Math.max(0, (startDelaySeconds + 59) / 60);
            long durationMinutes = Duration.between(startDateTime, endDateTime).toMinutes();

            double startPrice = Double.parseDouble(startPriceText);
            double stepPrice = Double.parseDouble(bidStepText);
            if (startPrice <= 0 || stepPrice <= 0) {
                showError("Giá không hợp lệ", "Giá khởi điểm và bước giá phải lớn hơn 0.");
                return;
            }
            if (startDateTime.isBefore(now.minusSeconds(30))) {
                showError("Thời gian không hợp lệ", "Thời điểm bắt đầu không được nằm trong quá khứ.");
                return;
            }
            if (durationMinutes <= 0) {
                showError("Thời gian không hợp lệ", "Thời điểm kết thúc phải sau thời điểm bắt đầu.");
                return;
            }

            String auctionId = Main.client.createAuctionFromItemAndReturnId(
                    selectedItem.id(),
                    startPrice,
                    stepPrice,
                    startDelayMinutes,
                    durationMinutes
            );

            if (auctionId != null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thành công");
                alert.setHeaderText(null);
                alert.setContentText("Kích hoạt phiên đấu giá thành công!");
                alert.showAndWait();
                BackToMain(event);
            } else {
                showError("Không thể tạo phiên đấu giá", "Vật phẩm không tồn tại hoặc đã được đưa lên phiên.");
            }
        } catch (NumberFormatException e) {
            showError("Giá không hợp lệ", "Vui lòng nhập giá bằng số.");
        } catch (Exception e) {
            showError("Không thể tạo phiên", e.getMessage());
        }
    }

    @FXML
    private void handleDecline(ActionEvent event) {
        BackToMain(event);
    }

    private void BackToMain(ActionEvent event) {
        try {
            FXMLLoader loader = FxmlResources.loader("/AUCTIONCODE/UI/view/auth/Main.fxml");
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(header);
        alert.setContentText(content == null ? "" : content);
        alert.showAndWait();
    }

    private void loadWarehouseItems() {
        itemOptions.clear();
        String payload = Main.client.getMyItems();
        if (payload != null && !payload.isBlank()) {
            for (String row : payload.split(";")) {
                String[] parts = row.split("\\|", -1);
                if (parts.length >= 3) {
                    itemOptions.add(new ItemOption(parts[0], parts[1], parts[2], parts.length > 7 ? parts[7] : ""));
                }
            }
        }
        itemComboBox.getItems().setAll(itemOptions);
    }

    private void updateSelectedImage(ItemOption item) {
        String imagePath = item == null ? "" : item.imagePath();
        if (!imagePath.isBlank() && Files.exists(Path.of(imagePath))) {
            selectedItemImageView.setImage(new Image(Path.of(imagePath).toUri().toString(), true));
            selectedImagePlaceholderLabel.setVisible(false);
        } else {
            selectedItemImageView.setImage(null);
            selectedImagePlaceholderLabel.setVisible(true);
        }
    }

    public record ItemOption(String id, String type, String name, String imagePath) {
        @Override
        public String toString() {
            String typeLabel = switch (type) {
                case "Electronics" -> "Điện tử";
                case "Vehicle" -> "Xe cộ";
                default -> "Nghệ thuật";
            };
            return name + " (" + typeLabel + ")";
        }
    }
}
