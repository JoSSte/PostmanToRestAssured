package dk.kodeninjaer.testing.converter;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Matcher;
import java.util.stream.Stream;

public class PatternsTest {
    /*
     * 
     */
    static final String[] parameters = {
        "some other script",
        "pm.environment.get(\"someValue\");",
        "pm.environment.set(\"someKey\",\"someValue\");",
        "pm.environment.set(\"someKey\",someReturnValue.method());",
        "console.log(\"someValue\");",
        "alert(\"someValue\");",
        "pm.globals.set(\"someKey\",\"someValue\");",
        "pm.globals.get(\"someValue\");",
        "pm.globals.set(\"someKey\",someReturnValue.method());"
    };
    

    @DisplayName("Test Class cannot be instantiated")
    @Test
    public void testConstructor() {
        // Verify we can't instantiate the class
        assertThrows(IllegalAccessException.class, () -> {
            Patterns.class.getDeclaredConstructor().newInstance();
        });
    }
    
    @DisplayName("Test Environment Set")
    @ParameterizedTest(name = "Test {index} with script: {arguments}")
    @MethodSource("provideSetParameters")
    public void testEnvironmentSet(String script, boolean expected) {
        Matcher matcher = Patterns.ENVIRONMENT_SET.matcher(script);
        assertEquals(expected, matcher.find());
    }

    /**
     * Method to provide the parameters for the testEnvironmentSet method
     * @return
     */
    private static Stream<Arguments> provideSetParameters() {
        return Stream.of(
                Arguments.of(parameters[0], false),
                Arguments.of(parameters[1], false),
                Arguments.of(parameters[2], true),
                Arguments.of(parameters[3], true),
                Arguments.of(parameters[4], false),
                Arguments.of(parameters[5], false),
                Arguments.of(parameters[6], false),
                Arguments.of(parameters[7], false),
                Arguments.of(parameters[8], false)
        );
    }



    @DisplayName("Test Environment Get")
    @ParameterizedTest(name = "Test {index} with script: {arguments}")
    @MethodSource("provideGetParameters")
    public void testEnvironmentGet(String script, boolean expected) {
        Matcher matcher = Patterns.ENVIRONMENT_GET.matcher(script);
        assertEquals(expected, matcher.find());
    }

    /**
     * Method to provide the parameters for the testEnvironmentGet method
     * @return
     */
    private static Stream<Arguments> provideGetParameters() {
        return Stream.of(
                Arguments.of(parameters[0], false),
                Arguments.of(parameters[1], true),
                Arguments.of(parameters[2], false),
                Arguments.of(parameters[3], false),
                Arguments.of(parameters[4], false),
                Arguments.of(parameters[5], false),
                Arguments.of(parameters[6], false),
                Arguments.of(parameters[7], false),
                Arguments.of(parameters[8], false)
        );
    }
}
