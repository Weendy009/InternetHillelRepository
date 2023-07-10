import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private static final int PORT = 8080;

    public Server() {
        try {
            serverSocket = new ServerSocket(PORT);
            clients = new ArrayList<>();
            System.out.println("Server started on port " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket);

                ClientHandler clientHandler = new ClientHandler(socket);

                clients.add(clientHandler);

                Thread thread = new Thread(clientHandler);
                thread.start();

                broadcastMessage("[SERVER] New client connected: " + socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream inputStream;
        private DataOutputStream outputStream;
        private String clientName;

        public ClientHandler(Socket socket) {
            try {
                this.socket = socket;
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
                clientName = "client-" + (clients.size() + 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                sendMessage("[SERVER] Welcome, " + clientName + "!");

                broadcastMessage("[SERVER] " + clientName + " successfully connected.");

                while (true) {
                    String receivedMessage = inputStream.readUTF();
                    if (receivedMessage.equals("-exit")) {
                        break;
                    } else if (receivedMessage.startsWith("-file")) {
                        receiveFile(receivedMessage);
                    } else {
                        sendMessage("[SERVER] You said: " + receivedMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                handleClientDisconnect();
            }
        }

        public void sendMessage(String message) {
            try {
                outputStream.writeUTF(message);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void receiveFile(String command) throws IOException {
            String[] parts = command.split(" ");
            if (parts.length < 2) {
                System.out.println("Invalid command format. Usage: -file <file_path>");
                return;
            }
            String filePath = parts[1];
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
                fileOutputStream.flush();
            }
            System.out.println("File received: " + fileName);
        }

        private void handleClientDisconnect() {
            clients.remove(this);

            broadcastMessage("[SERVER] Client disconnected: " + socket);

            try {
                inputStream.close();
                outputStream.close();

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}