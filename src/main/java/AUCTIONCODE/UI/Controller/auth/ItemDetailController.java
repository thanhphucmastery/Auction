package AUCTIONCODE.UI.Controller.auth;
import AUCTIONCODE.UI.FxmlResources;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

public class ItemDetailController {
    @FXML
    private Button BackButton;
    @FXML
    private void BackToMain (ActionEvent event) {
        try {
            FXMLLoader loader = FxmlResources.loader("/AUCTIONCODE/UI/view/auth/Main.fxml");
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
