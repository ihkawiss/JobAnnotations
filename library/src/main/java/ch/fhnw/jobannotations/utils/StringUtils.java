package ch.fhnw.jobannotations.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for String operations.
 *
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
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


    /**
     * Extracts complete sentences from given String.
     *
     * @param plainText Plain text to be used for sentence extraction
     * @return Extracted sentences as a single String
     */
    public static String extractSentencesFromPlaintText(String plainText) {
        StringBuilder sentences = new StringBuilder();
        boolean firstLine = true;
        for (String line : plainText.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            String sentenceRegex = "[^.!?:]+[.!?:]";
            Matcher sentenceMatcher = Pattern.compile(sentenceRegex).matcher(line);
            while (sentenceMatcher.find()) {
                String sentence = sentenceMatcher.group().trim();
                if (sentence.isEmpty()) {
                    continue;
                }
                if (sentence.split(" ").length < 2) {
                    continue;
                }

                if (!firstLine) {
                    sentences.append("\n");
                }
                firstLine = false;
                sentences.append(sentence);
            }
        }
        return sentences.toString();
    }
}
