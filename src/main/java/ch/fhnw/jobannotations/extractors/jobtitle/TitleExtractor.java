package ch.fhnw.jobannotations.extractors.jobtitle;

import ch.fhnw.jobannotations.domain.JobOffer;
import ch.fhnw.jobannotations.extractors.IExtractor;
import ch.fhnw.jobannotations.utils.ConfigurationUtil;
import ch.fhnw.jobannotations.utils.FileUtils;
import ch.fhnw.jobannotations.utils.IntStringPair;
import ch.fhnw.jobannotations.utils.NlpHelper;
import com.aliasi.dict.TrieDictionary;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible to identify potential job titles in a job offer document. To prevent false results and to
 * improve performance, various techniques are used.
 *
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
 */
public class TitleExtractor implements IExtractor {

    private final static Logger LOG = Logger.getLogger(TitleExtractor.class);

    /**
     * Identifies job title candidates found in jobOffer.
     *
     * @param jobOffer to process
     */
    @Override
    public String parse(JobOffer jobOffer) {

        Document document = jobOffer.getDocument().clone();

        LOG.debug("Started to parse job title from offer");

        // remove irrelevant tags
        for (String irrelevantTag : TitleExtractorConstants.IRRELEVANT_TAGS) {
            document.getElementsByTag(irrelevantTag).remove();
        }

        String html = document.html();
        String[] htmlLines = html.split("\\n");

        // extract rated strings
        List<IntStringPair> ratedStrings = extractRatedStringsFromHtml(htmlLines);

        // adjust rating based on job title indicators and clean strings
        adjustRatingsByJobTitleIndicator(ratedStrings);

        // clean all entries and remove entries with empty strings
        Pattern patternSpecialChars = Pattern.compile(TitleExtractorConstants.REGEX_SPECIAL_CHARS_TO_REMOVE);
        for (int i = ratedStrings.size() - 1; i > -1; i--) {
            // remove special characters
            IntStringPair ratedString = ratedStrings.get(i);
            String string = ratedString.getString();
            Matcher matcher = patternSpecialChars.matcher(string);
            while (matcher.find()) {
                string = string.replace(matcher.group(), "");
            }
            string = string.trim();

            if (string.isEmpty()) {
                // remove empty entries
                ratedStrings.remove(i);

            } else {
                // update cleaned entries
                ratedString.setString(string);
            }
        }

        // adjust rating based on special char percentage
        adjustRatingsBySpecialCharCount(ratedStrings);

        // adjust rating based on text length
        adjustRatingsByTextLength(ratedStrings);

        // remove all entries with low ratings
        for (int i = ratedStrings.size() - 1; i > -1; i--) {
            IntStringPair ratedString = ratedStrings.get(i);
            if (ratedString.getInt() < TitleExtractorConstants.LOW_RATING_THRESHOLD) {
                ratedStrings.remove(i);
            }
        }

        if (ratedStrings.isEmpty()) {
            return null;
        }

        // adjust rating based on number of appearances in list
        adjustRatingsByRepetitionCount(ratedStrings);

        // sort according to rating
        ratedStrings.sort((o1, o2) -> o2.getInt() - o1.getInt());

        // only keep top 5 entries
        if (ratedStrings.size() > 5) {
            ratedStrings = ratedStrings.subList(0, 5);
        }

        LOG.debug("Adjusting rating by known job titles");

        // adjust rating by check with known job title list
        adjustRatingsByKnownJobTitleList(ratedStrings);

        return ratedStrings.get(0).getString();
    }

    /**
     * Save found job title into train file
     *
     * @param data found in job offer
     */
    @Override
    public void learn(String data) {
        FileUtils.addDataToTrainFile(ConfigurationUtil.get("extraction.titles.train"), data);
    }

