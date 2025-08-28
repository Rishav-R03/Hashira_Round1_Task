import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This program is written to solve a polynomial equation.
 * The tasks given are:
 * 1.Calling a json file
 * 2.Decoding the y values from the file
 * 3.Use of LIF
 * 
 * Note:
 * 1.Not used python
 * 2.Not copy pasted code
 * 3.Not hardcoded jsonfile
 */
public class PolynomialSecretSolver {

    /**
     * Decodes a string value from a given base to a BigInteger.(since values in
     * test case 2 is too large.)
     * 
     * @param value The string representation of the number.
     * @param base  The base of the number.
     * @return The integer value as a long.
     */
    public static long decodeValue(String value, int base) {
        // Long.parseLong can handle larger bases and values than Integer.parseInt
        return Long.parseLong(value, base);
    }

    /**
     * Reads the entire content of a JSON file into a single string.
     *
     * @param filename The path to the JSON file.
     * @return The content of the file as a string.
     * @throws Exception if there is an issue reading the file.
     */
    public static String readJsonFile(String filename) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Correctly parses a single JSON object string into a nested Map structure.
     * This method uses a regular expression to match a key followed by a nested
     * object, which is a more robust way to handle the given JSON structure.
     *
     * @param objStr The string content of a single JSON object.
     * @return A Map representing the parsed JSON data for one test case.
     */
    public static Map<String, Map<String, String>> parseSingleObject(String objStr) {
        Map<String, Map<String, String>> data = new HashMap<>();

        // A more reliable regex to capture keys and their nested object content.
        // It looks for a key in quotes followed by a colon and a brace-enclosed object.
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{([^\\}]+)\\}");
        Matcher matcher = pattern.matcher(objStr);

        while (matcher.find()) {
            String topLevelKey = matcher.group(1);
            String innerContent = matcher.group(2);

            Map<String, String> innerMap = new HashMap<>();
            // Regex to find key-value pairs inside the inner object
            Pattern innerKvPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"?([^\",\"]+)\"?");
            Matcher innerKvMatcher = innerKvPattern.matcher(innerContent);

            while (innerKvMatcher.find()) {
                String key = innerKvMatcher.group(1);
                String value = innerKvMatcher.group(2);
                innerMap.put(key, value);
            }
            data.put(topLevelKey, innerMap);
        }
        return data;
    }

    /**
     * Solves for the secret constant 'c' (the y-intercept) using
     * Lagrange interpolation.
     *
     * @param xs A list of x-coordinates.
     * @param ys A list of y-coordinates (decoded values).
     * @return The calculated secret, rounded to the nearest integer.
     */
    public static long computeSecretC(List<Integer> xs, List<Long> ys) {
        int k = xs.size();
        double c = 0.0;
        for (int i = 0; i < k; i++) {
            double term = ys.get(i);
            for (int j = 0; j < k; j++) {
                if (j != i) {
                    term *= (0.0 - xs.get(j)) / (double) (xs.get(i) - xs.get(j));
                }
            }
            c += term;
        }
        return (long) Math.round(c);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java PolynomialSecretSolver <input.json>");
            return;
        }
        String filename = args[0];
        try {
            String jsonString = readJsonFile(filename);

            // Remove all whitespace to simplify parsing
            String cleanedJson = jsonString.replaceAll("\\s", "");

            // Remove outer array brackets and split by object boundaries
            String content = cleanedJson.substring(1, cleanedJson.length() - 1);
            String[] testCaseStrings = content.split("\\}(,)?\\{");

            for (int i = 0; i < testCaseStrings.length; i++) {
                // Re-add the brackets to make each string a valid object string
                String testCaseStr = "{" + testCaseStrings[i] + "}";

                System.out.println("--- Processing Test Case " + (i + 1) + " ---");
                Map<String, Map<String, String>> data = parseSingleObject(testCaseStr);

                // Extract the 'n' and 'k' values
                Map<String, String> keys = data.get("keys");
                // Check if keys object was correctly parsed
                if (keys == null) {
                    System.err.println("Error: Could not parse 'keys' for this test case.");
                    continue;
                }
                int k = Integer.parseInt(keys.get("k"));

                // Prepare lists for x and y values
                List<Integer> xs = new ArrayList<>();
                List<Long> ys = new ArrayList<>();

                // Collect roots from the parsed data
                for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {
                    String key = entry.getKey();
                    if (key.equals("keys")) {
                        continue;
                    }
                    try {
                        int x = Integer.parseInt(key);
                        Map<String, String> rootObj = entry.getValue();
                        String baseStr = rootObj.get("base");
                        String valueStr = rootObj.get("value");

                        int base = Integer.parseInt(baseStr);
                        long y = decodeValue(valueStr, base);

                        xs.add(x);
                        ys.add(y);
                        System.out.println("Decoded Root: x = " + x + ", y = " + y);
                    } catch (NumberFormatException | NullPointerException e) {
                        System.err.println("Skipping malformed root: " + key);
                    }
                }

                // Use only the first k points
                if (xs.size() > k) {
                    xs = xs.subList(0, k);
                    ys = ys.subList(0, k);
                }

                // Calculate the secret 'c'
                long secretC = computeSecretC(xs, ys);
                System.out.println("The calculated secret (c) is: " + secretC);
                System.out.println("------------------------------------");
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
