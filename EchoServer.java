import java.io.*;
import java.net.*;

class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            System.out.println("Client connected: " + clientSocket.getInetAddress());

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);
                out.println(line);
            }

            System.out.println("Client disconnected.");

            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class EchoServer {
    public static void main(String[] args) throws IOException {
        int port = 5000;
        ServerSocket serverSocket = new ServerSocket(port);

        System.out.println("Server listening on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            Thread thread = new Thread(new ClientHandler(clientSocket));
            thread.start();
        }
    }
}