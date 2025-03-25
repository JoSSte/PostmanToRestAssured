package dk.kodeninjaer.testing.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class to generate a test class from a Postman collection
 */
public class PostmanToRestAssuredGenerator {
    /**
     * Logger to log messages during generation
     */
    private static final Logger logger = LoggerFactory.getLogger(PostmanToRestAssuredGenerator.class);
    /**
     * Output package to store the generated test class in
     */
    private final String outputPackage;
    /**
     * Output class name to store the generated test class
     */
    private final String outputClassName;
    /**
     * Base path to store the generated test class
     */
    public static final String OUTPUT_BASEPATH = "build/generated/";
    /**
     * List to store collection variables
     */    
    List<CollectionVariable> collectionVariables;

    /**
     * Constructor to initialize the output package and class name
     * @param outputPackage
     * @param outputClassName
     */
    public PostmanToRestAssuredGenerator(String outputPackage, String outputClassName) {
        this.outputPackage = outputPackage;
        this.outputClassName = outputClassName;
    }

    /**
     * Main method to generate a test class
     * @param postmanCollectionPath
     * @throws IOException
     * @throws JsonProcessingException
     */
    public void generate(String postmanCollectionPath) throws IOException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode collection = mapper.readTree(new File(postmanCollectionPath));

        String baseUrl = collection.path("info").path("url").asText("");
        List<TestCase> testCases = parseItems(collection.path("item"));
        parseCollectionVariables(collection.path("variable"));

