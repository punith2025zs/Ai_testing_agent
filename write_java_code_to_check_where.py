"""
This Python script generates Java code to determine if a given number is odd or even.
The generated Java code will expect a number as a command-line argument.
"""

def generate_java_odd_even_checker():
    """
    Generates and returns a string containing Java code for checking if a number is odd or even.
    The Java code includes a main method that expects a single command-line argument.
    """
    java_code = """
import java.util.Scanner; // Not used in this version, but often useful for input

/**
 * This Java class checks if a number provided as a command-line argument is odd or even.
 * If no argument is provided or if it's not a valid integer, it prints an error message.
 *
 * To compile: javac OddEvenChecker.java
 * To run with an even number: java OddEvenChecker 10
 * To run with an odd number: java OddEvenChecker 7
 */
public class OddEvenChecker {

    public static void main(String[] args) {
        // Check if an argument was provided
        if (args.length == 0) {
            System.out.println("Usage: java OddEvenChecker <number>");
            System.out.println("Please provide an integer as a command-line argument.");
            return; // Exit the program
        }

        int number;
        try {
            // Attempt to parse the first command-line argument to an integer
            number = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            // Handle cases where the argument is not a valid integer
            System.out.println("Invalid input: '" + args[0] + "' is not a valid integer.");
            System.out.println("Please provide a valid integer.");
            return; // Exit the program
        }

        // Check if the number is even or odd using the modulo operator
        if (number % 2 == 0) {
            System.out.println("The number " + number + " is Even.");
        } else {
            System.out.println("The number " + number + " is Odd.");
        }
    }
}
"""
    return java_code

if __name__ == "__main__":
    # Print the generated Java code directly to standard output
    # The user can then redirect this output to a .java file, e.g.,
    # python write_java_code_to_check_where.py > OddEvenChecker.java
    print(generate_java_odd_even_checker())