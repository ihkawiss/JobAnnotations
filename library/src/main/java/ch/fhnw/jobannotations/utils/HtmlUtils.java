package ch.fhnw.jobannotations.utils;

import org.jsoup.Jsoup;

/**
 * Utility class providing utility methods for HTML String handling.
 *
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
 */
public class HtmlUtils {
    private HtmlUtils() {
        // util class
    }

    /**
     * Extracts plain text from given HTML String.
     *
     * @param html HTML String to be used for plain text extraction
     * @return Extracted plain text
     */
    public static String getPlainTextFromHtml(String html) {
        // keep line breaks of b-tags after other tags
        html = html.replaceAll("(?i)>\\s*\\n*\\s*<b>", "><br><b>");

        // replace b-tags with space to prevent line breaks
        html = html.replaceAll("(?i)\\s*\\n*\\s*</?b>\\s*", " ");

        // replace br-tags and line breaks with placeholder
        String breakTagPlaceholder = "%BREAK%";
        html = html.replaceAll("(?i)(<br[^>]*>|\\n)", breakTagPlaceholder);

        // replace p-tags with placeholder
        html = html.replaceAll("(?i)(<p>|\\n)", breakTagPlaceholder);

        // clean html
        html = Jsoup.parse(html).text();

        // replace non-breaking space with normal whitespace
        html = html.replaceAll("\\u00A0", " ");

        // replace placeholder with real line breaks
        html = html.replaceAll(breakTagPlaceholder, "\n");

        return html;
    }
}
