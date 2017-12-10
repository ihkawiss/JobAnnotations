package ch.fhnw.jobannotations.utils;

/**
 * @author Hoang
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {

    // regex to filter special characters
    public static final String SPECIAL_CHARS = "[$&+,:;=?@#<>.^*%!\\-/]";

    private StringUtils() {
        // private constructor
    }

    /**
     * Changes given string to lower case and removes special chars.
     *
     * @param text String to simplify
     * @return Simplified string
     * @see #removeSpecialChars(String)
     */
    public static String simplify(String text) {
        return removeSpecialChars(text.toLowerCase());
    }

    /**
     * Removes special characters from given string.
     *
     * @param text String to remove special characters from
     * @return String without special characters
     * @see #SPECIAL_CHARS
     */
    public static String removeSpecialChars(String text) {
        return text.replaceAll(SPECIAL_CHARS, "");
    }
}
