package dk.kodeninjaer.testing.converter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostmanToRestAssuredGeneratorTest {

    @Test
    public void testConstructor() {
        PostmanToRestAssuredGenerator generator = new PostmanToRestAssuredGenerator(
                "dk.kodeninjaer.testing.converter",
                "TestCollectionTests");
        assertNotNull(generator);
    }

    @ParameterizedTest
    @ValueSource(strings = { "src/test/resources/TestCollection.postman_collection.json",
            "src/test/resources/TestCollectionFolders.postman_collection.json" })
    void processCollectionsWithoutVariables(Path collectionPath, @TempDir Path tempDir) {
        System.out.println("processCollection " + collectionPath);
        try {
            String testName = collectionPath.getFileName().toString().replace(".postman_collection.json", "");

            // Create generator
            PostmanToRestAssuredGenerator generator = new PostmanToRestAssuredGenerator(
                    "dk.kodeninjaer.testing.converter.generated",
                    testName + "Test");

            // Generate the test class
            generator.generate(collectionPath.toString());

            // Verify the generated file exists
            Path expectedOutputPath = Path
                    .of(PostmanToRestAssuredGenerator.OUTPUT_BASEPATH + "/dk/kodeninjaer/testing/converter/generated")
                    .resolve(testName + "Test.java");
            assertTrue(Files.exists(expectedOutputPath),
                    "Generated test file should exist at " + expectedOutputPath);

            //TODO compile the generated file
            //TODO run the generated file
            //TODO assert the output

            // Basic validation of the generated file
            String content = Files.readString(expectedOutputPath);
            assertTrue(content.contains("@Test"), "Generated file should contain @Test annotations");
            assertTrue(content.contains("class " + testName + "Test"),
                    "Generated file should contain the correct class name");
            assertTrue(content.contains("RestAssured"),
                    "Generated file should contain RestAssured imports");
            Matcher matcher = Patterns.VARIABLE.matcher(content);
            assertFalse(matcher.find(), "Generated file should not contain double moustache notation elements");
        } catch (IOException e) {
            fail("Failed to process collection " + collectionPath + ": " + e.getMessage());
        }
    }

//    @Disabled("Not implemented yet")
    @ParameterizedTest
    @ValueSource(strings = { "src/test/resources/TestCollectionVariables.postman_collection.json" })
    void processVariableCollection(Path collectionPath, @TempDir Path tempDir) {
        System.out.println("processCollection " + collectionPath);
        try {
            String testName = collectionPath.getFileName().toString().replace(".postman_collection.json", "");

            // Create generator
            PostmanToRestAssuredGenerator generator = new PostmanToRestAssuredGenerator(
                    "dk.kodeninjaer.testing.converter.generated",
                    testName + "Test");

            // Generate the test class
            generator.generate(collectionPath.toString());

            // Verify the generated file exists
            Path expectedOutputPath = Path
                    .of(PostmanToRestAssuredGenerator.OUTPUT_BASEPATH + "/dk/kodeninjaer/testing/converter/generated")
                    .resolve(testName + "Test.java");
            assertTrue(Files.exists(expectedOutputPath),
                    "Generated test file should exist at " + expectedOutputPath);

            // TODO: use reflection to inspect the collection variables
            // https://stackoverflow.com/questions/27857612/access-a-private-field-for-a-junit-test

            // Basic validation of the generated file
            String content = Files.readString(expectedOutputPath);
            assertTrue(content.contains("@Test"), "Generated file should contain @Test annotations");
            assertTrue(content.contains("class " + testName + "Test"),
                    "Generated file should contain the correct class name");
            assertTrue(content.contains("RestAssured"),
                    "Generated file should contain RestAssured imports");
            // assertFalse(content.matches(".*\\{\\{\\}\\}.*"),"Generated file should not
            // contain double moustache notation elements");


            Matcher matcher = Patterns.VARIABLE.matcher(content);
            assertFalse(matcher.find(), "Generated file should not contain double moustache notation elements");

        } catch (IOException e) {
            fail("Failed to process collection " + collectionPath + ": " + e.getMessage());
        }
    }
}