        generateTestClass(baseUrl, testCases);
    }

    /**
     * Method to parse collection variables and save them in the collectionVariables list
     * @param variables
     */
    private void parseCollectionVariables(JsonNode variables) {
        collectionVariables = new ArrayList<>();
        for (JsonNode variable : variables) {
            collectionVariables.add(new CollectionVariable(variable.path("key").asText(), variable.path("value").asText()));
        }
        // log collection variables as key=value pairs
        String collectionVariableList = collectionVariables.stream()
                .map(v -> v.key + "=" + v.value)
                .collect(Collectors.joining(", "));
        logger.info("Collection variables: {}", collectionVariableList);
    }

    /**
     * Method to parse items recursively
     * @param items
     * @return
     * @throws JsonProcessingException
     */
    private List<TestCase> parseItems(JsonNode items) throws JsonProcessingException {
        List<TestCase> testCases = new ArrayList<>();
        parseItemsRecursive(items, "", testCases);
        return testCases;
    }

    /**
     * Method to parse items recursively
     * @param items
     * @param folderPath
     * @param testCases
     * @throws JsonProcessingException
     */
    private void parseItemsRecursive(JsonNode items, String folderPath, List<TestCase> testCases) throws JsonProcessingException {
        for (JsonNode item : items) {
            if (item.has("request")) {
                TestCase testCase = new TestCase();
                JsonNode request = item.path("request");

                // Combine folder path with request name for the test method name
                String requestName = item.path("name").asText();
                testCase.name = folderPath.isEmpty() ? requestName : folderPath + "_" + requestName;
                testCase.method = request.path("method").asText();
                testCase.url = request.path("url").path("raw").asText();

                // Parse headers
                JsonNode headers = request.path("header");
                for (JsonNode header : headers) {
                    testCase.headers.put(
                        header.path("key").asText(),
                        header.path("value").asText()
                    );
                }

                // Parse body if exists
                if (request.has("body")) {
                    testCase.body = request.path("body").path("raw").asText();
                }

                // Parse pre-request scripts
                // TODO: check if there is any pm.collectionVariables.set() in the script and
                // add them to the collectionVariables list
                // TODO: check if there is any pm.environment.set() in the script and add them
                // to the collectionVariables list (we will expect Environment only variables to
                // be set independently)
                JsonNode events = item.path("event");
                for (JsonNode event : events) {
                    String listen = event.path("listen").asText();
                    String script = (new ObjectMapper()).writeValueAsString(event.path("script").path("exec"));

                    if ("prerequest".equals(listen)) {
                        testCase.preRequestScript = parseScript(script);
                    } else if ("test".equals(listen)) {
                        testCase.testScript = parseAssertions(script);
                    }
                }

                testCases.add(testCase);
            } else if (item.has("item")) {
                // This is a folder, recursively process its items
                String newFolderPath = folderPath.isEmpty() ?
                    item.path("name").asText() :
                    folderPath + "_" + item.path("name").asText();
                parseItemsRecursive(item.path("item"), newFolderPath, testCases);
            }
        }
    }

    /**
     * Method to parse a script and return a list of the commands
     * @param script
     * @return
     */
    private List<ScriptCommand> parseScript(String script) {
        List<ScriptCommand> commands = new ArrayList<>();
        java.util.regex.Matcher matcher = Patterns.ENVIRONMENT_SET.matcher(script);

        while (matcher.find()) {
            ScriptCommand cmd = new ScriptCommand();
            cmd.type = "SET_ENV";
            cmd.key = matcher.group(1);
            cmd.value = matcher.group(2);
            cmd.originalScript = matcher.group(0);
            commands.add(cmd);
        }
        return commands;
    }

    /**
     * Method to parse assertions
     * @param script
     * @return
     */
    private List<Assertion> parseAssertions(String script) {
        if (script == null || script.isEmpty()) {
            logger.error("Empty test script found");
        }
        List<Assertion> assertions = new ArrayList<>();

        // Parse pm.expect assertions
        Pattern expectPattern = Pattern.compile(
                "pm\\.expect\\((.*?)\\)\\.to\\.(.*?)\\((.*?)\\)|pm\\.response\\.to\\.have\\.status\\((\\d+)\\)");
        java.util.regex.Matcher expectMatcher = expectPattern.matcher(script);

        while (expectMatcher.find()) {
            Assertion assertion = new Assertion();
            assertion.type = "expect";

            if (expectMatcher.group(4) != null) {
                // pm.response.to.have.status(200)
                assertion.matcher = "have.status";
                assertion.expected = expectMatcher.group(4);
            } else {
                // pm.expect(jsonData.json.message).to.equal("Hello World")
                assertion.actual = expectMatcher.group(1);
                assertion.matcher = expectMatcher.group(2);
                assertion.expected = expectMatcher.group(3);
            }
            // TODO: check for collection gets and sets and substitute them with environment
            // variables
            assertions.add(assertion);
        }
        if (assertions.isEmpty()) {
            logger.error("No assertions found in script: " + script);
        }

        // Parse pm.test assertions
        java.util.regex.Matcher testMatcher = Patterns.TEST.matcher(script);

        while (testMatcher.find()) {
            String description = testMatcher.group(1);
            String testScript = testMatcher.group(2);
            String originalScript = testMatcher.group(0);

            // Convert common pm.test assertions to RestAssured format
            if (testScript.contains("pm.response.to.have.status")) {
                Assertion assertion = new Assertion();
                assertion.type = "expect";
                assertion.matcher = "have.status";
                assertion.expected = testScript.replaceAll(".*pm\\.response\\.to\\.have\\.status\\((\\d+)\\).*", "$1");
                assertion.originalScript = originalScript;
                assertions.add(assertion);
            }

            if (testScript.contains("pm.response.to.be.json")) {
                Assertion assertion = new Assertion();
                assertion.type = "expect";
                assertion.matcher = "contentType";
                assertion.expected = "\"application/json\"";
                assertion.originalScript = originalScript;
                assertions.add(assertion);
            }

            // Add more pm.test conversions as needed
        }

        return assertions;
    }

    /**
     * Method to generate a test class file in the output directory
     * @param baseUrl
     * @param testCases
     * @throws IOException
     */
    private void generateTestClass(String baseUrl, List<TestCase> testCases) throws IOException {
        String outputPath = OUTPUT_BASEPATH + outputPackage.replace('.', '/') + "/" + outputClassName + ".java";
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(outputFile)) {
            // Write package and imports
            writer.write("package " + outputPackage + ";\n\n");
            writer.write("import io.restassured.builder.RequestSpecBuilder;\n");
            writer.write("import io.restassured.response.Response;\n");
            writer.write("import io.restassured.specification.RequestSpecification;\n");
            writer.write("import org.junit.jupiter.api.BeforeAll;\n");
            writer.write("import org.junit.jupiter.api.Test;\n");
            writer.write("import static io.restassured.RestAssured.given;\n");
            writer.write("import static org.hamcrest.Matchers.*;\n");
            writer.write("import org.slf4j.Logger;\n");
            writer.write("import org.slf4j.LoggerFactory;\n\n");
            writer.write("import java.util.*;\n\n");

            // Write class declaration
            writer.write("public class " + outputClassName + " {\n");
            writer.write("    private static final Logger logger = LoggerFactory.getLogger(" + outputClassName + ".class);\n");
            writer.write("    private static RequestSpecification requestSpec;\n");
            writer.write("    private static Map<String, String> environment = new HashMap<>();\n\n");
            writer.write("    private static Map<String, String> collectionVariables = new HashMap<>();\n\n");

            // Write setup method
            writer.write("    @BeforeAll\n");
            writer.write("    public static void setup() {\n");
            writer.write("        requestSpec = new RequestSpecBuilder()\n");
            writer.write("            .setBaseUri(\"" + baseUrl + "\")\n");
            writer.write("            .build();\n");
            // Write collection variables
            for (CollectionVariable variable : collectionVariables) {
                writer.write("        collectionVariables.put(\"" + variable.key + "\", \"" + variable.value + "\");\n");
            }
            writer.write("    }\n\n");

            // Write test methods
            for (TestCase test : testCases) {
                generateTestMethod(writer, test);
            }

            writer.write("}\n");
        }
    }

    /**
     * Method to generate a test method
     * @param writer
     * @param test
     * @throws IOException
     */
    private void generateTestMethod(FileWriter writer, TestCase test) throws IOException {
        String methodName = test.name.replaceAll("[^a-zA-Z0-9]", "_");
        writer.write("    @Test\n");
        writer.write("    public void " + methodName + "() {\n");

        // Write original scripts as comments
        if (!test.preRequestScript.isEmpty()) {
            writer.write("        // Pre-request script:\n");
            writer.write("        /*\n");
            for (ScriptCommand cmd : test.preRequestScript) {
                writer.write("        " + cmd.originalScript + "\n");
            }
            writer.write("        */\n\n");
        }

        // Write pre-request script execution
        for (ScriptCommand cmd : test.preRequestScript) {
            if ("SET_ENV".equals(cmd.type)) {
                writer.write("        environment.put(\"" + cmd.key + "\", " + cmd.value + ");\n");
            }
        }

        // Write request execution
        writer.write("        RequestSpecification spec = given(requestSpec)\n");
        writer.write("            .headers(" + generateMap(test.headers) + ");\n\n");

        if (test.body != null) {

            // Escape quotes and newlines in the JSON body
            String escapedBody = test.body.replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
            Matcher bodyVarMatcher = Patterns.VARIABLE.matcher(escapedBody);
            while (bodyVarMatcher.find()) {
                String variableName = bodyVarMatcher.group().substring(2, bodyVarMatcher.group().length() - 2);
                escapedBody = escapedBody.replace(bodyVarMatcher.group(),
                        "\" + collectionVariables.get(\"" + variableName + "\") + \"");
            }
            writer.write("        spec.body(\"" + escapedBody + "\");\n\n");
        }
        Matcher urlVarMatcher = Patterns.VARIABLE.matcher(test.url);
        while (urlVarMatcher.find()) {
            String variableName = urlVarMatcher.group().substring(2, urlVarMatcher.group().length() - 2);
            test.url = test.url.replace(urlVarMatcher.group(),
                    "\" + collectionVariables.get(\"" + variableName + "\") + \"");
        }
        writer.write(
                "        Response response = spec.when()." + test.method.toLowerCase() + "(\"" + test.url + "\");\n\n");

        // Write assertions
        if (!test.testScript.isEmpty()) {
            writer.write("\n        // Test script:\n");
            writer.write("        /*\n");
            for (Assertion assertion : test.testScript) {
                writer.write("        " + assertion.originalScript + "\n");
            }
            writer.write("        */\n\n");
        }

        for (Assertion assertion : test.testScript) {
            switch (assertion.matcher) {
                case "equal":
                    // Extract JSON path from the actual value (remove jsonData prefix)
                    String jsonPath = assertion.actual.replace("jsonData.", "");
                    // Unescape quotes in the expected value
                    String unescapedExpected = assertion.expected.replace("\\\"", "\"");
                    //TODO: Fix conditioIf the expected value contains pm. we will just put it in a string since we have a bracket matchin issue.
                    if(unescapedExpected.contains("pm.")) {
                        unescapedExpected = "\"/*" + assertion.expected + "*/\"";
                    }
                    writer.write("        response.then().body(\"" + jsonPath + "\", equalTo(" + unescapedExpected
                            + "));\n");
                    break;
                case "contain":
                    String containPath = assertion.actual.replace("jsonData.", "");
                    String unescapedContainExpected = assertion.expected.replace("\\\"", "\"");
                    writer.write("        response.then().body(\"" + containPath + "\", containsString("
                            + unescapedContainExpected + "));\n");
                    break;
                case "have.status":
                    writer.write("        response.then().statusCode(" + assertion.expected + ");\n");
                    break;
                case "contentType":
                    writer.write("        response.then().contentType(" + assertion.expected + ");\n");
                    break;
            }
        }

        writer.write("    }\n\n");
    }

    /**
     * Method to generate a map of headers
     * @param map
     * @return
     */
    private String generateMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("new HashMap<String, String>() {{\n");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String headerValue = "\"" + entry.getValue() + "\"";
            Matcher matcher = Patterns.VARIABLE.matcher(entry.getValue());
            while (matcher.find()) {
                // log find
                logger.info("Variable found in header: " + entry.getValue());
                // resolve variable
                String variableName = matcher.group().substring(2, matcher.group().length() - 2);
                // logger.info("Variable name: " + variableName);
                if (!collectionVariables.stream().anyMatch(v -> v.key.equals(variableName))) {
                    //TODO: add comment to the generated code to indicate that the variable is not found in the collection variables list
                    logger.warn(
                            "Variable {} not found in collection variables. Adding it to the collection variables list with an empty value",
                            variableName);
                    collectionVariables.add(new CollectionVariable(variableName, ""));
                }
                if (entry.getValue() == matcher.group()) {
                    headerValue = entry.getValue().replace(matcher.group(),
                            "collectionVariables.get(\"" + variableName + "\")");
                    logger.info("Resolved variable: " + headerValue);
                } else {
                    headerValue = "\"" + entry.getValue().replace(matcher.group(),
                            "\" + collectionVariables.get(\"" + variableName + "\") + \"") + "\"";
                    logger.info("Variable {} is a part of an aggregate string: {}", variableName, entry.getValue());
                }

            }
            sb.append("            put(\"").append(entry.getKey()).append("\", ").append(headerValue).append(");\n");
        }
        sb.append("        }}");
        return sb.toString();
    }

    /**
     * Class to represent a test case
     */
    private static class TestCase {
        String name;
        String method;
        String url;
        String body;
        Map<String, String> headers = new HashMap<>();
        List<ScriptCommand> preRequestScript = new ArrayList<>();
        List<Assertion> testScript = new ArrayList<>();
    }

    /**
     * Class to represent a test case
     */
    private static class ScriptCommand {
        String type;
        String key;
        String value;
        String originalScript;
    }

    /**
     * Class to represent a script command
     */
    private static class Assertion {
        String type; // "expect" or "test"
        String actual;
        String matcher;
        String expected;
        String description; // for pm.test assertions
        String originalScript; // original Postman script
    }

    /**
     * Class to represent a variable
     */
    private static class Variable {
        String key;
        String value;
        String type;

        Variable(String key, String value, String type) {
            this.key = key;
            this.value = value;
            this.type = type;
        }
    }

    /**
     * Class to represent a collection variable
     */
    private static class CollectionVariable extends Variable {
        CollectionVariable(String key, String value) {
            super(key, value, "Collection");
        }
    }
}