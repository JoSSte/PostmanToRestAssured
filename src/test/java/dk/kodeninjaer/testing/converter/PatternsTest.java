package dk.kodeninjaer.testing.converter;


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

public class PatternsTest {
    @Test
    public void testConstructor() {
        // Verify we can't instantiate the class
        assertThrows(IllegalAccessException.class, () -> {
            Patterns.class.getDeclaredConstructor().newInstance();
        });
    }
    @Test
    public void testEnvironmentSet() {
        String script = "pm.environment.set(\"key\", \"value\");";
        Matcher matcher = Patterns.ENVIRONMENT_SET.matcher(script);
        assertTrue(matcher.find());
    }

    @Test
    public void testEnvironmentSetNegative() {
        String script = "some other script";
        Matcher matcher = Patterns.ENVIRONMENT_SET.matcher(script);
        assertFalse(matcher.find());
    }
    
    @Test
    public void testEnvironmentGet() {
        String script = "pm.environment.get(\"someValue\");";
        Matcher matcher = Patterns.ENVIRONMENT_GET.matcher(script);
        assertTrue(matcher.find());
    }

    @Test
    public void testEnvironmentGetNegative() {
        String script = "some other script";
        Matcher matcher = Patterns.ENVIRONMENT_GET.matcher(script);
        assertFalse(matcher.find());
    }
}
