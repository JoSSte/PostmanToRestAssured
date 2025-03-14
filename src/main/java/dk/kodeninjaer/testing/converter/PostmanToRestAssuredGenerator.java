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
import java.util.regex.Pattern;

public class PostmanToRestAssuredGenerator {
    private static final Logger logger = LoggerFactory.getLogger(PostmanToRestAssuredGenerator.class);
    private final String outputPackage;
    private final String outputClassName;
    private final Map<String, String> environment = new HashMap<>();

    public PostmanToRestAssuredGenerator(String outputPackage, String outputClassName) {
        this.outputPackage = outputPackage;
        this.outputClassName = outputClassName;
    }

    public void generate(String postmanCollectionPath) throws IOException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode collection = mapper.readTree(new File(postmanCollectionPath));
        
        String baseUrl = collection.path("info").path("url").asText("");
        List<TestCase> testCases = parseItems(collection.path("item"));
        
        generateTestClass(baseUrl, testCases);
    }

    private List<TestCase> parseItems(JsonNode items) throws JsonProcessingException {
        List<TestCase> testCases = new ArrayList<>();
        for (JsonNode item : items) {
            if (item.has("request")) {
                TestCase testCase = new TestCase();
                JsonNode request = item.path("request");
                
                testCase.name = item.path("name").asText();
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
            }
        }
        return testCases;
    }

    private List<ScriptCommand> parseScript(String script) {
        List<ScriptCommand> commands = new ArrayList<>();
        Pattern pattern = Pattern.compile("pm\\.environment\\.set\\([\"'](.*?)[\"'],\\s*(.*?)\\)");
        java.util.regex.Matcher matcher = pattern.matcher(script);
        
        while (matcher.find()) {
            ScriptCommand cmd = new ScriptCommand();
            cmd.type = "SET_ENV";
            cmd.key = matcher.group(1);
            cmd.value = matcher.group(2);
            commands.add(cmd);
        }
        return commands;
    }

    private List<Assertion> parseAssertions(String script) {
        if(script == null || script.isEmpty()){
            System.err.println("Empty test script found");
        }
        List<Assertion> assertions = new ArrayList<>();
        
        // Parse pm.expect assertions
        Pattern expectPattern = Pattern.compile("pm\\.expect\\((.*?)\\)\\.to\\.(.*?)\\((.*?)\\)|pm\\.response\\.to\\.have\\.status\\((\\d+)\\)");
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
            assertions.add(assertion);
        }
        if(assertions.isEmpty()){
            System.err.println("No assertions found in script: " + script);
        }

        // Parse pm.test assertions
        Pattern testPattern = Pattern.compile("pm\\.test\\([\"'](.*?)[\"'],\\s*function\\s*\\(\\)\\s*\\{\\s*(.*?)\\s*\\}\\)");
        java.util.regex.Matcher testMatcher = testPattern.matcher(script);
        
        while (testMatcher.find()) {
            Assertion assertion = new Assertion();
            assertion.type = "test";
            assertion.description = testMatcher.group(1);
            assertion.script = testMatcher.group(2);
            assertions.add(assertion);
        }
        
        return assertions;
    }

    private void generateTestClass(String baseUrl, List<TestCase> testCases) throws IOException {
        String outputPath = "src/test/java/" + outputPackage.replace('.', '/') + "/" + outputClassName + ".java";
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

            // Write setup method
            writer.write("    @BeforeAll\n");
            writer.write("    public static void setup() {\n");
            writer.write("        requestSpec = new RequestSpecBuilder()\n");
            writer.write("            .setBaseUri(\"" + baseUrl + "\")\n");
            writer.write("            .build();\n");
            writer.write("    }\n\n");

            // Write test methods
            for (TestCase test : testCases) {
                generateTestMethod(writer, test);
            }

            writer.write("}\n");
        }
    }

    private void generateTestMethod(FileWriter writer, TestCase test) throws IOException {
        String methodName = test.name.replaceAll("[^a-zA-Z0-9]", "_");
        writer.write("    @Test\n");
        writer.write("    public void " + methodName + "() {\n");
        
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
            writer.write("        spec.body(\"" + escapedBody + "\");\n\n");
        }

        writer.write("        Response response = spec.when()." + test.method.toLowerCase() + "(\"" + test.url + "\");\n\n");

        // Write assertions
        for (Assertion assertion : test.testScript) {
            if ("expect".equals(assertion.type)) {
                switch (assertion.matcher) {
                    case "equal":
                        writer.write("        response.then().body(" + assertion.actual + ", equalTo(" + assertion.expected + "));\n");
                        break;
                    case "contain":
                        writer.write("        response.then().body(" + assertion.actual + ", containsString(" + assertion.expected + "));\n");
                        break;
                    case "have.status":
                        writer.write("        response.then().statusCode(" + assertion.expected + ");\n");
                        break;
                }
            } else if ("test".equals(assertion.type)) {
                writer.write("        // Test: " + assertion.description + "\n");
                // Convert pm.test script to RestAssured assertions
                String script = assertion.script;
                if (script.contains("pm.response.to.have.status")) {
                    String statusCode = script.replaceAll(".*pm\\.response\\.to\\.have\\.status\\((\\d+)\\).*", "$1");
                    writer.write("        response.then().statusCode(" + statusCode + ");\n");
                }
                if (script.contains("pm.response.to.be.json")) {
                    writer.write("        response.then().contentType(\"application/json\");\n");
                }
                // Add more pm.test conversions as needed
            }
        }

        writer.write("    }\n\n");
    }

    private String generateMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("new HashMap<String, String>() {{\n");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append("            put(\"").append(entry.getKey()).append("\", \"").append(entry.getValue()).append("\");\n");
        }
        sb.append("        }}");
        return sb.toString();
    }

    private static class TestCase {
        String name;
        String method;
        String url;
        String body;
        Map<String, String> headers = new HashMap<>();
        List<ScriptCommand> preRequestScript = new ArrayList<>();
        List<Assertion> testScript = new ArrayList<>();
    }

    private static class ScriptCommand {
        String type;
        String key;
        String value;
    }

    private static class Assertion {
        String type;  // "expect" or "test"
        String actual;
        String matcher;
        String expected;
        String description;  // for pm.test assertions
        String script;      // for pm.test assertions
    }
} 