package Test;

import Network.AuctionClient;
import Network.ClientHandler;

import java.net.ServerSocket;
import java.net.Socket;

public class ClientServerSmokeTest {
    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> acceptOneClient(serverSocket));
            serverThread.start();

            AuctionClient client = new AuctionClient("127.0.0.1", serverSocket.getLocalPort());
            client.connect();

            String suffix = Long.toString(System.nanoTime());
            if (!client.register("socket_user_" + suffix, "secret123", "Socket User",
                    "0111222333", "socket@example.com", "Ho Chi Minh")) {
                throw new IllegalStateException("Register failed");
            }
            if (!client.login("socket_user_" + suffix, "secret123")) {
                throw new IllegalStateException("Login failed");
            }
            if (!client.deposit(500)) {
                throw new IllegalStateException("Deposit failed");
            }
            if (!client.logout()) {
                throw new IllegalStateException("Logout failed");
            }
            if (!client.registerAdmin("socket_admin_" + suffix, "secret123", "Socket Admin",
                    "0111222444", "socket-admin@example.com", "Ha Noi", "BIZ-001")) {
                throw new IllegalStateException("Admin register failed");
            }
            if (!client.login("socket_admin_" + suffix, "secret123")) {
                throw new IllegalStateException("Admin login failed");
            }
            String users = client.adminGetUsers();
            if (users == null || !users.contains("socket_admin_" + suffix)) {
                throw new IllegalStateException("Admin list users failed");
            }

            client.disconnect();
            serverThread.join(3000);
        }

        System.out.println("CLIENT_SERVER_SMOKE_TEST_OK");
    }

    private static void acceptOneClient(ServerSocket serverSocket) {
        try {
            Socket socket = serverSocket.accept();
            new ClientHandler(socket).run();
        } catch (Exception e) {
            throw new IllegalStateException("Server test failed", e);
        }
    }
}
