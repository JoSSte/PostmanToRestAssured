# Postman to RestAssured Converter

This tool converts Postman collections into RestAssured test classes. It takes a Postman collection JSON file and generates a Java test class that uses RestAssured to perform the same API tests.

## Features

- Converts Postman requests to RestAssured test methods
- Preserves request headers, body, and URL parameters
- Converts Postman test scripts to RestAssured assertions
- Supports common Postman assertions like:
  - `pm.expect().to.equal()`
  - `pm.response.to.have.status()`
  - `pm.response.to.be.json()`
- Handles environment variables
- Generates proper JUnit 5 test structure
- Supports Postman folders (nested requests)

## Prerequisites

- Java 21 or higher
- Maven 3.9 or higher
- Postman collection exported as JSON (tested on version 2.1 of the spec.)

## Dependencies

The project uses the following main dependencies:
- RestAssured
- JUnit 5
- Jackson (for JSON processing)
- SLF4J (for logging)

## Building the Project

```bash
mvn clean package
```

This will create an executable JAR file in the `target` directory.

## Usage

### Command Line

```bash
java -jar target/postman-to-restassured-1.0-SNAPSHOT.jar <path-to-postman-collection.json> <output-package> <output-class-name>
```

Example:
```bash
java -jar target/postman-to-restassured-1.0-SNAPSHOT.jar ./TestCollection.postman_collection.json dk.kodeninjaer.testing.converter TestCollectionTests
```

### Programmatic Usage

```java
PostmanToRestAssuredGenerator generator = new PostmanToRestAssuredGenerator(
    "dk.kodeninjaer.testing.converter",  // output package
    "TestCollectionTests"                // output class name
);
generator.generate("path/to/collection.json");
```

## Generated Test Structure

The generated test class will include:
- A `setup()` method with `@BeforeAll` annotation to configure the base URL
- Individual test methods for each request in the collection
- Proper RestAssured assertions converted from Postman test scripts

Example generated test:
```java
@Test
public void get_message() {
    RequestSpecification spec = given(requestSpec)
        .headers(new HashMap<String, String>() {{
            put("Content-Type", "application/json");
        }});

    Response response = spec.when().get("/api/message");

    response.then().body("json.message", equalTo("Hello World"));
    response.then().statusCode(200);
}
```

### Folder Support

The tool supports Postman folders and generates test methods with names that reflect the folder structure. For example:

```java
@Test
public void auth_login_post() {  // From folder "auth" and request "login"
    // ... test implementation
}

@Test
public void users_admin_list_get() {  // From folder "users/admin" and request "list"
    // ... test implementation
}
```

## Supported Postman Features

### Request Features
- HTTP methods (GET, POST, PUT, DELETE, etc.)
- Headers
- Request body (JSON)
- URL parameters
- Nested folders and requests

### Test Script Features
- `pm.expect()` assertions
- `pm.response.to.have.status()`
- `pm.response.to.be.json()`
- Environment variable handling (basic key-value pairs)

## Features we aim to implement
The order is not prioritised
### Collection Features
- Collection variables
- Collection-level scripts
- Collection-level pre-request scripts
- Collection-level test scripts

### Request Features
- Request-level pre-request scripts
- Request-level test scripts
- Request-level variables

### Flow Control
- Data-driven testing
- Iterations and loops

### Advanced Features
- Form data
- Multipart form data

## Contributing

Feel free to submit issues and enhancement requests! 