package dk.kodeninjaer.testing.converter;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java -jar postman-to-restassured.jar <postman-collection.json> <output-package> <output-class-name>");
            System.exit(1);
        }

        String postmanCollectionPath = args[0];
        String outputPackage = args[1];
        String outputClassName = args[2];

        try {
            PostmanToRestAssuredGenerator generator = new PostmanToRestAssuredGenerator(outputPackage, outputClassName);
            generator.generate(postmanCollectionPath);
            System.out.println("Successfully generated RestAssured test class!");
        } catch (IOException e) {
            System.err.println("Error generating test class: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 