    /**
     * Extracts potential job titles from given HTML text lines.
     *
     * @param htmlLines HTML lines to analyse
     * @return Extracted potential job titles
     */
    private List<IntStringPair> extractRatedStringsFromHtml(String[] htmlLines) {
        List<IntStringPair> extractedRatedStrings = new ArrayList<>();

        for (String htmlLine : htmlLines) {
            int additionalRating = 0;

            // extract all important tags
            String addedImportantTagContent = null;
            for (IntStringPair tagRating : TitleExtractorConstants.RATINGS_HIGH_PRIORITY_TAG) {
                String tagName = tagRating.getString();
                String regex = String.format(TitleExtractorConstants.REGEX_FORMAT_TAG, tagName, tagName);
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(htmlLine);

                while (matcher.find()) {
                    String matchedString = matcher.group();

                    // remove all html tags
                    matchedString = matchedString.replaceAll(TitleExtractorConstants.REGEX_TAG, "").trim();

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
            htmlLine = htmlLine.replaceAll(TitleExtractorConstants.REGEX_TAG, "").trim();

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

    /**
     * Adjust ratings of job titles in given List by using indicators.
     *
     * @param ratedStrings List of job titles to be rated
     */
    private void adjustRatingsByJobTitleIndicator(List<IntStringPair> ratedStrings) {
        for (IntStringPair ratedString : ratedStrings) {

            int jobTitleRating = 0;
            String string = ratedString.getString();

            for (IntStringPair jobTitleIndicatorRating : TitleExtractorConstants.JOB_TITLE_INDICATOR_REGEX_LIST) {
                String jobTitleIndicatorRegex = jobTitleIndicatorRating.getString();
                Pattern pattern = Pattern.compile(jobTitleIndicatorRegex);
                Matcher matcher = pattern.matcher(string);
                while (matcher.find()) {
                    String matchedString = matcher.group();

                    boolean removeMatchedString = jobTitleIndicatorRating.getInt() == 1;
                    if (removeMatchedString) {
                        string = string.replace(matchedString, "").trim();
                    }
                    jobTitleRating += TitleExtractorConstants.RATING_JOB_TITLE_INDICATOR;
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

    /**
     * Adjust ratings of job titles in given List by checking number of special chars.
     *
     * @param ratedStrings List of job titles to be rated
     */
    private void adjustRatingsBySpecialCharCount(List<IntStringPair> ratedStrings) {
        for (IntStringPair ratedString : ratedStrings) {
            String text = ratedString.getString();
            Pattern pattern = Pattern.compile(TitleExtractorConstants.REGEX_SPECIAL_CHARS);
            Matcher matcher = pattern.matcher(text);

            // remove special chars
            while (matcher.find()) {
                String matchedString = matcher.group();
                text = text.replace(matchedString, "");
            }

            double specialCharPercentage = 1 - (double) text.length() / ratedString.getString().length(); // 50% = 0.5, not 50
            if (specialCharPercentage >= TitleExtractorConstants.THRESHOLD_RATING_SPECIAL_CHARS) {
                int ratingAdjustment = (int) (TitleExtractorConstants.RATING_NEGATIVE_MAX_SPECIAL_CHARS * specialCharPercentage);
                int newRating = ratedString.getInt() + ratingAdjustment;
                ratedString.setInt(newRating);
            }
        }
    }

    /**
     * Adjust ratings of job titles in given List by checking length of job title text length.
     *
     * @param ratedStrings List of job titles to be rated
     */
    private void adjustRatingsByTextLength(List<IntStringPair> ratedStrings) {
        for (IntStringPair ratedString : ratedStrings) {
            int ratingMultiplier = ratedString.getString().length() / TitleExtractorConstants.THRESHOLD_TEXT_TOO_LONG;
            if (ratingMultiplier > 0) {
                int ratingAdjustment = ratingMultiplier * TitleExtractorConstants.RATING_NEGATIVE_TEXT_TOO_LONG;
                int newRating = ratedString.getInt() + ratingAdjustment;
                ratedString.setInt(newRating);
            }
        }

    }

    /**
     * Adjust ratings of job titles in given List based on number of duplications.
     *
     * @param ratedStrings List of job titles to be rated
     */
    private void adjustRatingsByRepetitionCount(List<IntStringPair> ratedStrings) {
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
            int additionalRating = counter * TitleExtractorConstants.RATING_REPETITION;
            currentRatedString.setInt(currentRating + additionalRating);
        }
    }

    /**
     * Adjust ratings of job titles in given List by checking them with a dictionary of known job titles.
     *
     * @param ratedStrings List of job titles to be rated
     */
    private void adjustRatingsByKnownJobTitleList(List<IntStringPair> ratedStrings) {
        TrieDictionary<String> titlesDictionary = NlpHelper.getInstance().getTitlesDictionary();

        for (IntStringPair ratedString : ratedStrings) {
            String text = ratedString.getString();


            IntStringPair titleDistance = NlpHelper.getInstance().calcDistanceWithDictionary(titlesDictionary, text, 1);
            if (titleDistance != null && titleDistance.getInt() == 1000) {
                ratedString.setInt(ratedString.getInt() + 100);
            }
        }
    }
}
