package AUCTIONCODE.UI.Controller.auth;

import AUCTIONCODE.UI.FxmlResources;
import AUCTIONCODE.UI.Main;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TransactionHistoryController {
    @FXML private TableView<TransactionRow> transactionTable;
    @FXML private TableColumn<TransactionRow, String> colTime;
    @FXML private TableColumn<TransactionRow, String> colType;
    @FXML private TableColumn<TransactionRow, String> colItem;
    @FXML private TableColumn<TransactionRow, String> colAmount;
    @FXML private TableColumn<TransactionRow, String> colCounterparty;
    @FXML private TableColumn<TransactionRow, String> colNote;
    @FXML private Label summaryLabel;

    @FXML
    private void initialize() {
        colTime.setCellValueFactory(data -> new SimpleStringProperty(formatDateTime(data.getValue().time())));
        colType.setCellValueFactory(data -> new SimpleStringProperty(displayType(data.getValue().type())));
        colItem.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().itemName()));
        colAmount.setCellValueFactory(data -> new SimpleStringProperty(formatSignedMoney(data.getValue().amount())));
        colCounterparty.setCellValueFactory(data -> new SimpleStringProperty(blank(data.getValue().counterparty())));
        colNote.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().note()));
        refresh();
    }

    @FXML
    private void refresh() {
        List<TransactionRow> rows = parseRows(Main.client.getTransactionHistory());
        rows.sort(Comparator.comparing(TransactionRow::timeValue).reversed());
        transactionTable.setItems(FXCollections.observableArrayList(rows));
        summaryLabel.setText(rows.size() + " giao dịch");
    }

    @FXML
    private void backToMain(ActionEvent event) {
        switchScene(event, "/AUCTIONCODE/UI/view/auth/Main.fxml");
    }

    private List<TransactionRow> parseRows(String data) {
        List<TransactionRow> rows = new ArrayList<>();
        if (data == null || data.isBlank()) {
            return rows;
        }
        for (String raw : data.split(";", -1)) {
            if (raw.isBlank()) {
                continue;
            }
            String[] parts = raw.split("\\|", -1);
            if (parts.length >= 7) {
                rows.add(new TransactionRow(
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[3],
                        parts[4],
                        parseDouble(parts[5]),
                        parts[6]
                ));
            }
        }
        return rows;
    }

    private void switchScene(ActionEvent event, String fxml) {
        try {
            FXMLLoader loader = FxmlResources.loader(fxml);
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showError("Không mở được màn hình", e.getMessage());
        }
    }

    private static String displayType(String type) {
        return switch (type) {
            case "WON" -> "Thắng đấu giá";
            case "SOLD" -> "Bán thành công";
            case "REFUNDED" -> "Hoàn tiền";
            case "NO_BIDS" -> "Không thành công";
            case "CANCELED" -> "Đã hủy";
            default -> type == null || type.isBlank() ? "--" : type;
        };
    }

    private static String formatSignedMoney(double amount) {
        if (amount > 0) {
            return "+" + formatMoney(amount);
        }
        if (amount < 0) {
            return "-" + formatMoney(Math.abs(amount));
        }
        return "0 VND";
    }

    private static String formatMoney(double value) {
        return String.format("%,.0f VND", value);
    }

    private static String formatDateTime(String value) {
        try {
            return LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return value == null || value.isBlank() ? "--" : value;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(header);
        alert.setContentText(content == null ? "" : content);
        alert.showAndWait();
    }

    public record TransactionRow(
            String time,
            String type,
            String auctionId,
            String itemName,
            String note,
            double amount,
            String counterparty
    ) {
        LocalDateTime timeValue() {
            try {
                return LocalDateTime.parse(time);
            } catch (Exception e) {
                return LocalDateTime.MIN;
            }
        }
    }
}
