package ch.fhnw.jobannotations.extractors.workload;

import ch.fhnw.jobannotations.domain.JobOffer;
import ch.fhnw.jobannotations.extractors.IExtractor;
import ch.fhnw.jobannotations.utils.ConfigurationUtil;
import ch.fhnw.jobannotations.utils.IntStringPair;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hoang
 */
public class JobWorkloadExtractor implements IExtractor {

    public static final String WORKLOAD_REGEX = "(\\d+\\s*%?\\s*(.|\\w+)\\s*)?\\d+\\s*%";
    private static final int RATING_REPETITION = 25;
    private static final int RATING_UNLIKELY_WORKLOAD = -50;

    @Override
    public String parse(JobOffer jobOffer) {

        if (ConfigurationUtil.isDebugModeEnabled()) {
            System.out.println("\n" + StringUtils.repeat("-", 80));
            System.out.println("[jobtitle-indicator]\t" + "Started to parse workload from offer");
        }

        String text = jobOffer.getPlainText();

        List<IntStringPair> ratedWorkloads = new ArrayList<>();

        Matcher workloadMatcher = Pattern.compile(WORKLOAD_REGEX).matcher(text);
        while (workloadMatcher.find()) {
            String match = workloadMatcher.group();
            IntStringPair ratedWorkload = new IntStringPair(100, match);
            ratedWorkloads.add(ratedWorkload);
        }

        if (ratedWorkloads.isEmpty()) {
            return null;
        }

        validateWorkloads(ratedWorkloads);
        removeDuplications(ratedWorkloads);
        filterByRatings(ratedWorkloads);

        if (ratedWorkloads.isEmpty()) {
            return null;
        }

        return ratedWorkloads.get(0).getString();
    }

    @Override
    public void learn(String data) {
        // NOP - makes currently no sense
    }

    /**
     * Remove invalid workloads and decrease ratings of unlikely workloads
     *
     * @param ratedWorkloads List of rated workloads
     */
    private void validateWorkloads(List<IntStringPair> ratedWorkloads) {
        outer:
        for (int i = ratedWorkloads.size() - 1; i >= 0; i--) {
            IntStringPair ratedWorkload = ratedWorkloads.get(i);

            // get digits
            List<Integer> digits = new ArrayList<>();
            Matcher digitMatcher = Pattern.compile("\\d+").matcher(ratedWorkload.getString());
            while (digitMatcher.find()) {
                digits.add(Integer.parseInt(digitMatcher.group()));
            }

            // remove workloads with invalid numbers of digits
            if (digits.size() < 1 || digits.size() > 3) {
                ratedWorkloads.remove(i);
                continue;
            }

            // remove workloads with invalid workload values
            for (Integer digit : digits) {
                if (digit < 1 || digit > 100) {
                    ratedWorkloads.remove(i);
                    continue outer;
                }
            }

            // decrease ratings of workloads with unlikely values
            for (Integer digit : digits) {
                if (digit % 5 != 0) {
                    ratedWorkload.setInt(ratedWorkload.getInt() + RATING_UNLIKELY_WORKLOAD);
                }
            }
        }
    }

    /**
     * Removes duplicated entries and adjust rating by number of duplications.
     */
    private void removeDuplications(List<IntStringPair> ratedWorkloads) {
        System.out.println("[workload]\tRemove duplicates and adjust rating by number of identical entries");
        ratedWorkloads.sort(Comparator.comparing(IntStringPair::getString));
        String lastLocation = null;
        for (int i = ratedWorkloads.size() - 1; i >= 0; i--) {
            IntStringPair ratedWorkload = ratedWorkloads.get(i);
            if (ratedWorkload.getString().equals(lastLocation)) {
                int ratingDifference = ratedWorkloads.get(i + 1).getInt() - ratedWorkload.getInt();
                if (ratingDifference < 0) {
                    ratingDifference = 0;
                }
                int additionalRating = RATING_REPETITION + ratingDifference;
                ratedWorkloads.remove(i + 1);
                ratedWorkload.setInt(ratedWorkload.getInt() + additionalRating);

            }
            lastLocation = ratedWorkload.getString();
        }
    }

    private void filterByRatings(List<IntStringPair> ratedWorkloads) {
        // sort by rating
        ratedWorkloads.sort((o1, o2) -> o2.getInt() - o1.getInt());

        // remove entries with lower ratings
        int highestRating = ratedWorkloads.get(0).getInt();
        for (int i = ratedWorkloads.size() - 1; i > 0; i--) {
            if (ratedWorkloads.get(i).getInt() < highestRating) {
                ratedWorkloads.remove(i);
            }
        }
    }
}
