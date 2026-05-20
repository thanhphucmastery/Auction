package Network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = parsePort(args);
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Auction server is running on port " + port);
            while (true) {
                Socket socketclient = serverSocket.accept();
                System.out.println("New client connected: " + socketclient.getInetAddress());
                ClientHandler handler = new ClientHandler(socketclient);
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket!= null){
                try {
                    serverSocket.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private static int parsePort(String[] args) {
        if (args.length == 0) {
            return DEFAULT_PORT;
        }
        try {
            int port = Integer.parseInt(args[0]);
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Port must be from 1 to 65535");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port must be a number", e);
        }
    }
}
