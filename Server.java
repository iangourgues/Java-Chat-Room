import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.Objects;

public class Server {
    public static void main(String[] args) {
    // List to hold all active client handlers and a list to reuse IDs of disconnected clients
        List<ClientHandler> clientList = new ArrayList<>();
        List<Integer> placeHolderIDs = new ArrayList<>();
        int portNumber = 2158;  // Port number where the server will listen for connections
        ServerSocket serverSocket;

        try {
            serverSocket = new ServerSocket(portNumber);  // Create the server socket
            System.out.println("Listening on: " + portNumber);

            while (true) {  // Main loop to accept new client connections
                Socket client = serverSocket.accept();
                System.out.println("New client connected.");

                synchronized (clientList) {  // Synchronize access to the client list
                    if (placeHolderIDs.isEmpty()) {  // Check if there are available IDs to reuse
                        ClientHandler handler = new ClientHandler(client, clientList, placeHolderIDs);
                        handler.setID(clientList.size());  // Set client ID
                        clientList.add(handler);
                        handler.start();  // Start the client handler thread
                    } else {
                        int reuseID = placeHolderIDs.remove(0);  // Reuse an ID from the placeholder list
                        ClientHandler handler = new ClientHandler(client, clientList, placeHolderIDs);
                        handler.setID(reuseID);
                        clientList.set(reuseID, handler);
                        handler.start();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Could not listen on port: " + portNumber);
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket clientSocket;
    private List<ClientHandler> clientListReference;
    private List<Integer> placeHolderIDsReference;
    private int ID;
    private String clientName = "New connection";  // Default initial client name
    private Scanner in;
    private OutputStream output;
    private PrintWriter chatHistoryWriter;
    private List<String> chatHistory = new ArrayList<>();

    public ClientHandler(Socket socket, List<ClientHandler> clientList, List<Integer> placeHolderList) {
        this.clientSocket = socket;
        this.clientListReference = clientList;
        this.placeHolderIDsReference = placeHolderList;
        try {
            this.in = new Scanner(socket.getInputStream());  // Input stream to read client messages
            this.output = socket.getOutputStream();  // Output stream to send messages to client
            this.chatHistoryWriter = new PrintWriter(new FileWriter("chat_history.txt", true));
        } catch (IOException e) {
            System.err.println("Failed to initialize client streams for " + clientSocket);
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String clientMessage;
            boolean setName = false;  // Flag to check if the client name has been set

            while (true) {  // Continuously read messages from client
                if (in.hasNextLine()) {
                    clientMessage = in.nextLine();
                    System.out.println(clientName + ": " + clientMessage);  // Print client message to server console
                    if (!setName) {
                        clientName = clientMessage.trim();  // Set client's name
                        setName = true;
                        continue;
                    }

                    // Handle different types of messages
                    switch (clientMessage.trim()) {
                        case "/quit":
                            broadcastMessage(clientName + " has left the chat.", -1);
                            return;  // Exit thread after announcing departure
                        case "/list":
                            listActiveClients();  // Send list of active clients to the requester
                            break;
                        default:
                            broadcastMessage(clientMessage, this.ID);  // Broadcast message to all clients
                            break;
                    }
                } else {
                    break;  // Exit if no input detected, indicating client disconnection
                }
            }
        } finally {
            cleanupClient();  // Clean up resources when client disconnects or sends /quit
        }
    }

    private void listActiveClients() {
        try {
            String connectedClients = clientListReference.stream()
                .filter(Objects::nonNull)  // Filter out any null entries
                .map(client -> client.clientName)  // Extract client names
                .collect(Collectors.joining(", "));  // Join names into a comma-separated string

            String response = "Connected clients: " + connectedClients + "\n";
            if (output != null) {
                output.write(response.getBytes());  // Send response to the client
                output.flush();
            }
            System.out.println("Sent to " + clientName + ": " + response);  // Log to server console
        } catch (IOException e) {
            System.err.println("Failed to send list of connected clients to " + clientName);
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String message, int senderID) {
        // Broadcast the message to all other clients except the sender
        for (ClientHandler client : clientListReference) {
            if (client != null && client.getID() != senderID) {  // Ensure the client is not the sender
                client.receiveMessage(clientName + ": " + message, this.ID);
            }
        }

        // Write the message to the server-side chat history
        try (PrintWriter chatHistoryWriter = new PrintWriter(new FileWriter("chat_history.txt", true))) {
            chatHistoryWriter.println(clientName + ": " + message);  // Write to file
        } catch (IOException e) {
            System.err.println("Error writing to chat history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void receiveMessage(String message, int senderID) {
        try {
            if (output != null) {
                output.write((message + "\n").getBytes());
                output.flush();
            }
        } catch (IOException e) {
            System.err.println("Error sending message to client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cleanupClient() {
        try {
            if (clientSocket != null) clientSocket.close();  // Close socket
            if (in != null) in.close();  // Close scanner
            if (output != null) output.close();  // Close output stream
        } catch (IOException e) {
            System.err.println("Error closing resources for " + clientName);
        } finally {
            synchronized (clientListReference) {
                broadcastMessage(clientName + " has left the server.", -1);  // Notify all clients of departure
                clientListReference.set(ID, null);  // Remove client from list
                placeHolderIDsReference.add(ID);  // Recycle the client's ID                  
            }
        }
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }
}