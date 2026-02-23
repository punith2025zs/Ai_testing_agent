import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Testoddeven {

    public static void main(String[] args) {
        // Store original System.in and System.out before any tests are run
        InputStream originalSystemIn = System.in;
        PrintStream originalSystemOut = System.out;

        try {
            originalSystemOut.println("Starting tests for oddeven class...");

            // Test Case 1: Positive Even Number
            runTest(10, "10 is an even number.", "Positive Even Number Test", originalSystemIn, originalSystemOut);

            // Test Case 2: Positive Odd Number
            runTest(7, "7 is an odd number.", "Positive Odd Number Test", originalSystemIn, originalSystemOut);

            // Test Case 3: Zero
            runTest(0, "0 is an even number.", "Zero Test", originalSystemIn, originalSystemOut);

            // Test Case 4: Negative Even Number
            runTest(-4, "-4 is an even number.", "Negative Even Number Test", originalSystemIn, originalSystemOut);

            // Test Case 5: Negative Odd Number
            runTest(-9, "-9 is an odd number.", "Negative Odd Number Test", originalSystemIn, originalSystemOut);

            originalSystemOut.println("Finished all tests for oddeven class.");

        } catch (Exception e) {
            // This catch block is for exceptions that occur outside of a specific test case,
            // e.g., if the test runner itself fails to set up or tear down.
            originalSystemOut.println("FAIL: An unexpected error occurred during test suite execution.");
            e.printStackTrace(originalSystemOut);
        } finally {
            // Ensure original System.in and System.out are restored even if an exception occurs
            // that prevents individual test cases from cleaning up properly.
            System.setIn(originalSystemIn);
            System.setOut(originalSystemOut);
        }
    }

    /**
     * Executes a single test case for the oddeven program.
     * It redirects System.in and System.out, calls oddeven.main(),
     * captures the output, and then restores System.in and System.out.
     *
     * @param inputNumber The integer to provide as input to the oddeven program.
     * @param expectedResultPhrase The specific part of the expected output (e.g., "10 is an even number.").
     * @param testName A descriptive name for the test case.
     * @param originalSystemIn A reference to the original System.in to restore after the test.
     * @param originalSystemOut A reference to the original System.out to print test results to.
     */
    private static void runTest(int inputNumber, String expectedResultPhrase, String testName,
                                InputStream originalSystemIn, PrintStream originalSystemOut) {

        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        
        // Prepare the input string with the number and a system-specific line separator
        String input = String.valueOf(inputNumber) + System.lineSeparator();
        ByteArrayInputStream testInputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        
        // Create a PrintStream to capture output to our ByteArrayOutputStream
        PrintStream testPrintStream = new PrintStream(capturedOutput, true, StandardCharsets.UTF_8);

        // Temporarily redirect System.in and System.out
        System.setIn(testInputStream);
        System.setOut(testPrintStream);

        try {
            // Call the main method of the oddeven class
            oddeven.main(null);

            // Get the captured output as a String using UTF-8 encoding
            String actualOutput = capturedOutput.toString(StandardCharsets.UTF_8.name());

            // Construct the full expected output, including the prompt and line separator
            String expectedOutputPrefix = "Enter an integer: ";
            String expectedFullOutput = expectedOutputPrefix + expectedResultPhrase + System.lineSeparator();

            // Perform the assertion: compare actual output with expected output after trimming whitespace
            if (actualOutput.trim().equals(expectedFullOutput.trim())) {
                originalSystemOut.println("PASS: " + testName + " (Input: " + inputNumber + ")");
            } else {
                originalSystemOut.println("FAIL: " + testName + " (Input: " + inputNumber + ")");
                originalSystemOut.println("      Expected (trimmed): \"" + expectedFullOutput.trim() + "\"");
                originalSystemOut.println("      Actual (trimmed):   \"" + actualOutput.trim() + "\"");
            }
        } catch (Exception e) {
            // Catch any unexpected exceptions during the execution of oddeven.main()
            originalSystemOut.println("FAIL: " + testName + " (Input: " + inputNumber + ") - Exception during test execution: " + e.getMessage());
            e.printStackTrace(originalSystemOut); // Print stack trace to the original System.out
        } finally {
            // CRITICAL: Restore System.in and System.out immediately after this test completes.
            // The oddeven.main() method calls scanner.close(), which closes the currently set System.in.
            // If not restored, subsequent tests would try to use a closed stream.
            System.setIn(originalSystemIn);
            System.setOut(originalSystemOut);
        }
    }
}