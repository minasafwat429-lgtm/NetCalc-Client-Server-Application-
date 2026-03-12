import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Date;

/**
 * CalculatorServer
 * 
 * A TCP Server that accepts connections from clients, receives calculation requests,
 * computes the result, and sends it back.
 * GUI Requirements:
 * - Shows Server Status (Running/Waiting)
 * - Logs all activities
 */
public class CalculatorServer extends JFrame {

    // GUI Components
    private JTextArea logArea;
    private JLabel statusLabel;
    private JButton startButton;
    
    // Networking
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private static final int PORT = 5000;

    public CalculatorServer() {
        super("TCP Calculator Server");
        setupGUI();
    }

    private void setupGUI() {
        // Window setup
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Top Panel: Status and Control
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.RED);
        
        startButton = new JButton("Start Server");
        startButton.addActionListener(e -> startServer());

        topPanel.add(startButton);
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        // Center Panel: Logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Server Logs"));
        add(scrollPane, BorderLayout.CENTER);

        // Center on screen
        setLocationRelativeTo(null);
    }

    private void startServer() {
        if (isRunning) return;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;
                
                // Update GUI
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Status: Running on Port " + PORT);
                    statusLabel.setForeground(Color.GREEN);
                    startButton.setEnabled(false);
                    appendLog("Server started at " + new Date());
                    appendLog("Waiting for clients...");
                });

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    appendLog("New client connected: " + clientSocket.getInetAddress());
                    
                    // Handle client in a separate thread
                    new ClientHandler(clientSocket).start();
                }

            } catch (IOException e) {
                appendLog("Server Error: " + e.getMessage());
            }
        }).start();
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + message + "\n");
            // Auto-scroll to bottom
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // Inner class to handle each client connection
    private class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Setup streams
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String request;
                while ((request = in.readLine()) != null) {
                    appendLog("Received request from " + socket.getPort() + ": " + request);
                    
                    String response = processRequest(request);
                    out.println(response);
                    
                    appendLog("Sent response to " + socket.getPort() + ": " + response);
                }

            } catch (IOException e) {
                appendLog("Client " + socket.getPort() + " disconnected.");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String processRequest(String request) {
            // Protocol: "OPERATION|NUM1|NUM2" (e.g., ADD|5|10)
            try {
                String[] parts = request.split("\\|");
                if (parts.length != 3) {
                    return "ERROR|Invalid Request Format";
                }

                String operation = parts[0];
                double num1 = Double.parseDouble(parts[1]);
                double num2 = Double.parseDouble(parts[2]);
                double result = 0;

                switch (operation) {
                    case "ADD": result = num1 + num2; break;
                    case "SUB": result = num1 - num2; break;
                    case "MUL": result = num1 * num2; break;
                    case "DIV": 
                        if (num2 == 0) return "ERROR|Division by Zero";
                        result = num1 / num2; 
                        break;
                    default: return "ERROR|Unknown Operation";
                }

                return "OK|" + result;

            } catch (NumberFormatException e) {
                return "ERROR|Invalid Numbers";
            } catch (Exception e) {
                return "ERROR|" + e.getMessage();
            }
        }
    }

    public static void main(String[] args) {
        // Ensure GUI runs on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            new CalculatorServer().setVisible(true);
        });
    }
}
