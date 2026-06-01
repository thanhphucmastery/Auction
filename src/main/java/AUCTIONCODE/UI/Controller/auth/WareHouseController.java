package AUCTIONCODE.UI.Controller.auth;

import AUCTIONCODE.UI.FxmlResources;
import AUCTIONCODE.UI.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WareHouseController {
    @FXML private TextField searchField;
    @FXML private FlowPane itemFlow;

    private final List<InventoryItem> items = new ArrayList<>();

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> renderItems());
        refreshItems();
    }

    @FXML
    private void BackToMain(ActionEvent event) {
        switchScene(event, "/AUCTIONCODE/UI/view/auth/Main.fxml");
    }

    private void refreshItems() {
        items.clear();
        String payload = Main.client.getMyItems();
        if (payload != null && !payload.isBlank()) {
            for (String row : payload.split(";")) {
                InventoryItem item = InventoryItem.parse(row);
                if (item != null) {
                    items.add(item);
                }
            }
        }
        renderItems();
    }

    private void renderItems() {
        itemFlow.getChildren().clear();
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<InventoryItem> visibleItems = items.stream()
                .filter(item -> keyword.isBlank()
                        || item.name().toLowerCase().contains(keyword)
                        || item.description().toLowerCase().contains(keyword)
                        || item.typeLabel().toLowerCase().contains(keyword))
                .toList();

        if (visibleItems.isEmpty()) {
            Label empty = new Label("Chưa có vật phẩm nào trong kho.");
            empty.getStyleClass().add("muted");
            itemFlow.getChildren().add(empty);
            return;
        }

        for (InventoryItem item : visibleItems) {
            itemFlow.getChildren().add(createItemCard(item));
        }
    }

    private VBox createItemCard(InventoryItem item) {
        StackPane preview = createImagePreview(item.imagePath(), item.typeLabel(), 90, 220);
        preview.setPrefHeight(90);

        Label name = new Label(item.name());
        name.setWrapText(true);
        name.setStyle("-fx-font-weight: 800;");

        Label description = new Label(item.description().isBlank() ? "Chưa có mô tả" : item.description());
        description.setWrapText(true);
        description.getStyleClass().add("muted");

        Label status = new Label("Sẵn sàng mở phiên");
        status.getStyleClass().add("muted-small");

        Button editButton = new Button("✎ Sửa");
        editButton.getStyleClass().add("ghost-button");
        editButton.setOnAction(event -> editItem(item));

        Button auctionButton = new Button("▶ Mở phiên");
        auctionButton.getStyleClass().add("primary-button");
        auctionButton.setOnAction(event -> openCreateBid(item, event));

        Button deleteButton = new Button("× Xóa");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(event -> deleteItem(item));

        HBox actions = new HBox(8, editButton, auctionButton, deleteButton);
        VBox card = new VBox(10, preview, name, description, status, actions);
        card.setPrefWidth(260);
        card.getStyleClass().add("surface-card");
        return card;
    }

    private void editItem(InventoryItem item) {
        Dialog<InventoryItem> dialog = new Dialog<>();
        dialog.setTitle("Sửa thông tin sản phẩm");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().setAll("Art", "Electronics", "Vehicle");
        typeCombo.getSelectionModel().select(item.type());
        TextField nameField = new TextField(item.name());
        TextArea descriptionArea = new TextArea(item.description());
        descriptionArea.setPrefRowCount(4);
        TextField extra1Field = new TextField(item.extra1());
        TextField extra2Field = new TextField(item.extra2());
        TextField extra3Field = new TextField(item.extra3());
        String[] imagePath = {item.imagePath()};
        StackPane imagePreview = createImagePreview(imagePath[0], "Chưa có ảnh", 120, 240);
        Button chooseImageButton = new Button("▧ Đổi ảnh");
        chooseImageButton.getStyleClass().add("ghost-button");
        chooseImageButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Chọn ảnh vật phẩm");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
            );
            File selected = chooser.showOpenDialog(chooseImageButton.getScene().getWindow());
            if (selected == null) {
                return;
            }
            try {
                imagePath[0] = copyImageToDataFolder(selected.toPath());
                imagePreview.getChildren().setAll(createImagePreview(imagePath[0], "Chưa có ảnh", 120, 240).getChildren());
            } catch (IOException e) {
                showError("Không thể chọn ảnh", e.getMessage());
            }
        });
        Label extra1Label = new Label();
        Label extra2Label = new Label();
        Label extra3Label = new Label();

        Runnable updateLabels = () -> updateExtraLabels(typeCombo.getValue(), extra1Label, extra2Label, extra3Label, extra3Field);
        typeCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateLabels.run());
        updateLabels.run();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Loại"), typeCombo);
        grid.addRow(1, new Label("Tên sản phẩm"), nameField);
        grid.addRow(2, new Label("Mô tả"), descriptionArea);
        grid.addRow(3, extra1Label, extra1Field);
        grid.addRow(4, extra2Label, extra2Field);
        grid.addRow(5, extra3Label, extra3Field);
        grid.addRow(6, new Label("Ảnh"), new VBox(8, imagePreview, chooseImageButton));
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            return new InventoryItem(
                    item.id(),
                    typeCombo.getValue(),
                    nameField.getText().trim(),
                    descriptionArea.getText().trim(),
                    extra1Field.getText().trim(),
                    extra2Field.getText().trim(),
                    extra3Field.getText().trim(),
                    imagePath[0]
            );
        });

        Optional<InventoryItem> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            if (updated.name().isBlank()) {
                showError("Thiếu thông tin", "Tên sản phẩm không được để trống.");
                return;
            }
            if (!Main.client.updateItem(updated.id(), updated.type(), updated.name(), updated.description(),
                    updated.extra1(), updated.extra2(), updated.extra3(), updated.imagePath())) {
                showError("Không thể cập nhật", "Sản phẩm không tồn tại hoặc đã được đưa lên phiên.");
                return;
            }
            refreshItems();
        });
    }

    private void deleteItem(InventoryItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa vật phẩm");
        confirm.setHeaderText("Xóa " + item.name() + "?");
        confirm.setContentText("Vật phẩm sẽ bị xóa khỏi kho nếu chưa đưa lên phiên đấu giá.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        if (!Main.client.deleteItem(item.id())) {
            showError("Không thể xóa", "Sản phẩm không tồn tại hoặc đã được đưa lên phiên.");
            return;
        }
        refreshItems();
    }

    private void openCreateBid(InventoryItem item, ActionEvent event) {
        try {
            FXMLLoader loader = FxmlResources.loader("/AUCTIONCODE/UI/view/auth/CreateBid.fxml");
            Parent root = loader.load();
            CreateBidController controller = loader.getController();
            controller.setPreselectedItem(item.id());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showError("Không thể mở phiên đấu giá", e.getMessage());
        }
    }

    private void updateExtraLabels(String type, Label extra1Label, Label extra2Label, Label extra3Label, TextField extra3Field) {
        if ("Electronics".equals(type)) {
            extra1Label.setText("Thương hiệu");
            extra2Label.setText("Năm sản xuất");
            extra3Label.setText("Bảo hành (tháng)");
            extra3Field.setDisable(false);
        } else if ("Vehicle".equals(type)) {
            extra1Label.setText("Dòng xe / model");
            extra2Label.setText("Năm sản xuất");
            extra3Label.setText("Số km đã đi");
            extra3Field.setDisable(false);
        } else {
            extra1Label.setText("Tác giả / người tạo");
            extra2Label.setText("Năm sáng tác");
            extra3Label.setText("Không dùng");
            extra3Field.setDisable(true);
        }
    }

    private StackPane createImagePreview(String imagePath, String fallbackText, double height, double width) {
        StackPane preview = new StackPane();
        preview.setPrefHeight(height);
        preview.getStyleClass().add("image-placeholder");
        if (imagePath != null && !imagePath.isBlank() && Files.exists(Path.of(imagePath))) {
            ImageView imageView = new ImageView(new Image(Path.of(imagePath).toUri().toString(), true));
            imageView.setFitHeight(height);
            imageView.setFitWidth(width);
            imageView.setPreserveRatio(true);
            preview.getChildren().add(imageView);
        } else {
            Label label = new Label(fallbackText);
            label.getStyleClass().add("muted");
            preview.getChildren().add(label);
        }
        return preview;
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

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(header);
        alert.setContentText(content == null ? "" : content);
        alert.showAndWait();
    }

    public record InventoryItem(String id, String type, String name, String description,
                                String extra1, String extra2, String extra3, String imagePath) {
        static InventoryItem parse(String row) {
            String[] parts = row.split("\\|", -1);
            if (parts.length < 7) {
                return null;
            }
            return new InventoryItem(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6],
                    parts.length > 7 ? parts[7] : "");
        }

        String typeLabel() {
            return switch (type) {
                case "Electronics" -> "Điện tử";
                case "Vehicle" -> "Xe cộ";
                default -> "Nghệ thuật";
            };
        }

        @Override
        public String toString() {
            return name + " (" + typeLabel() + ")";
        }
    }
}
