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
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class AddItemController {
    @FXML private TextField itemNameField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextArea descriptionArea;
    @FXML private Label extraLabel1;
    @FXML private Label extraLabel2;
    @FXML private Label extraLabel3;
    @FXML private TextField extraField1;
    @FXML private TextField extraField2;
    @FXML private TextField extraField3;
    @FXML private ImageView previewImage;
    @FXML private Label imageHintLabel;
    @FXML private Button DeclineButton;

    private String imagePath = "";

    @FXML
    public void initialize() {
        categoryCombo.getItems().setAll("Art", "Electronics", "Vehicle");
        categoryCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateExtraFields(newValue));
        categoryCombo.getSelectionModel().select("Art");
    }

    @FXML
    private void handleSaveItem(ActionEvent event) {
        String name = itemNameField.getText().trim();
        String type = categoryCombo.getValue();
        String description = descriptionArea.getText().trim();

        if (name.isEmpty() || type == null || type.isBlank()) {
            showError("Thiếu thông tin", "Vui lòng nhập tên sản phẩm và chọn loại sản phẩm.");
            return;
        }

        try {
            String itemId = Main.client.addItem(
                    type,
                    name,
                    description,
                    extraField1.getText().trim(),
                    extraField2.getText().trim(),
                    extraField3.getText().trim(),
                    imagePath
            );
            if (itemId == null) {
                showError("Không thể lưu vật phẩm", "Server không nhận đăng ký món hàng.");
                return;
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thành công");
            alert.setHeaderText(null);
            alert.setContentText("Đã đăng ký vật phẩm vào kho.");
            alert.showAndWait();
            openWarehouse(event);
        } catch (Exception e) {
            showError("Không thể lưu vật phẩm", e.getMessage());
        }
    }

    @FXML
    private void chooseImage(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh vật phẩm");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File selected = chooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (selected == null) {
            return;
        }

        try {
            imagePath = copyImageToDataFolder(selected.toPath());
            previewImage.setImage(new Image(Path.of(imagePath).toUri().toString(), true));
            imageHintLabel.setVisible(false);
        } catch (IOException e) {
            showError("Không thể chọn ảnh", e.getMessage());
        }
    }

    @FXML
    private void DeclineAddItem(ActionEvent event) {
        BackToMain(event);
    }

    private void updateExtraFields(String type) {
        extraField1.clear();
        extraField2.clear();
        extraField3.clear();
        if ("Electronics".equals(type)) {
            extraLabel1.setText("Thương hiệu");
            extraLabel2.setText("Năm sản xuất");
            extraLabel3.setText("Bảo hành (tháng)");
            extraField3.setDisable(false);
        } else if ("Vehicle".equals(type)) {
            extraLabel1.setText("Dòng xe / model");
            extraLabel2.setText("Năm sản xuất");
            extraLabel3.setText("Số km đã đi");
            extraField3.setDisable(false);
        } else {
            extraLabel1.setText("Tác giả / người tạo");
            extraLabel2.setText("Năm sáng tác");
            extraLabel3.setText("Không dùng");
            extraField3.setDisable(true);
        }
    }

    private void openWarehouse(ActionEvent event) {
        switchScene(event, "/AUCTIONCODE/UI/view/auth/WareHouse.fxml");
    }

    private void BackToMain(ActionEvent event) {
        switchScene(event, "/AUCTIONCODE/UI/view/auth/Main.fxml");
    }

    private void switchScene(ActionEvent event, String path) {
        try {
            FXMLLoader loader = FxmlResources.loader(path);
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String copyImageToDataFolder(Path source) throws IOException {
        Path imageDir = Path.of("data", "images");
        Files.createDirectories(imageDir);
        String fileName = source.getFileName().toString();
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = fileName.substring(dotIndex);
        }
        Path target = imageDir.resolve(UUID.randomUUID() + extension);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString().replace("\\", "/");
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(header);
        alert.setContentText(content == null ? "" : content);
        alert.showAndWait();
    }
}
