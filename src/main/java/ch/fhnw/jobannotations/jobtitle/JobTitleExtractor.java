package ch.fhnw.jobannotations.jobtitle;

import ch.fhnw.jobannotations.JobOffer;
import ch.fhnw.jobannotations.jobtitle.rating.JobTitleStringRatingManager;
import ch.fhnw.jobannotations.utils.IntStringPair;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hoang
 */
public class JobTitleExtractor {
    private static final String REGEX_SPECIAL_CHARS_TO_REMOVE = "[$+,:;=?@#<>.^*%!-\"]";
    private static final String[] IRRELEVANT_TAGS = {
            "style",
            "script",
            "meta",
            "link",
            "a",
            "input",
            "button"
    };

    private JobTitleStringRatingManager jobTitleStringRatingManager;
    private boolean cleanJobTitle = true;

    public JobTitleExtractor() {
        jobTitleStringRatingManager = new JobTitleStringRatingManager();
    }

    public String parseJobTitle(JobOffer jobOffer) {
        Document document = jobOffer.getDocument().clone();

        // remove irrelevant tags
        for (String irrelevantTag : IRRELEVANT_TAGS) {
            document.getElementsByTag(irrelevantTag).remove();
        }

        String html = document.html();
        String[] htmlLines = html.split("\\n");

        // extract rated strings
        List<IntStringPair> ratedStrings = jobTitleStringRatingManager.extractRatedStringsFromHtml(htmlLines);

        // adjust rating based on job title indicators and clean strings
        jobTitleStringRatingManager.adjustRatingsByJobTitleIndicator(ratedStrings, cleanJobTitle);

        if (cleanJobTitle) {
            // clean all entries and remove entries with empty strings
            Pattern patternSpecialChars = Pattern.compile(REGEX_SPECIAL_CHARS_TO_REMOVE);
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
        }

        // adjust rating based on special char percentage
        jobTitleStringRatingManager.adjustRatingsBySpecialCharCount(ratedStrings);

        // adjust rating based on text length
        jobTitleStringRatingManager.adjustRatingsByTextLength(ratedStrings);

        // remove all entries with low ratings
        for (int i = ratedStrings.size() - 1; i > -1; i--) {
            IntStringPair ratedString = ratedStrings.get(i);
            if (ratedString.getInt() < JobTitleStringRatingManager.LOW_RATING_THRESHOLD) {
                ratedStrings.remove(i);
            }
        }

        if (ratedStrings.isEmpty()) {
            return null;
        }

        // adjust rating based on number of appearances in list
        jobTitleStringRatingManager.adjustRatingsByRepetitionCount(ratedStrings);

        // sort according to rating
        ratedStrings.sort((o1, o2) -> o2.getInt() - o1.getInt());

        // only keep top 5 entries
        if (ratedStrings.size() > 5) {
            ratedStrings = ratedStrings.subList(0, 5);
        }

        for (IntStringPair ratedString : ratedStrings) {
            System.out.println(ratedString.toString());
        }

        System.out.println("adjustRatingsByKnownJobTitleList");

        // adjust rating by check with known job title list
        jobTitleStringRatingManager.adjustRatingsByKnownJobTitleList(ratedStrings);


        return ratedStrings.get(0).getString();
    }

    public JobTitleStringRatingManager getJobTitleStringRatingManager() {
        return jobTitleStringRatingManager;
    }

    public void setCleanJobTitle(boolean cleanJobTitle) {
        this.cleanJobTitle = cleanJobTitle;
    }
}
