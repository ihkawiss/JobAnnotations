package ch.fhnw.jobannotations.extractors.workload;

import ch.fhnw.jobannotations.domain.JobOffer;
import ch.fhnw.jobannotations.extractors.IExtractor;
import ch.fhnw.jobannotations.utils.IntStringPair;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class is responsible to identify workload in a job offer document.
 *
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
 */
public class WorkloadExtractor implements IExtractor {

    final static Logger LOG = Logger.getLogger(WorkloadExtractor.class);

    public static final String WORKLOAD_REGEX = "(\\d+\\s*%?\\s*(.|\\w+)\\s*)?\\d+\\s*%";
    private static final int RATING_REPETITION = 25;
    private static final int RATING_UNLIKELY_WORKLOAD = -50;

    @Override
    public String parse(JobOffer jobOffer) {

        LOG.debug("Started to parse workload from offer");

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
     * Removes invalid workloads and decreases ratings of unlikely workloads
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
     * Removes duplicated entries and adjusts rating by number of duplications.
     *
     * @param ratedWorkloads List of workloads to be rated
     */
    private void removeDuplications(List<IntStringPair> ratedWorkloads) {
        LOG.debug("Remove duplicates and adjust rating by number of identical entries");
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

    /**
     * Filters given List of workloads by removing workloads that do not have the highest rating in the Lists so that
     * after filtering all workloads have the same rating.
     *
     * @param ratedWorkloads List of workloads to be rated
     */
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
