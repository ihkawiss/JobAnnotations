package ch.fhnw.jobannotations.extractors.jobtitle.rating;

import ch.fhnw.jobannotations.extractors.workload.JobWorkloadExtractor;
import ch.fhnw.jobannotations.utils.ConfigurationUtil;
import ch.fhnw.jobannotations.utils.FileUtils;
import ch.fhnw.jobannotations.utils.IntStringPair;
import ch.fhnw.jobannotations.utils.PartOfSpeechUtil;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.TrieDictionary;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hoang
 */
public class JobTitleStringRatingManager {

    final static Logger LOG = Logger.getLogger(JobTitleStringRatingManager.class);

    public static final int LOW_RATING_THRESHOLD = 10;

    // rating values
    private static final int RATING_TAG_H1 = 20;
    private static final int RATING_JOB_TITLE_INDICATOR = 50;
    private static final int RATING_REPETITION = RATING_JOB_TITLE_INDICATOR / 2 * RATING_TAG_H1;
    private static final int RATING_MAX_KNOWN_LIST_MATCH = RATING_JOB_TITLE_INDICATOR * 2;
    private static final int RATING_NEGATIVE_MAX_SPECIAL_CHARS = -RATING_JOB_TITLE_INDICATOR;
    private static final int RATING_NEGATIVE_TEXT_TOO_LONG = -RATING_JOB_TITLE_INDICATOR;

    /**
     * Array of high priority tags with corresponding rating value
     */
    private static final IntStringPair[] RATINGS_HIGH_PRIORITY_TAG = {
            new IntStringPair(RATING_TAG_H1, "h1"),
            new IntStringPair(RATING_TAG_H1 / 4 * 3, "h2"),
            new IntStringPair(RATING_TAG_H1 / 2, "h3"),
            new IntStringPair(RATING_TAG_H1 / 4, "h4"),
            new IntStringPair(RATING_TAG_H1 / 4, "h5"),
            new IntStringPair(RATING_TAG_H1 / 4, "h6"),
            new IntStringPair(RATING_TAG_H1 / 4, "title"),
            new IntStringPair(RATING_TAG_H1 / 4, "b"),
            new IntStringPair(RATING_TAG_H1 / 4, "strong"),
            new IntStringPair(-RATING_TAG_H1, "li")
    };

    // html parsing
    private static final String REGEX_FORMAT_TAG = "<%s[^>]*>.*<\\/%s>";
    private static final String REGEX_TAG = "<[^>]+>";


    // job title indicator
    private static final String REGEX_GENDER_TEXT = "\\s?[wmfWMF]\\s?\\/\\s?[wmfWMF]\\s?";
    private static final String REGEX_JOB_TITLE_GENDER_SUFFIX = "\\w[-\\/\\(\\|]+(in|IN|r|R)|\\w[-\\/\\(\\|]*In";
    private static final String REGEX_WORKLOAD_GENDER_INDICATOR = "\\((" + JobWorkloadExtractor.WORKLOAD_REGEX + ")[^\\(\\)]*(" + REGEX_GENDER_TEXT + ")\\)|\\((" + REGEX_GENDER_TEXT + ")[^\\(\\)]*(" + JobWorkloadExtractor.WORKLOAD_REGEX + ")\\)";
    private static final String REGEX_GENDER_INDICATOR = "\\(?" + REGEX_GENDER_TEXT + "\\)?";
    private static final String REGEX_WORKLOAD_INDICATOR = "\\(?" + JobWorkloadExtractor.WORKLOAD_REGEX + "\\)?";

    /**
     * Array of job title indicator regex. Int value defines whether matched String should be removed or not (1 = true)
     */
    private static final IntStringPair[] JOB_TITLE_INDICATOR_REGEX_LIST = {
            new IntStringPair(0, REGEX_JOB_TITLE_GENDER_SUFFIX),
            new IntStringPair(1, REGEX_WORKLOAD_GENDER_INDICATOR),
            new IntStringPair(1, REGEX_GENDER_INDICATOR),
            new IntStringPair(1, REGEX_WORKLOAD_INDICATOR)
    };

    // special char count
    private static final String REGEX_SPECIAL_CHARS = "[^a-zA-Z]+";
    private static final double THRESHOLD_RATING_SPECIAL_CHARS = 0.5;

    // text length
    private static final int THRESHOLD_TEXT_TOO_LONG = 75;

    // known job titles
    private static final String DICTIONARY_CATEGORY_JOB_TITLE = "JobTitle";
    private static final int CHUNK_INFO_POS_START = 0;
    private static final int CHUNK_INFO_POS_END = 1;
    private static final int CHUNK_INFO_POS_SCORE = 2;


    private static TrieDictionary<String> dictionary = null;


    public JobTitleStringRatingManager() {
        if (dictionary == null) {
            dictionary = initJobTitleDictionary();
        }
    }

    private TrieDictionary<String> initJobTitleDictionary() {
        TrieDictionary<String> dictionary = new TrieDictionary<>();
        List<String> knownJobTitles = new ArrayList<>();

        InputStream inputStream = FileUtils.getResourceInputStream(ConfigurationUtil.get("extraction.titles.train"));
        if (inputStream != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    knownJobTitles.add(line);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            for (String title : knownJobTitles) {
                DictionaryEntry<String> entry = new DictionaryEntry<>(title, DICTIONARY_CATEGORY_JOB_TITLE);
                dictionary.addEntry(entry);
            }
        }

        return dictionary;
    }

