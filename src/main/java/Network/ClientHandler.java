package Network;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    BufferedReader in;
    PrintWriter out;

    public ClientHandler(Socket socket){
        this.socket=socket;
    }

    @Override
    public void run(){
        try {
            InputStream read = socket.getInputStream();
            in = new BufferedReader(new InputStreamReader(read));

            OutputStream write = socket.getOutputStream();
            out=new PrintWriter(new OutputStreamWriter(write),true);

            String request;
            while ( (request=in.readLine()) != null){
                String response = RequestParser.handle(request);
                out.println(response);
            }

        } catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                if (socket != null){
                    socket.close();
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}

