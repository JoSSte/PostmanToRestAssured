package dk.kodeninjaer.testing.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostmanToRestAssured {

    private static final Logger logger = LoggerFactory.getLogger(PostmanToRestAssured.class);
    private static RequestSpecification requestSpec;
    private static List<TestCase> testCases = new ArrayList<>();
    private static Map<String, String> environment = new HashMap<>();

    @BeforeAll
    public static void setup() throws IOException {
        // Read Postman collection
        ObjectMapper mapper = new ObjectMapper();
        JsonNode collection = mapper.readTree(
            PostmanToRestAssured.class.getClassLoader()
                .getResourceAsStream("TestCollection.postman_collection.json")
        );

        // Extract base URL if exists
        String baseUrl = collection.path("info").path("url").asText("");
        
        // Build base request specification
        requestSpec = new RequestSpecBuilder()
            .setBaseUri(baseUrl)
            .build();

        // Parse items (requests)
        JsonNode items = collection.path("item");
        parseItems(items);
    }

    private static void parseItems(JsonNode items) {
        for (JsonNode item : items) {
            if (item.has("request")) {
                TestCase testCase = new TestCase();
                JsonNode request = item.path("request");
                
                testCase.name = item.path("name").asText();
                testCase.method = request.path("method").asText();
                testCase.url = request.path("url").path("raw").asText();
                // Log request details
                logger.info("Test Case: {}", testCase.name);
                logger.info("Method: {}", testCase.method);
                logger.info("URL: {}", testCase.url);
                
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
                    String script = event.path("script").path("exec").asText();

                    if ("prerequest".equals(listen)) {
                        testCase.preRequestScript = parseScript(script);
                    } else if ("test".equals(listen)) {
                        testCase.testScript = parseAssertions(script);
                    }
                }
                
                testCases.add(testCase);
            }
        }
    }

    private static List<ScriptCommand> parseScript(String script) {
        List<ScriptCommand> commands = new ArrayList<>();
        
        // Parse variable assignments
        Pattern pattern = Pattern.compile("pm\\.environment\\.set\\([\"'](.*?)[\"'],\\s*(.*?)\\)");
        Matcher matcher = pattern.matcher(script);
        
        while (matcher.find()) {
            ScriptCommand cmd = new ScriptCommand();
            cmd.type = "SET_ENV";
            cmd.key = matcher.group(1);
            cmd.value = matcher.group(2);
            commands.add(cmd);
        }
        
        return commands;
    }

    private static List<Assertion> parseAssertions(String script) {
        List<Assertion> assertions = new ArrayList<>();
        
        // Parse pm.expect assertions
        Pattern pattern = Pattern.compile("pm\\.expect\\((.*?)\\)\\.to\\.(.*?)\\((.*?)\\)");
        Matcher matcher = pattern.matcher(script);
        
        while (matcher.find()) {
            Assertion assertion = new Assertion();
            assertion.actual = matcher.group(1);
            assertion.matcher = matcher.group(2);
            assertion.expected = matcher.group(3);
            assertions.add(assertion);
        }
        
        return assertions;
    }

    @Test
    public void executeTests() {
        for (TestCase test : testCases) {
            // Execute pre-request scripts
            executePreRequestScript(test.preRequestScript);
            
            // Make the request
            Response response = executeRequest(test);
            
            // Execute test scripts
            executeTestScript(test.testScript, response);
        }
    }

    private void executePreRequestScript(List<ScriptCommand> commands) {
        for (ScriptCommand cmd : commands) {
            if ("SET_ENV".equals(cmd.type)) {
                environment.put(cmd.key, cmd.value);
            }
        }
    }

    private Response executeRequest(TestCase test) {
        RequestSpecification spec = given(requestSpec)
            .headers(test.headers);

        if (test.body != null) {
            spec.body(test.body);
        }

        Response response = null;
        switch (test.method.toUpperCase()) {
            case "GET":
                response = spec.when().get(test.url);
                break;
            case "POST":
                response = spec.when().post(test.url);
                break;
            case "PUT":
                response = spec.when().put(test.url);
                break;
            case "DELETE":
                response = spec.when().delete(test.url);
                break;
        }
        return response;
    }

    private void executeTestScript(List<Assertion> assertions, Response response) {
        for (Assertion assertion : assertions) {
            switch (assertion.matcher) {
                case "equal":
                    response.then().body(assertion.actual, equalTo(assertion.expected));
                    break;
                case "contain":
                    response.then().body(assertion.actual, containsString(assertion.expected));
                    break;
                case "have.status":
                    response.then().statusCode(Integer.parseInt(assertion.expected));
                    break;
                // Add more matchers as needed
            }
        }
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
        String actual;
        String matcher;
        String expected;
    }
}


