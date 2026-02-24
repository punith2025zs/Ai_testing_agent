import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class java_server_scm_test {

    private static final int PORT = 8080;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(10); // Simple thread pool for client handlers

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                out.println("Welcome to the Command Execution Server!");
                out.println("Send 'EXECUTE <command>' to run a command.");
                out.println("Type 'QUIT' to disconnect.");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.trim().equalsIgnoreCase("QUIT")) {
                        out.println("Goodbye!");
                        break;
                    } else if (inputLine.startsWith("EXECUTE ")) {
                        String command = inputLine.substring("EXECUTE ".length());
                        out.println("Executing: " + command);
                        String output = executeSystemCommand(command); // THIS IS THE VULNERABLE CALL
                        out.println("--- Command Output ---");
                        out.println(output);
                        out.println("----------------------");
                    } else {
                        out.println("Unknown command. Send 'EXECUTE <command>' or 'QUIT'.");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client " + clientSocket.getInetAddress().getHostAddress() + ": " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }

        // The security vulnerability is within this method: direct use of Runtime.exec(command)
        // without proper sanitization or validation of the user-supplied 'command'.
        private String executeSystemCommand(String command) {
            StringBuilder output = new StringBuilder();
            Process process = null;
            try {
                // VULNERABILITY: Directly executing user-supplied command.
                // An attacker can inject arbitrary commands using shell metacharacters
                // (e.g., "ls -l ; cat /etc/passwd" on Linux/macOS, or "dir & type C:\path\to\file" on Windows).
                process = Runtime.getRuntime().exec(command);

                // Read standard output from the command
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append(System.lineSeparator());
                    }
                }

                // Read error output from the command
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        output.append("ERROR: ").append(line).append(System.lineSeparator());
                    }
                }

                process.waitFor(); // Wait for the command to complete
            } catch (IOException | InterruptedException e) {
                output.append("Failed to execute command: ").append(e.getMessage()).append(System.lineSeparator());
            } finally {
                if (process != null) {
                    process.destroy(); // Ensure the process is terminated
                }
            }
            return output.toString();
        }
    }
}