package ch.fhnw.jobannotations.extractors.jobtitle;

import ch.fhnw.jobannotations.extractors.workload.WorkloadExtractor;
import ch.fhnw.jobannotations.utils.IntStringPair;

/**
 * This class holds constants that are used in {@link TitleExtractor}
 *
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
 */
class TitleExtractorConstants {

    static final String REGEX_SPECIAL_CHARS_TO_REMOVE = "[$+,:;=?@#<>.^*%!-\"]";
    static final String[] IRRELEVANT_TAGS = {"style", "script", "meta", "link", "a", "input", "button"};

    static final int LOW_RATING_THRESHOLD = 10;

    // rating values
    private static final int RATING_TAG_H1 = 20;
    static final int RATING_JOB_TITLE_INDICATOR = 50;
    static final int RATING_REPETITION = RATING_JOB_TITLE_INDICATOR / 2 * RATING_TAG_H1;
    static final int RATING_NEGATIVE_MAX_SPECIAL_CHARS = -RATING_JOB_TITLE_INDICATOR;
    static final int RATING_NEGATIVE_TEXT_TOO_LONG = -RATING_JOB_TITLE_INDICATOR;

    /**
     * Array of high priority tags with corresponding rating value
     */
    static final IntStringPair[] RATINGS_HIGH_PRIORITY_TAG = {
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
    static final String REGEX_FORMAT_TAG = "<%s[^>]*>.*<\\/%s>";
    static final String REGEX_TAG = "<[^>]+>";

    // job title indicator
    private static final String REGEX_GENDER_TEXT = "\\s?[wmfWMF]\\s?\\/\\s?[wmfWMF]\\s?";
    private static final String REGEX_JOB_TITLE_GENDER_SUFFIX = "\\w[-\\/\\(\\|]+(in|IN|r|R)|\\w[-\\/\\(\\|]*In";
    private static final String REGEX_WORKLOAD_GENDER_INDICATOR = "\\((" + WorkloadExtractor.WORKLOAD_REGEX + ")[^\\(\\)]*(" + REGEX_GENDER_TEXT + ")\\)|\\((" + REGEX_GENDER_TEXT + ")[^\\(\\)]*(" + WorkloadExtractor.WORKLOAD_REGEX + ")\\)";
    private static final String REGEX_GENDER_INDICATOR = "\\(?" + REGEX_GENDER_TEXT + "\\)?";
    private static final String REGEX_WORKLOAD_INDICATOR = "\\(?" + WorkloadExtractor.WORKLOAD_REGEX + "\\)?";

    /**
     * Array of job title indicator regex. Int value defines whether matched String should be removed or not (1 = true)
     */
    static final IntStringPair[] JOB_TITLE_INDICATOR_REGEX_LIST = {
            new IntStringPair(0, REGEX_JOB_TITLE_GENDER_SUFFIX),
            new IntStringPair(1, REGEX_WORKLOAD_GENDER_INDICATOR),
            new IntStringPair(1, REGEX_GENDER_INDICATOR),
            new IntStringPair(1, REGEX_WORKLOAD_INDICATOR)
    };

    // special char count
    static final String REGEX_SPECIAL_CHARS = "[^a-zA-Z]+";
    static final double THRESHOLD_RATING_SPECIAL_CHARS = 0.5;

    // text length
    static final int THRESHOLD_TEXT_TOO_LONG = 75;
}
