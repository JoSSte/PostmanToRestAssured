package dk.kodeninjaer.testing.converter;

import java.util.regex.Pattern;

/**
 * This class contains all the patterns used in the converter.  
 * It is a utility class and should not be instantiated.
 */
public final class Patterns {
    private Patterns() {
    }

    /**
     * For matching double-moustache notation variables
     */
    public static final Pattern VARIABLE             = Pattern.compile("\\{\\{[a-zA-Z0-9]*\\}\\}");
    /**
     * For matching any variable getter in a script 
     * pm.<scope>.get("variableName")
     */
    public static final Pattern VAR_GET              = Pattern.compile("pm\\.(environment|globals|collectionVariables)\\.get\\([\"'](.*?)[\"']\\)");
    /**
     * For matching any variable setter in a script 
     */
    public static final Pattern VAR_SET              = Pattern.compile("pm\\.(environment|globals|collectionVariables)\\.set\\([\"'](.*?)[\"'],\\s*(.*?)\\)");
    public static final Pattern ENVIRONMENT_SET      = Pattern.compile("pm\\.environment\\.set\\([\"'](.*?)[\"'],\\s*(.*?)\\)");
    public static final Pattern ENVIRONMENT_GET      = Pattern.compile("pm\\.environment\\.get\\([\"'](.*?)[\"']\\)");
    public static final Pattern ENVIRONMENT_GET_PART = Pattern.compile("pm\\.environment\\.get\\([\"'](.*?)[\"']");
    public static final Pattern EXPECT               = Pattern.compile("pm\\.expect\\((.*?)\\)\\.to\\.(.*?)\\((.*?)\\)|pm\\.response\\.to\\.have\\.status\\((\\d+)\\)");
    public static final Pattern TEST                 = Pattern.compile("pm\\.test\\([\"'](.*?)[\"'],\\s*function\\s*\\(\\)\\s*\\{\\s*(.*?)\\s*\\}\\)");
    

}
