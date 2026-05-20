package Test;

import Network.AuctionClient;
import Network.ClientHandler;

import java.net.ServerSocket;
import java.net.Socket;

public class MultiClientAuctionSmokeTest {
    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> acceptClients(serverSocket));
            serverThread.setDaemon(true);
            serverThread.start();

            AuctionClient seller = new AuctionClient("127.0.0.1", serverSocket.getLocalPort());
            AuctionClient buyer = new AuctionClient("127.0.0.1", serverSocket.getLocalPort());
            seller.connect();
            buyer.connect();

            String suffix = Long.toString(System.nanoTime());
            require(seller.register("seller_" + suffix, "secret123", "Seller User",
                    "0123456789", "seller@example.com", "Ha Noi"), "seller register");
            require(buyer.register("buyer_" + suffix, "secret123", "Buyer User",
                    "0987654321", "buyer@example.com", "Da Nang"), "buyer register");
            require(seller.login("seller_" + suffix, "secret123"), "seller login");
            require(buyer.login("buyer_" + suffix, "secret123"), "buyer login");
            require(buyer.deposit(1000), "buyer deposit");

            String auctionId = seller.createAuctionAndReturnId("Phone", 100, 10, 5);
            require(auctionId != null && !auctionId.isBlank(), "seller create auction");
            require(buyer.joinAuction(auctionId), "buyer join auction");
            require(buyer.placeBid(auctionId, 150), "buyer bid");
            require(seller.endAuction(auctionId), "seller end auction");

            seller.disconnect();
            buyer.disconnect();
        }

        System.out.println("MULTI_CLIENT_AUCTION_SMOKE_TEST_OK");
    }

    private static void acceptClients(ServerSocket serverSocket) {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                Thread handlerThread = new Thread(new ClientHandler(socket));
                handlerThread.setDaemon(true);
                handlerThread.start();
            } catch (Exception e) {
                if (!serverSocket.isClosed()) {
                    throw new IllegalStateException("Server test failed", e);
                }
            }
        }
    }

    private static void require(boolean condition, String step) {
        if (!condition) {
            throw new IllegalStateException("Failed step: " + step);
        }
    }
}
