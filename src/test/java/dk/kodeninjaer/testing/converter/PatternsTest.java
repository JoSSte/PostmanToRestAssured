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
        "pm.globals.set(\"someKey\",someReturnValue.method());",
        "pm.environment.get(\"somePartValue\"",
        "pm.environment.get(\"somePartValue\"XXX",
        "pm.environment.get(\"somePartValue\"\"",
        "pm.collectionVariables.set(\"somevarKey\",someReturnValue.method());",
        "pm.collectionVariables.get(\"somevarKey\");",
        "pm.collectionVariables.set(\"someKey\",someReturnValue);",
        "pm.collectionVariables.get(\"someKey\");"
    };
    

    @DisplayName("Test Class cannot be instantiated")
    @Test
    public void testConstructor() {
        // Verify we can't instantiate the class
        assertThrows(IllegalAccessException.class, () -> {
            Patterns.class.getDeclaredConstructor().newInstance();
        });
    }
    
    @ParameterizedTest(name = "Test Environment Set Matching #{index} with script: {arguments}")
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
                Arguments.of(parameters[8], false),
                Arguments.of(parameters[9], false),
                Arguments.of(parameters[10], false),
                Arguments.of(parameters[11], false)
        );
    }



    @ParameterizedTest(name = "Test Environment Get #{index} with script: {arguments}")
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
                Arguments.of(parameters[8], false),
                Arguments.of(parameters[9], false),
                Arguments.of(parameters[10], false),
                Arguments.of(parameters[11], false)
        );
    }

    @DisplayName("Test Environment Get")
    @ParameterizedTest(name = "Test {index} with script: {arguments}")
    @MethodSource("provideGetPartParameters")
    public void testEnvironmentGetPartial(String script, boolean expected) {
        Matcher matcher = Patterns.ENVIRONMENT_GET_PART.matcher(script);
        assertEquals(expected, matcher.find());
    }

    /**
     * Method to provide the parameters for the testEnvironmentGet method
     * @return
     */
    private static Stream<Arguments> provideGetPartParameters() {
        return Stream.of(
                Arguments.of(parameters[0], false),
                Arguments.of(parameters[1], true),
                Arguments.of(parameters[2], false),
                Arguments.of(parameters[3], false),
                Arguments.of(parameters[4], false),
                Arguments.of(parameters[5], false),
                Arguments.of(parameters[6], false),
                Arguments.of(parameters[7], true),
                Arguments.of(parameters[8], false),
                Arguments.of(parameters[9], true),
                Arguments.of(parameters[10], true),
                Arguments.of(parameters[11], true),
                Arguments.of(parameters[12], false),
                Arguments.of(parameters[13], true),
                Arguments.of(parameters[14], false),
                Arguments.of(parameters[15], true)
        );
    }

    @ParameterizedTest(name = "Test Generic getter #{index} with script: {arguments}")
    @MethodSource("provideGetterParameters")
    public void testEnvironmentGetters(String script, boolean expected) {
        Matcher matcher = Patterns.VAR_GET.matcher(script);
        assertEquals(expected, matcher.find());
    }

    /**
     * Method to provide the parameters for the testEnvironmentGet method
     * @return
     */
    private static Stream<Arguments> provideGetterParameters() {
        return Stream.of(
                Arguments.of(parameters[0], false),
                Arguments.of(parameters[1], true),
                Arguments.of(parameters[2], false),
                Arguments.of(parameters[3], false),
                Arguments.of(parameters[4], false),
                Arguments.of(parameters[5], false),
                Arguments.of(parameters[6], false),
                Arguments.of(parameters[7], true),
                Arguments.of(parameters[8], false),
                Arguments.of(parameters[9], false),
                Arguments.of(parameters[10], false),
                Arguments.of(parameters[11], false),
                Arguments.of(parameters[12], false),
                Arguments.of(parameters[13], true),
                Arguments.of(parameters[14], false),
                Arguments.of(parameters[15], true)
        );
    }

    @ParameterizedTest(name = "Test Generic setter #{index} with script: {arguments}")
    @MethodSource("provideSetterParameters")
    public void testEnvironmentSetters(String script, boolean expected) {
        Matcher matcher = Patterns.VAR_SET.matcher(script);
        assertEquals(expected, matcher.find());
    }

    /**
     * Method to provide the parameters for the testEnvironmentGet method
     * @return
     */
    private static Stream<Arguments> provideSetterParameters() {
        return Stream.of(
                Arguments.of(parameters[0], false),
                Arguments.of(parameters[1], false),
                Arguments.of(parameters[2], true),
                Arguments.of(parameters[3], true),
                Arguments.of(parameters[4], false),
                Arguments.of(parameters[5], false),
                Arguments.of(parameters[6], true),
                Arguments.of(parameters[7], false),
                Arguments.of(parameters[8], true),
                Arguments.of(parameters[9], false),
                Arguments.of(parameters[10], false),
                Arguments.of(parameters[11], false),
                Arguments.of(parameters[12], true),
                Arguments.of(parameters[13], false),
                Arguments.of(parameters[14], true),
                Arguments.of(parameters[15], false)
        );
    }
}
