import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;

public class test {

    // This is hardcoded for demonstration purposes. In a real application,
    // credentials should never be hardcoded and should be securely managed.
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "password123";

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/login", new LoginHandler());
        server.createContext("/secret", new SecretHandler());
        server.createContext("/", new DefaultHandler());

        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on port " + port);
        System.out.println("------------------------------------------------------------------");
        System.out.println("To demonstrate the security issue:");
        System.out.println("1. Try to 'log in': http://localhost:" + port + "/login?username=admin&password=password123");
        System.out.println("   (Observe the successful login message.)");
        System.out.println("2. Now access the 'protected' resource directly: http://localhost:" + port + "/secret");
        System.out.println("   (Notice that you gain access without any actual session or authentication check.)");
        System.out.println("The security issue: The /secret endpoint has Broken Access Control. It should be protected,");
        System.out.println("but it lacks actual authentication enforcement, allowing unauthorized access directly.");
        System.out.println("------------------------------------------------------------------");
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            String response = "Invalid credentials.";
            int statusCode = 401; // Unauthorized by default

            String username = params.get("username");
            String password = params.get("password");

            // Simple check against hardcoded credentials
            if (username != null && password != null &&
                username.equals(ADMIN_USER) && password.equals(ADMIN_PASS)) {
                response = "Login successful! You are now 'authenticated' (conceptually).";
                statusCode = 200; // OK
                // In a real application, a secure session token or cookie would be
                // generated and sent to the client here. For this example, we just
                // simulate success without actually establishing a session.
            } else {
                response += " Expected: username=" + ADMIN_USER + ", password=" + ADMIN_PASS;
            }

            sendResponse(exchange, response, statusCode);
        }
    }

    static class SecretHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // THE SECURITY ISSUE: BROKEN ACCESS CONTROL
            // This endpoint is intended to be a "secret" or "protected" resource.
            // However, there is no actual check performed here to verify if the
            // accessing user has successfully logged in, possesses a valid session
            // token, or is authorized to view this content.
            // Any client can directly access this URL without going through the /login
            // endpoint or possessing any form of authentication credentials.

            String response = "This is a secret message! You've accessed a protected resource " +
                              "without proper authentication enforcement. This is the security issue.";
            sendResponse(exchange, response, 200); // Always sends 200 OK, implying open access
        }
    }

    static class DefaultHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Welcome to the insecure server! Instructions are in the console. Try /login or /secret.";
            sendResponse(exchange, response, 200);
        }
    }

    // Helper method to parse query parameters from the URI
    private static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }
        for (String param : query.split("&")) {
            int idx = param.indexOf("=");
            if (idx > 0) {
                result.put(param.substring(0, idx), param.substring(idx + 1));
            } else {
                result.put(param, "");
            }
        }
        return result;
    }

    // Helper method to send HTTP response
    private static void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        byte[] responseBytes = response.getBytes("UTF-8"); // Specify encoding
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}