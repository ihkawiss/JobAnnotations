package ch.fhnw.jobannotations.utils;

import org.jsoup.Jsoup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hoang
 */
public class HtmlUtils {
    private HtmlUtils() {
        // util class
    }


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
