import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ChatGUI extends Application {
    // GUI components
    private TextArea chatArea;  // Area where chat messages are displayed
    private TextField messageField;  // Input field for writing messages
    private Button sendButton;  // Button to send messages
    private Socket socket;  // Socket for network communication
    private BufferedReader reader;  // To read messages from the server
    private BufferedWriter writer;  // To send messages to the server
    private TextField nameField;  // To input the user's name before connecting
    private Button connectButton;  // Button to start the connection

    @Override
    public void start(Stage primaryStage) {
        // Initialize components
        chatArea = new TextArea();
        messageField = new TextField();
        sendButton = new Button("Send");
        nameField = new TextField();
        connectButton = new Button("Connect");

        // Setup properties for components
        chatArea.setEditable(false);  // Prevents user from editing chat history
        chatArea.setWrapText(true);  // Wraps text in the TextArea
        messageField.setPromptText("Enter message...");  // Placeholder text
        nameField.setPromptText("Enter your name...");  // Placeholder text
        sendButton.setOnAction(event -> sendMessage());  // Event handler for sending messages
        connectButton.setOnAction(event -> connectToServer("localhost", 2158));  // Event handler for connecting to server

        // Layout setup using VBox for vertical alignment
        VBox layout = new VBox(10, nameField, connectButton, chatArea, messageField, sendButton);
        Scene scene = new Scene(layout, 400, 500);  // Scene with predefined size

        // Stage setup
        primaryStage.setTitle("Chat Application");  // Title of the window
        primaryStage.setScene(scene);  // Assign the scene to the stage
        primaryStage.show();  // Display the stage
    }

    private void connectToServer(String host, int port) {
        try {
            socket = new Socket(host, port);  // Connect to the server
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String userName = nameField.getText().trim();  // Get the username from input field
            if (!userName.isEmpty()) {
                writer.write(userName + "\n");  // Send the username to the server
                writer.flush();  // Flush the stream to ensure the message is sent
            }
            chatArea.appendText("Connected as: " + userName + "\n");  // Confirm connection in GUI
            startMessageReader();  // Start a new thread to read messages from server
            connectButton.setDisable(true);  // Disable connect button after connection
            nameField.setDisable(true);  // Disable name field after sending name
        } catch (IOException e) {
            System.err.println("Could not connect to the server: " + e.getMessage());
            chatArea.appendText("Failed to connect to the server.\n");  // Display connection error
        }
    }

    private void startMessageReader() {
        Thread thread = new Thread(() -> {
            try {
                String messageLine;
                while ((messageLine = reader.readLine()) != null) {  // Continuously read messages from server
                    final String message = messageLine;
                    Platform.runLater(() -> chatArea.appendText(message + "\n"));  // Update chat area with received messages
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    chatArea.appendText("Server connection lost.\n");  // Notify user of lost connection
                    connectButton.setDisable(false);  // Re-enable connection button
                    nameField.setDisable(false);  // Re-enable name field
                });
            }
        });
        thread.setDaemon(true);  // Set thread as daemon so it doesn't prevent application exit
        thread.start();  // Start the thread
    }

    private void sendMessage() {
        String message = messageField.getText().trim();  // Get text from message field
        if (!message.isEmpty()) {
            try {
                writer.write(message + "\n");  // Send message to server
                writer.flush();  // Flush the stream to ensure the message is sent
                // Display the message in the chat area immediately after sending
                Platform.runLater(() -> chatArea.appendText("You: " + message + "\n"));
                messageField.clear();  // Clear input field after message is sent
            } catch (IOException e) {
                System.err.println("Error sending message: " + e.getMessage());
                Platform.runLater(() -> chatArea.appendText("Failed to send message: " + e.getMessage() + "\n"));
            }
        }
    }

    public static void main(String[] args) {
        launch(args);  // Launch the application
    }
}
