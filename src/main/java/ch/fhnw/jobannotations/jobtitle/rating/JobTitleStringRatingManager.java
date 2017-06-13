package ch.fhnw.jobannotations.jobtitle.rating;

import ch.fhnw.jobannotations.utils.FileUtils;
import ch.fhnw.jobannotations.utils.IntStringPair;
import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.ApproxDictionaryChunker;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.TrieDictionary;
import com.aliasi.spell.FixedWeightEditDistance;
import com.aliasi.spell.WeightedEditDistance;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hoang
 */
public class JobTitleStringRatingManager {

    public static final int LOW_RATING_THRESHOLD = 10;

    private static final String JOB_TITLES_TRAIN_FILE_NAME = "jobtitles.fhnw.raw";

    private static final int KNOWN_LIST_MATCH_MAX_RATING = 100;
    private static final int JOB_TITLE_INDICATOR_RATING = 50;
    private static final int TAG_RATING_H1 = 20;
    private static final int REPETITION_RATING = JOB_TITLE_INDICATOR_RATING / 2 * TAG_RATING_H1;

    private static final String REGEX_TAG = "<[^>]+>";
    private static final String REGEX_FORMAT_TAG = "<%s[^>]*>.*<\\/%s>";
    private static final String REGEX_JOB_TITLE_GENDER_SUFFIX = "\\w[-\\/\\(\\|]+(in|IN|r|R)|\\w[-\\/\\(\\|]*In";
    private static final String REGEX_GENDER_TEXT = "\\s?[wmfWMF]\\s?\\/\\s?[wmfWMF]\\s?";
    private static final String REGEX_WORKLOAD_TEXT = "\\s?(\\d+\\s?%?\\s?-\\s?)?\\d+\\s?%\\s?";
    private static final String REGEX_GENDER_INDICATOR = "\\(?" + REGEX_GENDER_TEXT + "\\)?";
    private static final String REGEX_WORKLOAD_INDICATOR = "\\(?" + REGEX_WORKLOAD_TEXT + "\\)?";
    private static final String REGEX_WORKLOAD_GENDER_INDICATOR = "\\((" + REGEX_WORKLOAD_TEXT + ")[^\\(\\)]*(" + REGEX_GENDER_TEXT + ")\\)|\\((" + REGEX_GENDER_TEXT + ")[^\\(\\)]*(" + REGEX_WORKLOAD_TEXT + ")\\)";
    private static final String DICTIONARY_CATEGORY_JOB_TITLE = "JobTitle";

    private final TrieDictionary<String> dictionary;

    public JobTitleStringRatingManager() {
        dictionary = initJobTitleDictionary();
    }

    private TrieDictionary<String> initJobTitleDictionary() {
        TrieDictionary<String> dictionary = new TrieDictionary<>();
        List<String> knownJobTitles = new ArrayList<>();

        FileInputStream fileInputStream = FileUtils.getFileInputStream(JOB_TITLES_TRAIN_FILE_NAME);
        if (fileInputStream != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));

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

    /**
     * Array of high priority tags with corresponding rating value
     */
    private static final IntStringPair[] HIGH_PRIORITY_TAG_RATINGS = {
            new IntStringPair(TAG_RATING_H1, "h1"),
            new IntStringPair(TAG_RATING_H1 / 4 * 3, "h2"),
            new IntStringPair(TAG_RATING_H1 / 2, "h3"),
            new IntStringPair(TAG_RATING_H1 / 4, "h4"),
            new IntStringPair(TAG_RATING_H1 / 4, "h5"),
            new IntStringPair(TAG_RATING_H1 / 4, "h6"),
            new IntStringPair(TAG_RATING_H1 / 4 * 3, "title"),
            new IntStringPair(TAG_RATING_H1 / 4, "b"),
            new IntStringPair(TAG_RATING_H1 / 4, "strong"),
            new IntStringPair(-TAG_RATING_H1, "li")
    };

    /**
     * Array of job title indicator regex. Int value defines whether matched String should be removed or not (1 = true)
     */
    private static final IntStringPair[] JOB_TITLE_INDICATOR_REGEX_LIST = {
            new IntStringPair(0, REGEX_JOB_TITLE_GENDER_SUFFIX),
            new IntStringPair(1, REGEX_WORKLOAD_GENDER_INDICATOR),
            new IntStringPair(1, REGEX_GENDER_INDICATOR),
            new IntStringPair(1, REGEX_WORKLOAD_INDICATOR)
    };

