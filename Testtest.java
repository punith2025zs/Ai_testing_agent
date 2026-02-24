import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class Testtest {

    private static final int SERVER_PORT = 8080;
    private static final String SERVER_BASE_URL = "http://localhost:" + SERVER_PORT;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Testtest...");

        // Start the 'test' server in a separate thread
        Thread serverThread = new Thread(() -> {
            try {
                System.out.println("Test server thread starting...");
                test.main(new String[]{}); // Calls the main method of the 'test' class
                // The server will run until the JVM exits, as its main method doesn't offer a stop mechanism
            } catch (Exception e) {
                System.err.println("Error starting test server: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true); // Allow JVM to exit if only daemon threads remain
        serverThread.start();

        // Give the server a moment to start up
        TimeUnit.SECONDS.sleep(2);
        
        System.out.println("Test server expected to be running. Proceeding with tests...");

        // Test Case 1: Successful Login
        runTest("Successful Login", 
                SERVER_BASE_URL + "/login?username=admin&password=password123", 
                200, 
                "Login successful!");

        // Test Case 2: Failed Login (Bad Password)
        runTest("Failed Login (Bad Password)", 
                SERVER_BASE_URL + "/login?username=admin&password=wrongpass", 
                401, 
                "Invalid credentials.");
        
        // Test Case 3: Failed Login (Bad Username)
        runTest("Failed Login (Bad Username)", 
                SERVER_BASE_URL + "/login?username=user&password=password123", 
                401, 
                "Invalid credentials.");

        // Test Case 4: Direct Access to Secret (Broken Access Control Demonstration)
        // This test case specifically highlights the security issue described in the 'test' class.
        runTest("Direct Secret Access (Broken Access Control)", 
                SERVER_BASE_URL + "/secret", 
                200, 
                "This is a secret message! You've accessed a protected resource without proper authentication enforcement.");

        // Test Case 5: Default Path Access
        runTest("Default Path Access", 
                SERVER_BASE_URL + "/", 
                200, 
                "Welcome to the insecure server!");
        
        System.out.println("All tests completed.");
        // The daemon server thread will terminate automatically when the Testtest.main method exits.
    }

    private static void runTest(String testName, String urlString, int expectedStatusCode, String expectedBodyContains) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds connect timeout
            connection.setReadTimeout(5000);    // 5 seconds read timeout

            int actualStatusCode = connection.getResponseCode();
            StringBuilder responseBody = new StringBuilder();

            // Read response body, handling error streams for non-2xx codes
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    actualStatusCode >= HttpURLConnection.HTTP_BAD_REQUEST 
                    ? connection.getErrorStream() 
                    : connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    responseBody.append(inputLine);
                }
            }

            if (actualStatusCode == expectedStatusCode && responseBody.toString().contains(expectedBodyContains)) {
                System.out.println("PASS: " + testName);
            } else {
                System.out.println("FAIL: " + testName);
                // For a production test, more debug info would be printed here.
                // Keeping output minimal as per "ONLY raw Java code" and specific output format.
            }
            connection.disconnect();

        } catch (Exception e) {
            System.out.println("FAIL: " + testName + " - Exception: " + e.getMessage());
            // Stack trace can be added for deeper debugging during development.
            // e.printStackTrace(); 
        }
    }
}