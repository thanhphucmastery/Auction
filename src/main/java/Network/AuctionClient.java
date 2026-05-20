package Network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class AuctionClient {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8080;
    private static final int CONNECT_TIMEOUT_MS = 3000;

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String sessionId;

    public AuctionClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        disconnect();

        Socket newSocket = new Socket();
        try {
            newSocket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket = newSocket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        } catch (IOException e) {
            try {
                newSocket.close();
            } catch (IOException closeError) {
                e.addSuppressed(closeError);
            }
            clearConnectionState();
            throw e;
        }
    }

    public void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clearConnectionState();
        }
    }

    public String sendRaw(String request) {
        if (!isConnected()) {
            return "ERROR:Client is not connected";
        }
        try {
            out.println(request);
            if (out.checkError()) {
                clearConnectionState();
                return "ERROR:Failed to send request";
            }
            String response = in.readLine();
            if (response == null) {
                clearConnectionState();
                return "ERROR:Server closed the connection";
            }
            return response;
        } catch (IOException e) {
            clearConnectionState();
            return "ERROR:" + e.getMessage();
        }
    }

    public boolean isConnected() {
        return socket != null
                && socket.isConnected()
                && !socket.isClosed()
                && in != null
                && out != null;
    }

    public boolean register(String username, String password, String fullName,
                            String phone, String email, String address) {
        String response = sendRaw("REGISTER:" + username + ":" + password + ":"
                + fullName + ":" + phone + ":" + email + ":" + address);
        return response != null && response.startsWith("OK");
    }

    public boolean registerAdmin(String username, String password, String fullName,
                                 String phone, String email, String address, String businessCode) {
        String response = sendRaw("REGISTER_ADMIN:" + username + ":" + password + ":"
                + fullName + ":" + phone + ":" + email + ":" + address + ":" + businessCode);
        return response != null && response.startsWith("OK");
    }

    public boolean login(String username, String password) {
        String response = sendRaw("LOGIN:" + username + ":" + password);
        if (response != null && response.startsWith("OK:")) {
            sessionId = response.substring(3);
            return true;
        }
        return false;
    }

    public boolean logout() {
        if (sessionId == null) {
            return false;
        }
        String response = sendRaw("LOGOUT:" + sessionId);
        if (response != null && response.startsWith("OK")) {
            sessionId = null;
            return true;
        }
        return false;
    }

    public boolean deposit(double amount) {
        String response = sendRaw("DEPOSIT:" + sessionId + ":" + amount);
        return response != null && response.startsWith("OK");
    }

    public String getAuctions() {
        String response = sendRaw("GET_AUCTIONS:" + sessionId);
        if (response != null && response.startsWith("OK:")) {
            return response.substring(3);
        }
        return null;
    }

    public String adminGetUsers() {
        String response = sendRaw("ADMIN_GET_USERS:" + sessionId);
        if (response != null && response.startsWith("OK:")) {
            return response.substring(3);
        }
        return null;
    }

    public boolean joinAuction(String auctionId) {
        String response = sendRaw("JOIN_AUCTION:" + sessionId + ":" + auctionId);
        return response != null && response.startsWith("OK");
    }

    public boolean placeBid(String auctionId, double amount) {
        String response = sendRaw("BID:" + sessionId + ":" + auctionId + ":" + amount);
        return response != null && response.startsWith("OK");
    }

    public boolean endAuction(String auctionId) {
        String response = sendRaw("END_AUCTION:" + sessionId + ":" + auctionId);
        return response != null && response.startsWith("OK");
    }

    public boolean createAuction(String itemName, double startPrice, double stepPrice, long durationMinutes) {
        return createAuctionAndReturnId(itemName, startPrice, stepPrice, durationMinutes) != null;
    }

    public String createAuctionAndReturnId(String itemName, double startPrice, double stepPrice, long durationMinutes) {
        String response = sendRaw("CREATE_AUCTION:" + sessionId + ":" + itemName + ":"
                + startPrice + ":" + stepPrice + ":" + durationMinutes);
        if (response != null && response.startsWith("OK:")) {
            return response.substring(3);
        }
        return null;
    }

    public boolean isLoggedIn() {
        return sessionId != null;
    }

    public String getSessionId() {
        return sessionId;
    }

    private void clearConnectionState() {
        socket = null;
        in = null;
        out = null;
        sessionId = null;
    }

    public static void main(String[] args) {
        String host = args.length >= 1 ? args[0] : DEFAULT_HOST;
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        AuctionClient client = new AuctionClient(host, port);
        try (Scanner scanner = new Scanner(System.in)) {
            client.connect();
            System.out.println("Connected to auction server " + host + ":" + port);
            runMenu(client, scanner);
        } catch (IOException e) {
            System.out.println("ERROR: Cannot connect to server: " + e.getMessage());
        } finally {
            client.disconnect();
        }
    }

    private static void runMenu(AuctionClient client, Scanner scanner) {
        boolean running = true;
        while (running && client.isConnected()) {
            printMenu(client);
            String choice = prompt(scanner, "Choose");
            switch (choice) {
                case "1" -> registerFromInput(client, scanner);
                case "2" -> loginFromInput(client, scanner);
                case "3" -> depositFromInput(client, scanner);
                case "4" -> createAuctionFromInput(client, scanner);
                case "5" -> listAuctions(client);
                case "6" -> joinAuctionFromInput(client, scanner);
                case "7" -> bidFromInput(client, scanner);
                case "8" -> endAuctionFromInput(client, scanner);
                case "9" -> logout(client);
                case "10" -> registerAdminFromInput(client, scanner);
                case "11" -> adminListUsers(client);
                case "raw" -> sendRawFromInput(client, scanner);
                case "0" -> running = false;
                default -> System.out.println("Unknown choice");
            }
        }
    }

    private static void printMenu(AuctionClient client) {
        System.out.println();
        System.out.println("=== Auction Client ===");
        System.out.println("Session: " + (client.isLoggedIn() ? client.getSessionId() : "not logged in"));
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Deposit");
        System.out.println("4. Create auction");
        System.out.println("5. List auctions");
        System.out.println("6. Join auction");
        System.out.println("7. Bid");
        System.out.println("8. End auction");
        System.out.println("9. Logout");
        System.out.println("10. Register admin");
        System.out.println("11. Admin list users");
        System.out.println("raw. Send raw request");
        System.out.println("0. Exit");
    }

    private static void registerFromInput(AuctionClient client, Scanner scanner) {
        String username = prompt(scanner, "Username");
        String password = prompt(scanner, "Password");
        String fullName = prompt(scanner, "Full name");
        String phone = prompt(scanner, "Phone (10 digits)");
        String email = prompt(scanner, "Email");
        String address = prompt(scanner, "Address");
        String response = client.sendRaw("REGISTER:" + username + ":" + password + ":"
                + fullName + ":" + phone + ":" + email + ":" + address);
        System.out.println(response);
    }

    private static void registerAdminFromInput(AuctionClient client, Scanner scanner) {
        String username = prompt(scanner, "Username");
        String password = prompt(scanner, "Password");
        String fullName = prompt(scanner, "Full name");
        String phone = prompt(scanner, "Phone (10 digits)");
        String email = prompt(scanner, "Email");
        String address = prompt(scanner, "Address");
        String businessCode = prompt(scanner, "Business code");
        String response = client.sendRaw("REGISTER_ADMIN:" + username + ":" + password + ":"
                + fullName + ":" + phone + ":" + email + ":" + address + ":" + businessCode);
        System.out.println(response);
    }

    private static void loginFromInput(AuctionClient client, Scanner scanner) {
        String username = prompt(scanner, "Username");
        String password = prompt(scanner, "Password");
        String response = client.sendRaw("LOGIN:" + username + ":" + password);
        if (response != null && response.startsWith("OK:")) {
            client.sessionId = response.substring(3);
        }
        System.out.println(response);
    }

    private static void depositFromInput(AuctionClient client, Scanner scanner) {
        if (!requireLogin(client)) {
            return;
        }
        String amount = prompt(scanner, "Amount");
        System.out.println(client.sendRaw("DEPOSIT:" + client.getSessionId() + ":" + amount));
    }

    private static void createAuctionFromInput(AuctionClient client, Scanner scanner) {
        if (!requireLogin(client)) {
            return;
        }
        String itemName = prompt(scanner, "Item name");
        String startPrice = prompt(scanner, "Start price");
        String stepPrice = prompt(scanner, "Step price");
        String durationMinutes = prompt(scanner, "Duration minutes");
        String response = client.sendRaw("CREATE_AUCTION:" + client.getSessionId() + ":"
                + itemName + ":" + startPrice + ":" + stepPrice + ":" + durationMinutes);
        System.out.println(response);
    }

    private static void listAuctions(AuctionClient client) {
        if (!requireLogin(client)) {
            return;
        }
        System.out.println(client.sendRaw("GET_AUCTIONS:" + client.getSessionId()));
    }

    private static void adminListUsers(AuctionClient client) {
        if (!requireLogin(client)) {
            return;
        }
        System.out.println(client.sendRaw("ADMIN_GET_USERS:" + client.getSessionId()));
    }

    private static void joinAuctionFromInput(AuctionClient client, Scanner scanner) {
        if (!requireLogin(client)) {
            return;
        }
        String auctionId = prompt(scanner, "Auction ID");
        System.out.println(client.sendRaw("JOIN_AUCTION:" + client.getSessionId() + ":" + auctionId));
    }

    private static void bidFromInput(AuctionClient client, Scanner scanner) {
        if (!requireLogin(client)) {
            return;
        }
        String auctionId = prompt(scanner, "Auction ID");
        String amount = prompt(scanner, "Bid amount");
        System.out.println(client.sendRaw("BID:" + client.getSessionId() + ":" + auctionId + ":" + amount));
    }

    private static void endAuctionFromInput(AuctionClient client, Scanner scanner) {
        if (!requireLogin(client)) {
            return;
        }
        String auctionId = prompt(scanner, "Auction ID");
        System.out.println(client.sendRaw("END_AUCTION:" + client.getSessionId() + ":" + auctionId));
    }

    private static void logout(AuctionClient client) {
        if (!requireLogin(client)) {
            return;
        }
        String response = client.sendRaw("LOGOUT:" + client.getSessionId());
        if (response != null && response.startsWith("OK")) {
            client.sessionId = null;
        }
        System.out.println(response);
    }

    private static void sendRawFromInput(AuctionClient client, Scanner scanner) {
        String request = prompt(scanner, "Raw request");
        System.out.println(client.sendRaw(request));
    }

    private static boolean requireLogin(AuctionClient client) {
        if (!client.isLoggedIn()) {
            System.out.println("ERROR: Login first");
            return false;
        }
        return true;
    }

    private static String prompt(Scanner scanner, String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }
}
