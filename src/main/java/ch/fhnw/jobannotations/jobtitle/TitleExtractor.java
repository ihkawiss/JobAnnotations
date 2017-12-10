package ch.fhnw.jobannotations.jobtitle;

import ch.fhnw.jobannotations.JobOffer;
import ch.fhnw.jobannotations.utils.IntStringPair;
import ch.fhnw.jobannotations.workload.JobWorkloadExtractor;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hoang
 */
public class TitleExtractor {
    private static final String[] IRRELEVANT_TAGS = {
            "style",
            "script",
            "meta",
            "link",
            "a",
            "input",
            "button"
    };
    private static final int RATING_TAG_H1 = 20;
    private static final int RATING_JOB_TITLE_INDICATOR = 50;
    private static final int RATING_REPETITION = RATING_JOB_TITLE_INDICATOR / 2 * RATING_TAG_H1;
    private static final int RATING_MAX_KNOWN_LIST_MATCH = RATING_JOB_TITLE_INDICATOR * 2;
    private static final int RATING_NEGATIVE_MAX_SPECIAL_CHARS = -RATING_JOB_TITLE_INDICATOR;
    private static final int RATING_NEGATIVE_TEXT_TOO_LONG = -RATING_JOB_TITLE_INDICATOR;
    private static final IntStringPair[] RATINGS_HIGH_PRIORITY_TAG = {
            new IntStringPair(RATING_TAG_H1, "h1"),
            new IntStringPair(RATING_TAG_H1 / 4 * 3, "h2"),
            new IntStringPair(RATING_TAG_H1 / 2, "h3"),
            new IntStringPair(RATING_TAG_H1 / 4, "h4"),
            new IntStringPair(RATING_TAG_H1 / 4, "h5"),
            new IntStringPair(RATING_TAG_H1 / 4, "h6"),
            new IntStringPair(RATING_TAG_H1 / 4 * 3, "title"),
            new IntStringPair(RATING_TAG_H1 / 4, "b"),
            new IntStringPair(RATING_TAG_H1 / 4, "strong")
    };

    private static final String REGEX_GENDER_TEXT = "\\s?[wmfWMF]\\s?\\/\\s?[wmfWMF]\\s?";
    private static final String REGEX_JOB_TITLE_GENDER_SUFFIX = "\\w[-\\/\\(\\|]+(in|IN|r|R)|\\w[-\\/\\(\\|]*In";
    private static final String REGEX_WORKLOAD_INDICATOR = JobWorkloadExtractor.WORKLOAD_REGEX;


    public String parse(JobOffer jobOffer) {
        Element bodyElement = jobOffer.getBodyElement();

        removeIrrelevantElements(bodyElement);

        List<IntStringPair> ratedTitles = getTitlesFromInterestingTags(bodyElement);

        Elements elementsMatchingText = bodyElement.getElementsMatchingOwnText(REGEX_GENDER_TEXT);

        return null;
    }

    private void removeIrrelevantElements(Element bodyElement) {
        for (String irrelevantTag : IRRELEVANT_TAGS) {
            bodyElement.getElementsByTag(irrelevantTag).remove();
        }
    }

    private List<IntStringPair> getTitlesFromInterestingTags(Element bodyElement) {
        List<IntStringPair> interestingTags = new ArrayList<>();
        for (IntStringPair tagRatings : RATINGS_HIGH_PRIORITY_TAG) {
            String tagName = tagRatings.getString();
            int tagRating = tagRatings.getInt();
            for (Element element : bodyElement.getElementsByTag(tagName)) {
                IntStringPair ratedElement = new IntStringPair(tagRating, element.text(), element.clone());
                interestingTags.add(ratedElement);
            }
        }
        return interestingTags;
    }

    private List<IntStringPair> getTitlesByIndicators(Element bodyElement) {
        return null;
    }
}
