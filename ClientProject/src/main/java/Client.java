import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private static final int PORT = 8080;
    private static final String SERV_ADDRESS = "localhost";

    public Client() {
        try {
            socket = new Socket(SERV_ADDRESS, PORT);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("Connected to server at " + SERV_ADDRESS + ":" + PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        Thread receiveThread = new Thread(this::receiveMessages);
        receiveThread.start();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String command = scanner.nextLine();
                sendCommand(command);

                if (command.equals("-exit")) {
                    disconnect();
                    break;
                }
            }
        }
    }

    private void receiveMessages() {
        try {
            while (!socket.isClosed()) {
                String message = inputStream.readUTF();
                System.out.println(message);
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                e.printStackTrace();
            }
        }
    }

    private void sendCommand(String command) {
        try {
            if (command.startsWith("-file")) {
                sendFile(command);
            } else {
                outputStream.writeUTF(command);
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile(String command) throws IOException {
        String[] parts = command.split(" ");
        if (parts.length < 2) {
            System.out.println("Invalid command format. Usage: -file <file_path>");
            return;
        }
        String filePath = parts[1];

        filePath = filePath.replace("\\", "/");

        File file = new File(filePath);
        if (file.isDirectory()) {
            System.out.println("Invalid file path: " + filePath + " (It is a directory)");
            return;
        }

        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    System.out.println("Empty file created: " + filePath);
                } else {
                    System.out.println("Failed to create empty file: " + filePath);
                    return;
                }
            } catch (IOException e) {
                System.out.println("Failed to create empty file: " + filePath);
                e.printStackTrace();
                return;
            }
        }

        outputStream.writeUTF("-file");
        outputStream.writeUTF(file.getName());
        outputStream.flush();

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
        System.out.println("File sent: " + file.getName());
    }



    private void disconnect() {
        try {
            inputStream.close();
            outputStream.close();

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}
