import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        // Server details
        String serverName = "localhost";  // Address of the server.
        int port = 2158;                  // Port number to connect to.

        // Try-with-resources to ensure all resources are closed properly
        try (Socket socket = new Socket(serverName, port)) {
            System.out.println("Connected to the server at " + serverName + " on port " + port);
            
            // Streams for communication
            InputStream input = socket.getInputStream();  // To read data from the server.
            OutputStream output = socket.getOutputStream();  // To send data to the server.
            Scanner serverMessageScanner = new Scanner(input);  // Scanner to read messages from the server.
            PrintWriter serverOutput = new PrintWriter(output, true);  // PrintWriter to send messages to the server.
            Scanner userInputScanner = new Scanner(System.in);  // Scanner to read user input from the console.

            // Thread to handle incoming server messages
            Thread messageListenerThread = new Thread(() -> {
                while (serverMessageScanner.hasNextLine()) {
                    String messageFromServer = serverMessageScanner.nextLine();  // Read messages from the server
                    System.out.println(messageFromServer);  // Display them to the console
                }
            });
            messageListenerThread.start();  // Start the thread

            // Prompt the user for their name and send it to the server
            System.out.print("Enter your name: ");
            String userName = userInputScanner.nextLine();
            serverOutput.println(userName);  // Send name to the server

            // Main loop to handle sending messages and commands
            while (true) {
                System.out.print("Enter message or command (/list or /quit): ");
                String message = userInputScanner.nextLine();
                serverOutput.println(message);  // Send message or command to the server
                if (message.equalsIgnoreCase("/quit")) {
                    break;  // Exit loop if user types '/quit'
                }
            }

            // Wait for the message listener thread to complete
            messageListenerThread.join();
            serverMessageScanner.close();  // Close server message scanner
            userInputScanner.close();  // Close user input scanner
        } catch (IOException e) {  // Catch IO exceptions from network errors
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {  // Catch interruptions to the message listener thread
            System.err.println("Thread interrupted: " + e.getMessage());
            e.printStackTrace();
        }
    }
}