    public List<IntStringPair> extractRatedStringsFromHtml(String[] htmlLines) {
        List<IntStringPair> extractedRatedStrings = new ArrayList<>();

        for (String htmlLine : htmlLines) {
            int additionalRating = 0;

            // extract all important tags
            String addedImportantTagContent = null;
            for (IntStringPair tagRating : RATINGS_HIGH_PRIORITY_TAG) {
                String tagName = tagRating.getString();
                String regex = String.format(REGEX_FORMAT_TAG, tagName, tagName);
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(htmlLine);

                while (matcher.find()) {
                    String matchedString = matcher.group();

                    // remove all html tags
                    matchedString = matchedString.replaceAll(REGEX_TAG, "").trim();

                    if (!matchedString.isEmpty()) {
                        // use Jsoup to convert html special to normal chars (eg.: &amp; => &)
                        matchedString = Jsoup.parse(Jsoup.parse(htmlLine).text()).text();

                        // add extracted tag content and rating to list
                        int rating = tagRating.getInt() + additionalRating;
                        IntStringPair ratedString = new IntStringPair(rating, matchedString);
                        extractedRatedStrings.add(ratedString);

                        addedImportantTagContent = matchedString;
                    }
                }
            }

            // remove all remaining html tags
            htmlLine = htmlLine.replaceAll(REGEX_TAG, "").trim();

            // use Jsoup to convert html special to normal chars (eg.: &amp; => &)
            htmlLine = Jsoup.parse(Jsoup.parse(htmlLine).text()).text();

            if (!htmlLine.isEmpty() && !htmlLine.equals(addedImportantTagContent)) {
                // add remaining text with base rating of 0 (+ additionalRating)
                IntStringPair ratedString = new IntStringPair(additionalRating, htmlLine);
                extractedRatedStrings.add(ratedString);
            }

        }

        return extractedRatedStrings;
    }

    public void adjustRatingsByJobTitleIndicator(List<IntStringPair> ratedStrings, boolean cleanJobTitle) {
        for (IntStringPair ratedString : ratedStrings) {

            int jobTitleRating = 0;
            String string = ratedString.getString();

            for (IntStringPair jobTitleIndicatorRating : JOB_TITLE_INDICATOR_REGEX_LIST) {
                String jobTitleIndicatorRegex = jobTitleIndicatorRating.getString();
                Pattern pattern = Pattern.compile(jobTitleIndicatorRegex);
                Matcher matcher = pattern.matcher(string);
                while (matcher.find()) {
                    String matchedString = matcher.group();

                    boolean removeMatchedString = jobTitleIndicatorRating.getInt() == 1;
                    if (removeMatchedString && cleanJobTitle) {
                        string = string.replace(matchedString, "").trim();
                    }
                    jobTitleRating += RATING_JOB_TITLE_INDICATOR;
                }
            }

            // remove double space
            string = string.replace("  ", " ");

            // update string
            ratedString.setString(string);

            // update rating
            int rating = ratedString.getInt();
            if (rating > 0 && jobTitleRating > 0) {
                rating *= jobTitleRating;
            } else {
                rating += jobTitleRating;
            }
            ratedString.setInt(rating);

        }
    }

    public void adjustRatingsBySpecialCharCount(List<IntStringPair> ratedStrings) {
        for (IntStringPair ratedString : ratedStrings) {
            String text = ratedString.getString();
            Pattern pattern = Pattern.compile(REGEX_SPECIAL_CHARS);
            Matcher matcher = pattern.matcher(text);

            // remove special chars
            while (matcher.find()) {
                String matchedString = matcher.group();
                text = text.replace(matchedString, "");
            }

            double specialCharPercentage = 1 - (double) text.length() / ratedString.getString().length(); // 50% = 0.5, not 50
            if (specialCharPercentage >= THRESHOLD_RATING_SPECIAL_CHARS) {
                int ratingAdjustment = (int) (RATING_NEGATIVE_MAX_SPECIAL_CHARS * specialCharPercentage);
                int newRating = ratedString.getInt() + ratingAdjustment;
                ratedString.setInt(newRating);
            }
        }
    }

    public void adjustRatingsByTextLength(List<IntStringPair> ratedStrings) {
        for (IntStringPair ratedString : ratedStrings) {
            int ratingMultiplier = ratedString.getString().length() / THRESHOLD_TEXT_TOO_LONG;
            if (ratingMultiplier > 0) {
                int ratingAdjustment = ratingMultiplier * RATING_NEGATIVE_TEXT_TOO_LONG;
                int newRating = ratedString.getInt() + ratingAdjustment;
                ratedString.setInt(newRating);
            }
        }

    }

    public void adjustRatingsByRepetitionCount(List<IntStringPair> ratedStrings) {
        for (IntStringPair currentRatedString : ratedStrings) {
            int counter = -1; // -1 because it will match with it's own list entry
            String currentString = currentRatedString.getString();
            for (IntStringPair ratedString : ratedStrings) {
                if (currentString.length() <= ratedString.getString().length()) {
                    if (ratedString.getString().contains(currentString)) {
                        counter++;
                    }
                }
            }

            int currentRating = currentRatedString.getInt();
            int additionalRating = counter * RATING_REPETITION;
            currentRatedString.setInt(currentRating + additionalRating);
        }
    }

    public void adjustRatingsByKnownJobTitleList(List<IntStringPair> ratedStrings) {
        for (IntStringPair ratedString : ratedStrings) {
            String text = ratedString.getString();

            LOG.debug(text);

            Map<String, Integer> foundChunks = PartOfSpeechUtil.getChunksByDictionary(dictionary, text, 1);
            for (String key : foundChunks.keySet()) {
                Integer score = foundChunks.get(key);

                LOG.debug("[" + score + "] " + key);

                if (score == 0) {
                    ratedString.setInt(ratedString.getInt() + 100);
                }

            }

        }
    }
}
