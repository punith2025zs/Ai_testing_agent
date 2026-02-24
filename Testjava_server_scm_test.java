import java.io.*;
import java.net.*;

public class Testjava_server_scm_test {

    private static final int PORT = 8080;
    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) throws InterruptedException {
        // Start the server in a separate thread.
        // This allows the test client to connect to it.
        Thread serverThread = new Thread(() -> {
            try {
                // Execute the main method of the server class
                java_server_scm_test.main(new String[]{});
            } catch (Exception e) {
                System.err.println("Server thread encountered an error: " + e.getMessage());
            }
        });
        serverThread.start();

        System.out.println("Starting server and waiting 2 seconds for it to bind to port " + PORT + "...");
        // Give the server a moment to start up and listen on the port
        Thread.sleep(2000); 
        System.out.println("Server should be up. Running tests...");

        // Run the 5 test cases demonstrating command execution and injection.
        // These tests focus on the identified "VULNERABILITY: Directly executing user-supplied command"
        // in the java_server_scm_test class.
        testBasicEcho();
        testSemicolonInjection();
        testLogicalANDInjection();
        testLogicalORInjection();
        testFileContentAccessInjection();

        System.out.println("\n--- Test Summary ---");
        System.out.println("Total tests run: " + testsRun);
        System.out.println("Tests passed: " + testsPassed);
        System.out.println("Tests failed: " + (testsRun - testsPassed));

        // Note: The server thread (which contains a while(true) loop) will continue to run
        // in the background even after the test client finishes. For this simple test,
        // the JVM exiting will implicitly terminate the server thread. For a more robust
        // test setup, the server would typically expose a clean shutdown mechanism.
    }

    /**
     * Helper method to record and print the result of a test.
     */
    private static void recordResult(String testName, boolean passed, String command, String fullResponse, String... expectedOutputs) {
        if (passed) {
            System.out.println("PASS: " + testName);
            testsPassed++;
        } else {
            System.out.println("FAIL: " + testName);
            System.out.println("  Command sent: '" + command + "'");
            System.out.print("  Expected to contain: ");
            for (String expected : expectedOutputs) {
                System.out.print("'" + expected + "' ");
            }
            System.out.println("\n  Actual full response from server (Command Output section):");
            if (fullResponse == null) {
                System.out.println("[Error: Could not retrieve response]");
            } else {
                System.out.println(fullResponse.trim().isEmpty() ? "[Empty output]" : fullResponse);
            }
        }
        testsRun++;
    }

    /**
     * Test Case 1: Verifies basic command execution works as expected.
     */
    private static void testBasicEcho() {
        String testName = "Test_1_BasicEchoCommand";
        String command = "echo Hello World from Test";
        String expected = "Hello World from Test";
        String response = getCommandOutput(testName, command);
        boolean passed = response != null && response.contains(expected);
        recordResult(testName, passed, command, response, expected);
    }

    /**
     * Test Case 2: Demonstrates command injection using a semicolon (;) to execute multiple commands.
     * This is a common shell metacharacter for chaining commands on Unix-like systems.
     * On Windows cmd.exe, '&' is often used for similar chaining.
     */
    private static void testSemicolonInjection() {
        String testName = "Test_2_SemicolonInjection";
        String command = "echo First Output ; echo Second Output";
        String expected1 = "First Output";
        String expected2 = "Second Output";
        String response = getCommandOutput(testName, command);
        boolean passed = response != null && response.contains(expected1) && response.contains(expected2);
        recordResult(testName, passed, command, response, expected1, expected2);
    }

    /**
     * Test Case 3: Demonstrates command injection using logical AND (&&).
     * The second command is executed only if the first one succeeds.
     * Works on both Unix-like systems and Windows (cmd.exe).
     */
    private static void testLogicalANDInjection() {
        String testName = "Test_3_LogicalANDInjection";
        String command = "echo PartA && echo PartB";
        String expected1 = "PartA";
        String expected2 = "PartB";
        String response = getCommandOutput(testName, command);
        boolean passed = response != null && response.contains(expected1) && response.contains(expected2);
        recordResult(testName, passed, command, response, expected1, expected2);
    }

    /**
     * Test Case 4: Demonstrates command injection using logical OR (||).
     * The second command is executed only if the first one fails.
     * 'false' is a common Unix command that exits with a non-zero status, indicating failure.
     * On Windows, one might use 'cmd /c exit 1' or a non-existent command for a similar effect.
     */
    private static void testLogicalORInjection() {
        String testName = "Test_4_LogicalORInjection";
        String command = "false || echo FallbackCommandExecuted";
        String expected = "FallbackCommandExecuted";
        String response = getCommandOutput(testName, command);
        boolean passed = response != null && response.contains(expected);
        recordResult(testName, passed, command, response, expected);
    }
    
    /**
     * Test Case 5: Demonstrates accessing system file content via command injection.
     * This command attempts to read '/etc/hosts' on Unix-like systems, which typically contains "localhost".
     * On Windows, an equivalent would be 'type %WINDIR%\\System32\\drivers\\etc\\hosts'.
     */
    private static void testFileContentAccessInjection() {
        String testName = "Test_5_FileContentAccessInjection";
        String command = "cat /etc/hosts";
        String expected = "localhost"; // Expected content in /etc/hosts
        String response = getCommandOutput(testName, command);
        boolean passed = response != null && response.contains(expected);
        recordResult(testName, passed, command, response, expected);
    }

    /**
     * Connects to the server, sends a command, and returns the content
     * found specifically within the "--- Command Output ---" section.
     * Returns null if there's a connection error or a major parsing issue,
     * or if the output section markers are not found.
     */
    private static String getCommandOutput(String testName, String commandToSend) {
        StringBuilder commandOutput = new StringBuilder();
        try (Socket clientSocket = new Socket("localhost", PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Read and discard initial welcome messages (expecting 3 lines).
            // This is a simplified client; a more robust client might parse these or wait for a specific prompt.
            for (int i = 0; i < 3; i++) {
                // Ensure a line is actually available before reading to prevent blocking indefinitely if server is slow.
                if (!in.ready() && System.currentTimeMillis() - clientSocket.getCreationTime() > 5000) {
                     System.err.println("WARN: Timeout waiting for server welcome messages for " + testName);
                     return null; // Server didn't send expected welcome in time
                }
                in.readLine(); 
            }

            out.println("EXECUTE " + commandToSend);

            String line;
            long startTime = System.currentTimeMillis();
            boolean inOutputSection = false;
            
            // Read server response until the end marker "----------------------" or timeout
            while ((line = in.readLine()) != null) {
                if (line.equals("--- Command Output ---")) {
                    inOutputSection = true;
                    continue; // Start collecting output from the next line
                } else if (line.equals("----------------------")) {
                    inOutputSection = false;
                    break; // End of command output section
                }

                if (inOutputSection) {
                    commandOutput.append(line).append(System.lineSeparator());
                }

                // Timeout for reading the entire response for this command
                if (System.currentTimeMillis() - startTime > 5000) { 
                    System.err.println("WARN: Timeout reading server response for " + testName + " (command: " + commandToSend + ")");
                    break;
                }
            }
            
            out.println("QUIT"); // Send QUIT command to gracefully close connection
            in.readLine(); // Read "Goodbye!" message

        } catch (IOException e) {
            System.err.println("ERROR: " + testName + " - Client connection or communication error for command '" + commandToSend + "': " + e.getMessage());
            return null; // Indicate failure to get output
        }
        return commandOutput.toString();
    }
}