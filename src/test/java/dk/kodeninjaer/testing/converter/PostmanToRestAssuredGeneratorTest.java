package dk.kodeninjaer.testing.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class PostmanToRestAssuredGeneratorTest {

    @Test
    public void testConstructor() {
        PostmanToRestAssuredGenerator generator = new PostmanToRestAssuredGenerator(
            "dk.kodeninjaer.testing.converter",
            "TestCollectionTests"
        );
        assertNotNull(generator);
    }

    @TestFactory
    Collection<DynamicTest> processAllCollections(@TempDir Path tempDir) throws IOException {
        List<DynamicTest> dynamicTests = new ArrayList<>();
        
        // Get all .json files from resources directory
        URL resourceUrl = getClass().getClassLoader().getResource(".");
        if (resourceUrl == null) {
            fail("Could not find resources directory");
        }

        File resourceDir = new File(resourceUrl.getFile());
        if (resourceDir.exists() && resourceDir.isDirectory()) {
            try (Stream<Path> paths = Files.walk(resourceDir.toPath())) {
                paths.filter(path -> path.toString().endsWith(".postman_collection.json"))
                     .forEach(path -> {
                         String fileName = path.getFileName().toString();
                         String testName = fileName.replace(".postman_collection.json", "");
                         
                         dynamicTests.add(DynamicTest.dynamicTest(
                             "Process collection: " + testName,
                             () -> processCollection(path, tempDir, testName)
                         ));
                     });
            }
        }
        System.out.println("Found " + dynamicTests.size() + " collections to process");
        return dynamicTests;
    }

    private void processCollection(Path collectionPath, Path tempDir, String testName) {
        try {
            // Create generator
            PostmanToRestAssuredGenerator generator = new PostmanToRestAssuredGenerator(
                "dk.kodeninjaer.testing.converter.generated",
                testName + "Test"
            );

            // Generate the test class
            generator.generate(collectionPath.toString());

            // Verify the generated file exists
            Path expectedOutputPath = Path.of("src/test/java/dk/kodeninjaer/testing/converter/generated")
                .resolve(testName + "Test.java");
            assertTrue(Files.exists(expectedOutputPath), 
                "Generated test file should exist at " + expectedOutputPath);

            // Basic validation of the generated file
            String content = Files.readString(expectedOutputPath);
            assertTrue(content.contains("@Test"), "Generated file should contain @Test annotations");
            assertTrue(content.contains("class " + testName + "Test"), 
                "Generated file should contain the correct class name");
            assertTrue(content.contains("RestAssured"), 
                "Generated file should contain RestAssured imports");
        } catch (IOException e) {
            fail("Failed to process collection " + collectionPath + ": " + e.getMessage());
        }
    }
} 