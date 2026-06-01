package AUCTIONCODE.UI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import AUCTIONCODE.Network.AuctionClient;

public class Main extends Application {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8080;

    public static AuctionClient client;

    @Override
    public void start(Stage primaryStage) {
        try {
            client = createClient();
            try {
                client.connect();
                System.out.println("Connected to auction server " + getConfiguredHost() + ":" + getConfiguredPort());
            } catch (Exception connectEx) {
                System.out.println("Could not connect to external server, starting embedded server...");
                int port = getConfiguredPort();
                Thread serverThread = new Thread(() -> {
                    try {
                        AUCTIONCODE.Network.AuctionServer.main(new String[]{String.valueOf(port)});
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }, "embedded-auction-server");
                serverThread.setDaemon(true);
                serverThread.start();
                // wait briefly for server to start
                try {
                    Thread.sleep(400);
                    client.connect();
                    System.out.println("Connected to embedded auction server " + getConfiguredHost() + ":" + getConfiguredPort());
                } catch (Exception e) {
                    System.out.println("Failed to connect to embedded server: " + e.getMessage());
                }
            }
            FXMLLoader loader = FxmlResources.loader("/AUCTIONCODE/UI/view/auth/Login.fxml");
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setTitle("Hệ thống quản lý");
            primaryStage.setScene(scene);

            // LẮNG NGHE SỰ KIỆN ĐỔI SCENE
            primaryStage.sceneProperty().addListener((observable, oldScene, newScene) -> {
                if (newScene != null) {
                    // Đợi một nhịp cực ngắn để giao diện mới nạp xong rồi mới phóng to
                    javafx.application.Platform.runLater(() -> {
                        primaryStage.setMaximized(false); // Reset trạng thái
                        primaryStage.setMaximized(true);  // Ép phóng to
                    });
                }
            });

            primaryStage.show();
            primaryStage.setMaximized(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        launch(args);
    }

    private AuctionClient createClient() {
        return new AuctionClient(getConfiguredHost(), getConfiguredPort());
    }

    private String getConfiguredHost() {
        Parameters parameters = getParameters();
        String host = parameters.getNamed().get("host");
        if (host == null || host.isBlank()) {
            host = System.getProperty("auction.host");
        }
        if (host == null || host.isBlank()) {
            host = System.getenv("AUCTION_HOST");
        }
        if (host == null || host.isBlank()) {
            host = DEFAULT_HOST;
        }
        return host;
    }

    private int getConfiguredPort() {
        Parameters parameters = getParameters();
        String portValue = parameters.getNamed().get("port");
        if (portValue == null || portValue.isBlank()) {
            portValue = System.getProperty("auction.port");
        }
        if (portValue == null || portValue.isBlank()) {
            portValue = System.getenv("AUCTION_PORT");
        }
        if (portValue == null || portValue.isBlank()) {
            return DEFAULT_PORT;
        }
        return Integer.parseInt(portValue);
    }
    }