    public List<IntStringPair> extractRatedStringsFromHtml(String[] htmlLines) {
        List<IntStringPair> extractedRatedStrings = new ArrayList<>();

        for (String htmlLine : htmlLines) {
            int additionalRating = 0;

            // extract all important tags
            String addedImportantTagContent = null;
            for (IntStringPair tagRating : HIGH_PRIORITY_TAG_RATINGS) {
                String tagName = tagRating.getString();
                String regex = String.format(REGEX_FORMAT_TAG, tagName, tagName);
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(htmlLine);

                while (matcher.find()) {
                    String matchedString = matcher.group();

                    // remove all html tags
                    matchedString = matchedString.replaceAll(REGEX_TAG, "").trim();

                    if (!matchedString.isEmpty()) {
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

            if (!htmlLine.isEmpty() && !htmlLine.equals(addedImportantTagContent)) {
                // add remaining text with base rating of 0 (+ additionalRating)
                IntStringPair ratedString = new IntStringPair(additionalRating, htmlLine);
                extractedRatedStrings.add(ratedString);
            }

        }

        return extractedRatedStrings;
    }

    public void cleanAndAdjustRatingsByJobTitleIndicator(List<IntStringPair> ratedStrings) {
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
                    if (removeMatchedString) {
                        string = string.replace(matchedString, "").trim();
                    }
                    jobTitleRating += JOB_TITLE_INDICATOR_RATING;
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
            int additionalRating = counter * REPETITION_RATING;
            currentRatedString.setInt(currentRating + additionalRating);
        }
    }

    public void adjustRatingByKnownJobTitleList(List<IntStringPair> ratedStrings) {
        IndoEuropeanTokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        WeightedEditDistance editDistance = new FixedWeightEditDistance(0, -2, -2, -2, Double.NEGATIVE_INFINITY);

        double maxDistance = 2;

        ApproxDictionaryChunker chunker = new ApproxDictionaryChunker(dictionary, tokenizerFactory, editDistance, maxDistance);

        for (IntStringPair ratedString : ratedStrings) {
            String text = ratedString.getString();
            System.out.println("\n\n " + text + "\n");
            Chunking chunking = chunker.chunk(text);
            CharSequence cs = chunking.charSequence();
            Set<Chunk> chunkSet = chunking.chunkSet();

            System.out.printf("%15s  %15s   %8s\n",
                    "Matched Phrase",
                    "Dict Entry",
                    "Distance");
            for (Chunk chunk : chunkSet) {
                int start = chunk.start();
                int end = chunk.end();
                CharSequence str = cs.subSequence(start, end);
                double distance = chunk.score();
                String match = chunk.type();
                System.out.printf("%15s  %15s   %8.1f\n",
                        str, match, distance);
            }
        }


        for (IntStringPair ratedString : ratedStrings) {
            String text = ratedString.getString();

            System.out.println("\n\n " + text + "\n");
            Chunking chunking = chunker.chunk(text);
            CharSequence cs = chunking.charSequence();
            Set<Chunk> chunkSet = chunking.chunkSet();

            System.out.printf("%15s  %15s   %8s\n", "Matched Phrase", "Dict Entry", "Distance");

            List<int[]> tuples = new ArrayList<int[]>();

            for (Chunk chunk : chunkSet) {

                // get start an end position of chunk
                int start = chunk.start();
                int end = chunk.end();

                int[] data = {start, end};

                tuples.add(data);

                System.out.println(chunk.score());
            }

            List<int[]> filteredList = new ArrayList<>(tuples);

            int previousListSize;
            do {
                previousListSize = filteredList.size();
                for (int[] ints : tuples) {
                    for (int i = filteredList.size() - 1; i > -1; i--) {
                        int[] potentialChild = filteredList.get(i);
                        if (potentialChild[0] >= ints[0] && potentialChild[1] < ints[1]
                                || potentialChild[0] > ints[0] && potentialChild[1] <= ints[1]) {
                            filteredList.remove(i);
                        }
                    }
                }

            } while (previousListSize > filteredList.size());

            for (int[] ints : filteredList) {
                System.out.println(text.substring(ints[0], ints[1]));
            }

        }
    }
}
