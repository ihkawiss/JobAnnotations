package ch.fhnw.jobannotations.extractors.skills;

import ch.fhnw.jobannotations.domain.JobOffer;
import ch.fhnw.jobannotations.extractors.IExtractor;
import ch.fhnw.jobannotations.utils.*;
import com.aliasi.dict.TrieDictionary;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible to identify potential skills in a job offer document. To prevent false results and to
 * improve performance, various techniques are used.
 *
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
 */
public class SkillExtractor implements IExtractor {

    private final static Logger LOG = Logger.getLogger(SkillExtractor.class);

    @Override
    public String parse(JobOffer jobOffer) {

        LOG.debug("Started to parse skills from offer by checking lists");

        Map<IntStringPair, List<String>> ratedSkillLists = extractSkillListsByListParsing(jobOffer);

        if (ratedSkillLists.isEmpty()) {
            // no skills found
            LOG.debug("No skills found with list parsing. Started to parse skills from offer by analysing sentences with NLP");

            ratedSkillLists = extractSkillListsBySentenceAnalyzing(jobOffer);
            if (ratedSkillLists.isEmpty()) {
                return null;
            }
        }

        LOG.debug("No skills found.");
        LOG.debug("Extract nouns from skill lists");

        formatSkills(ratedSkillLists);

        // create single skill list
        HashSet<String> mergedSkillList = new HashSet<>();
        for (IntStringPair ratedListTitle : ratedSkillLists.keySet()) {
            List<String> skills = ratedSkillLists.get(ratedListTitle);
            mergedSkillList.addAll(skills);
        }

        return generateSkillListPrintout(mergedSkillList);
    }

    @Override
    public void learn(String data) {
        data = data.replaceAll(SkillExtractorConstants.SEPARATOR, "\n");
        FileUtils.addDataToTrainFile(ConfigurationUtil.get("extraction.skills.train.positive"), data);
    }

    /**
     * Formats skills of given skill List Map by extracting nouns from the sentences.
     *
     * @param ratedSkillLists Formatted skill List Map
     */
    private void formatSkills(Map<IntStringPair, List<String>> ratedSkillLists) {
        for (IntStringPair listTitle : ratedSkillLists.keySet()) {
            List<String> skillSentences = ratedSkillLists.get(listTitle);

            List<String> formattedSkills = extractSkills(skillSentences);

            // replace old list
            ratedSkillLists.put(listTitle, formattedSkills);
        }
    }

    /**
     * Extracts nouns of given List of skill sentences
     *
     * @param skillSentences List of skill sentences
     * @return List of skill nouns
     */
    private List<String> extractSkills(List<String> skillSentences) {
        List<String> extractedNouns = NlpHelper.getInstance().extractNouns(skillSentences);
        return filterSkillNouns(extractedNouns);
    }

    /**
     * Filters given List of potential skill nouns by rating the probability of being a skill noun and removing those
     * with bad rating.
     *
     * @param nouns List of potential skill nouns
     * @return Filtered List
     */
    private List<String> filterSkillNouns(List<String> nouns) {
        List<IntStringPair> ratedSkillNouns = new ArrayList<>();
        for (String noun : nouns) {
            int rating = calcSkillNounRating(noun);
            IntStringPair ratedSkillNoun = new IntStringPair(rating, noun);
            if (ratedSkillNoun.getInt() >= 0) {
                ratedSkillNouns.add(ratedSkillNoun);
            }

        }

        ratedSkillNouns.sort(Comparator.comparingInt(IntStringPair::getInt));

        List<String> skills = new ArrayList<>();
        for (IntStringPair ratedSkillNoun : ratedSkillNouns) {

            LOG.debug("Rate skill noun with dictionary: " + ratedSkillNoun.getString() + " => " + ratedSkillNoun.getInt());

            skills.add(ratedSkillNoun.getString());
        }

        return skills;
    }

    /**
     * Calculates rating of given potential skill noun by comparing it with dictionaries of known skills and known anti
     * skill.
     *
     * @param noun Potential skill noun
     * @return Calculated rating
     */
    private int calcSkillNounRating(String noun) {
        String simplifiedNoun = StringUtils.simplify(noun);

        // dictionaries
        TrieDictionary<String> skillsDictionary = NlpHelper.getInstance().getSkillsDictionary();
        TrieDictionary<String> antiSkillsDictionary = NlpHelper.getInstance().getAntiSkillsDictionary();
        TrieDictionary<String> simplifiedSkillsDictionary = NlpHelper.getInstance().getSkillsDictionary();
        TrieDictionary<String> simplifiedAntiSkillsDictionary = NlpHelper.getInstance().getAntiSkillsDictionary();

        // get distances
        IntStringPair skillWordDistance = getDictionaryNounDistance(skillsDictionary, noun);
        IntStringPair antiSkillWordDistance = getDictionaryNounDistance(antiSkillsDictionary, noun);
        IntStringPair simplifiedSkillWordDistance = getDictionaryNounDistance(simplifiedSkillsDictionary, simplifiedNoun);
        IntStringPair simplifiedAntiSkillWordDistance = getDictionaryNounDistance(simplifiedAntiSkillsDictionary, simplifiedNoun);

        // combine distances
        int skillDistance = 0;
        if (skillWordDistance != null) {
            skillDistance += skillWordDistance.getInt();
        } else {
            skillDistance += SkillExtractorConstants.SKILL_NOUN_DEFAULT_DISTANCE_ADJUST_VALUE;
        }
        if (simplifiedSkillWordDistance != null) {
            skillDistance += simplifiedSkillWordDistance.getInt();
        } else {
            skillDistance += SkillExtractorConstants.SKILL_NOUN_DEFAULT_DISTANCE_ADJUST_VALUE;
        }

        int antiSkillDistance = 0;
        if (antiSkillWordDistance != null) {
            antiSkillDistance += antiSkillWordDistance.getInt();
        } else {
            antiSkillDistance += SkillExtractorConstants.SKILL_NOUN_DEFAULT_DISTANCE_ADJUST_VALUE;
        }
        if (simplifiedAntiSkillWordDistance != null) {
            antiSkillDistance += simplifiedAntiSkillWordDistance.getInt();
        } else {
            antiSkillDistance += SkillExtractorConstants.SKILL_NOUN_DEFAULT_DISTANCE_ADJUST_VALUE;
        }

        if (antiSkillDistance > (SkillExtractorConstants.SKILL_NOUN_DEFAULT_DISTANCE_ADJUST_VALUE / 3 * 2)) {
            // special case for acronyms
            if (noun.contains("-")) {
                for (String part : noun.split("-")) {
                    if (StringUtils.isAllUpperCase(part.trim())) {
                        skillDistance -= (SkillExtractorConstants.SKILL_NOUN_DEFAULT_DISTANCE_ADJUST_VALUE / 2);
                    }
                }
            } else if (StringUtils.isAllUpperCase(noun)) {
                skillDistance -= SkillExtractorConstants.SKILL_NOUN_DEFAULT_DISTANCE_ADJUST_VALUE;
            }
        }

        // check diff
        return antiSkillDistance - skillDistance;
    }

    /**
     * Calculates distance of given noun to entries of given dictionary.
     *
     * @param dictionary Dictionary to be used for comparison
     * @param noun       Noun to be used for comparison
     * @return IntStringPair of calculated distance and the word that has been analysed
     */
    private IntStringPair getDictionaryNounDistance(TrieDictionary<String> dictionary, String noun) {
        // calculate max distance
        int nounLength = noun.length();
        int maxDistance = 0;
        if (nounLength > 4) {
            maxDistance = 1 + nounLength / 10;
        }
        return NlpHelper.getInstance().calcDistanceWithDictionary(dictionary, noun, maxDistance);
    }

    /**
     * Extracts Lists of skills from given job offer and creates a Map with the title of the skill list as the key and
     * the List of skills as the value.
     *
     * @param jobOffer Job offer to be parsed
     * @return Map of skill Lists and their titles
     */
    private Map<IntStringPair, List<String>> extractSkillListsByListParsing(JobOffer jobOffer) {
        Map<IntStringPair, List<String>> ratedSkillLists = new HashMap<>();

        // add skill lists by getting html list elements
        Map<IntStringPair, List<String>> skillListsByHtmlTags = extractSkillListsByHtmlTags(jobOffer);
        ratedSkillLists.putAll(skillListsByHtmlTags);

        // add skill lists by using list pattern
        Map<IntStringPair, List<String>> skillListsByPattern = extractSkillListsByPattern(jobOffer);
        ratedSkillLists.putAll(skillListsByPattern);

        // calculate rating for skill lists
        List<IntStringPair> ratedSkillListTitles = new ArrayList<>();
        for (IntStringPair ratedListTitle : ratedSkillLists.keySet()) {
            List<String> listItems = ratedSkillLists.get(ratedListTitle);
            String listTitle = ratedListTitle.getString();
            int rating = calcSkillListRating(jobOffer, listTitle, listItems);
            ratedListTitle.addInt(rating);
            ratedSkillListTitles.add(ratedListTitle);
        }

        // remove lists with low ratings
        LOG.debug("Remove skill lists with low rating");

        ratedSkillListTitles.stream()
                .filter(r -> r.getInt() <= 100)
                .forEach(ratedSkillLists::remove);

        return ratedSkillLists;
    }

    /**
     * Extracts Lists of skills by checking the HTML List elements from given job offer and creates a Map with the title
     * of the skill list as the key and the List of skills as the value.
     *
     * @param jobOffer Job offer to be parsed
     * @return Map of skill Lists and their titles
     */
    private Map<IntStringPair, List<String>> extractSkillListsByHtmlTags(JobOffer jobOffer) {
        Map<IntStringPair, List<String>> ratedSkillLists = new HashMap<>();

        // get html list elements from body
        Elements listElements = getListElements(jobOffer.getBodyElement());

        for (Element listElement : listElements) {
            // get child list item elements
            Elements listItems = listElement.getElementsByTag("li");

            // get list title
            String listTitle = getListTitle(jobOffer, listItems);

            if (listTitle == null) {
                continue;
            }

            int rating = 100 + calcHtmlSkillListItemsRating(listItems);

            List<String> skills = new ArrayList<>();
            for (Element listItem : listItems) {
                skills.add(listItem.text());
            }

            IntStringPair ratedSkillListTitle = new IntStringPair(rating, listTitle);
            ratedSkillLists.put(ratedSkillListTitle, skills);
        }

        return ratedSkillLists;
    }

    /**
     * Extracts Lists of skills by checking for plain text lists of given job offer and creates a Map with the title of
     * the skill list as the key and the List of skills as the value.
     *
     * @param jobOffer Job offer to be parsed
     * @return Map of skill Lists and their titles
     */
    private Map<IntStringPair, List<String>> extractSkillListsByPattern(JobOffer jobOffer) {
        Map<IntStringPair, List<String>> ratedSkillLists = new HashMap<>();

        // Find non valid html lists
        String lastBulletPoint = null;
        String lastLine = null;
        String lastListTitle = null;
        List<String> lastListItems = new ArrayList<>();
        String[] lines = jobOffer.getPlainText().split("\\n");
        for (String line : lines) {
            line = line.trim();

            boolean addSkillList = false;

            if (line.isEmpty()) {
                if (lastListTitle != null) {
                    addSkillList = true;
                }
            } else {
                // check for potential list
                Matcher matcher = Pattern.compile("^([^a-zA-ZäöüÄÖÜ])\\s+.*$").matcher(line);
                if (matcher.find()) {
                    String bulletPoint = matcher.group(1);

                    if (lastBulletPoint == null) {
                        // first list item
                        lastBulletPoint = bulletPoint;
                        lastListTitle = lastLine;

                        line = line.substring(1, line.length()).trim();
                        lastListItems.add(line);

                    } else if (!bulletPoint.equals(lastBulletPoint)) {
                        if (lastListTitle != null) {
                            addSkillList = true;
                        }
                    } else if (bulletPoint.equals(lastBulletPoint)) {
                        // additional list item
                        line = line.substring(1, line.length()).trim();
                        lastListItems.add(line);

                    }
                } else {
                    if (lastListTitle != null) {
                        addSkillList = true;
                    }
                }
            }

            if (addSkillList) {
                IntStringPair ratedSkillListTitle = new IntStringPair(100, lastListTitle);
                ratedSkillLists.put(ratedSkillListTitle, lastListItems);
                lastListItems = new ArrayList<>();

                lastListTitle = null;
                lastBulletPoint = null;
            }

            // keep last non empty line
            lastLine = line;
        }

        return ratedSkillLists;
    }

    /**
     * Extracts Lists of skills by checking for potential skill list titles in plaint text of given job offer and
     * creates a Map with the title of the skill list as the key and the List of skills as the value.
     *
     * @param jobOffer Job offer to be parsed
     * @return Map of skill Lists and their titles
     */
    private Map<IntStringPair, List<String>> extractSkillListsBySentenceAnalyzing(JobOffer jobOffer) {
        Map<IntStringPair, List<String>> skillLists = new HashMap<>();

        String[] lines = jobOffer.getPlainText().split("\n");
        IntStringPair lastTitle = null;
        boolean parsingSkills = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                if (parsingSkills) {
                    List<String> skillList = skillLists.get(lastTitle);
                    if (!skillList.isEmpty()) {
                        parsingSkills = false;
                    }
                }
                continue;
            }

            if (parsingSkills) {
                List<String> skillList = skillLists.get(lastTitle);
                if (skillList != null) {
                    skillList.add(line);
                }
            }

            int nofWords = line.split("\\s").length;
            if (nofWords > 6) {
                // ignore lines with many words
                continue;
            }

            int rating = calcSkillListTitleRating(jobOffer, line);
            if (rating > 0) {
                parsingSkills = true;
                lastTitle = new IntStringPair(0, line);
                skillLists.put(lastTitle, new ArrayList<>());
            }

        }
        return skillLists;
    }

    /**
     * Extracts HTML list elements from given body element.
     *
     * @param body Body element to be extracted from
     * @return Extracted list elements
     */
    private Elements getListElements(Element body) {
        Elements listElements = new Elements();

        // add ul elements
        listElements.addAll(body.getElementsByTag("ul"));

        // add ol elements
        listElements.addAll(body.getElementsByTag("ol"));

        return listElements;
    }

    /**
     * Calculates skill list rating of given list.
     *
     * @param listItems List to be rated.
     * @return Calculated rating
     */
    private int calcHtmlSkillListItemsRating(Elements listItems) {
        double probability = 0;
        int nofListItems = listItems.size();
        for (Element listItem : listItems) {
            // adjust rating based on types of children
            if (containsSuspiciousChildren(listItem)) {
                probability -= 75d / nofListItems;
            }

            // adjust rating based on number of children (incl. children of children)
            int allChildCount = getAllChildCount(listItem);
            if (allChildCount > 3) {
                double subtract = (5d / nofListItems);
                subtract *= (allChildCount - 3);
                if (subtract > (25d / nofListItems)) {
                    subtract = (25d / nofListItems);
                }
                probability -= subtract;
            }
        }

        return (int) probability;
    }

    /**
     * Calculates skill list rating.
     *
     * @param jobOffer       Job offer object that will be used for the calculation
     * @param skillListTitle Title of the skill list to be rated
     * @param skills         List of skills to be rated
     * @return Calculated rating
     */
    private int calcSkillListRating(JobOffer jobOffer, String skillListTitle, List<String> skills) {
        double rating = 0;
        int nofListItems = skills.size();

        // adjust rating based on number of list items
        if (nofListItems == 0) {
            return 0;
        } else if (nofListItems == 1) {
            rating -= 25;
        } else if (nofListItems == 2) {
            rating -= 15;
        }

        // adjust rating based on number of special chars
        for (String skill : skills) {
            int nofSpecialChars = getNumberOfSpecialChars(skill);
            if (nofSpecialChars > skill.length() / 4d) {
                double subtract = 75d / nofListItems;
                subtract *= (double) nofSpecialChars / skill.length();
                rating -= subtract;
            }
        }

        // adjust rating based on list title ratings
        rating += calcSkillListTitleRating(jobOffer, skillListTitle);

        LOG.debug(rating + "\t" + skillListTitle);

        return (int) rating;
    }

    /**
     * Calculates rating of the skill list title.
     *
     * @param jobOffer Job offer element to be used for calculations
     * @param title    Title of skill list to be rated
     * @return Calculated rating
     */
    private int calcSkillListTitleRating(JobOffer jobOffer, String title) {
        // check if title is in suspicious element
        String regexSafeTitle = Pattern.quote(title);
        Elements titleElements = jobOffer.getDocument().getElementsMatchingOwnText(regexSafeTitle);
        for (Element element : titleElements) {
            String tagName = element.tagName();
            if (tagName.equalsIgnoreCase("a")
                    || tagName.equalsIgnoreCase("input")
                    || tagName.equalsIgnoreCase("button")) {
                return -15;
            }
        }

        int rating = 0;

        // adjust rating by number of words in title
        int nofWords = title.split("\\s").length;
        if (nofWords > 6) {
            rating -= nofWords * 5;
        }

        List<CoreMap> annotatedSentences = NlpHelper.getInstance().getAnnotatedSentences(title);
        for (CoreMap annotatedSentence : annotatedSentences) {
            SemanticGraph semanticGraph = annotatedSentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
            Collection<TypedDependency> typedDependencies = semanticGraph.typedDependencies();
            if (typedDependencies.size() == 1) {
                for (TypedDependency typedDependency : typedDependencies) {
                    rating += calcSingleWordTitleDependencyRating(typedDependency);
                }

            } else {
                for (TypedDependency typedDependency : typedDependencies) {
                    rating += calcTitleDependencyRating(typedDependency);
                }
            }

        }

        return rating;
    }

    /**
     * Calculates rating of given {@link TypedDependency} that is a part of a skill list title. Rating calculation is
     * based on grammatical type of the word and the grammatical type of the dependency word and their meaning.
     *
     * @param dependency {@link TypedDependency} to be rated
     * @return Calculated rating
     */
    private int calcTitleDependencyRating(TypedDependency dependency) {
        String shortName = dependency.reln().getShortName();
        String word = dependency.dep().backingLabel().value().toLowerCase();

        IndexedWord gov = dependency.gov();
        String govTag = gov.tag();
        String govWord = gov.backingLabel().value().toLowerCase();

        if (EnglishGrammaticalRelations.DETERMINER.getShortName().equals(shortName)
                && "NN".equals(govTag)) {
            if (SkillExtractorConstants.DETERMINERS_YOUR.contains(word)) {
                if (wordContainsListItem(SkillExtractorConstants.NOUNS_SKILL_SYNONYM, govWord)) {
                    return 25;
                } else if (wordContainsListItem(SkillExtractorConstants.NOUNS_JOB_SYNONYM, govWord)) {
                    return -25;
                } else if (wordContainsListItem(SkillExtractorConstants.NOUNS_POSSIBILITY_SYNONYM, govWord)) {
                    return -25;
                }
            } else if (SkillExtractorConstants.DETERMINERS_OUR.contains(word)) {
                if (wordContainsListItem(SkillExtractorConstants.NOUNS_EXPECTATION_SYNONYM, govWord)) {
                    return 25;
                } else if (wordContainsListItem(SkillExtractorConstants.NOUNS_OFFER_SYNONYM, govWord)) {
                    return -25;
                }
            }
        } else if ((EnglishGrammaticalRelations.NOMINAL_SUBJECT.getShortName().equals(shortName) ||
                EnglishGrammaticalRelations.DIRECT_OBJECT.getShortName().equals(shortName)) &&
                "VVFIN".equals(govTag)) {
            if (SkillExtractorConstants.SUBJECTS_YOU.contains(word)) {
                if (SkillExtractorConstants.VERBS_OFFER.contains(govWord)) {
                    return 50;
                } else if (SkillExtractorConstants.VERBS_EXPECT.contains(govWord)) {
                    return -50;
                }
            } else if (SkillExtractorConstants.SUBJECTS_WE.contains(word)) {
                if (SkillExtractorConstants.VERBS_EXPECT.contains(govWord)) {
                    return 50;
                } else if (SkillExtractorConstants.VERBS_OFFER.contains(govWord)) {
                    return -50;
                }
            }
        } else if ((EnglishGrammaticalRelations.PUNCTUATION.getShortName().equals(shortName) ||
                "root".equals(shortName))
                && "NN".equals(govTag)) {
            if (wordContainsListItem(SkillExtractorConstants.NOUNS_SKILL_SYNONYM, govWord)) {
                return 25;
            } else if (wordContainsListItem(SkillExtractorConstants.NOUNS_EXPECTATION_SYNONYM, govWord)) {
                return 25;
            } else if (wordContainsListItem(SkillExtractorConstants.NOUNS_JOB_SYNONYM, govWord)) {
                return -35;
            } else if (wordContainsListItem(SkillExtractorConstants.NOUNS_OFFER_SYNONYM, govWord)) {
                return -35;
            } else if (wordContainsListItem(SkillExtractorConstants.NOUNS_POSSIBILITY_SYNONYM, govWord)) {
                return -35;
            }
        }

        return 0;
    }

    /**
     * Calculates rating of single word skill list title. Rating is based on grammatical type of the word.
     *
     * @param dependency {@link TypedDependency} of the word to be rated
     * @return Calculated rating
     */
    private int calcSingleWordTitleDependencyRating(TypedDependency dependency) {
        String tag = dependency.dep().tag();
        String word = dependency.dep().backingLabel().value().toLowerCase();

        if ("NN".equals(tag)) {
            if (wordContainsListItem(SkillExtractorConstants.NOUNS_SKILL_SYNONYM, word)) {
                return 25;
            } else if (wordContainsListItem(SkillExtractorConstants.NOUNS_EXPECTATION_SYNONYM, word)) {
                return 25;
            } else if (wordContainsListItem(SkillExtractorConstants.NOUNS_JOB_SYNONYM, word)) {
                return -35;
            } else if (wordContainsListItem(SkillExtractorConstants.NOUNS_OFFER_SYNONYM, word)) {
                return -35;
            } else if (wordContainsListItem(SkillExtractorConstants.NOUNS_POSSIBILITY_SYNONYM, word)) {
                return -35;
            }
        }
        return 0;
    }

    /**
     * Checks whether the given term contains an entry of the given List.
     *
     * @param list List of strings to be checked
     * @param term String that might contain a word of the list
     * @return <code>true</code> if the term contains an list entry, <code>false</code> otherwise
     */
    private boolean wordContainsListItem(List<String> list, String term) {
        for (String listItem : list) {
            if (term.contains(listItem)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts title of the given List from the given job offer.
     *
     * @param jobOffer  Job offer object to be analysed
     * @param listItems List whose title will be extracted
     * @return Extracted list title or null if nothing found
     */
    private String getListTitle(JobOffer jobOffer, Elements listItems) {
        String firstListText = null;
        for (Element listItem : listItems) {
            firstListText = HtmlUtils.getPlainTextFromHtml(listItem.html()).trim();
            firstListText = getLastLine(firstListText);
            if (firstListText != null) {
                break;
            }
        }

        if (firstListText == null) {
            return null;
        }

        String jobOfferText = jobOffer.getPlainText();
        int firstListItemIndex = jobOfferText.indexOf(firstListText);
        String listTitle = jobOfferText.substring(0, firstListItemIndex);
        return getLastLine(listTitle);
    }

    /**
     * Counts number of special characters in given text.
     *
     * @param text Text to be analysed
     * @return Number of special characters
     */
    private int getNumberOfSpecialChars(String text) {
        Matcher specialCharMatcher = Pattern.compile(SkillExtractorConstants.SPECIAL_CHARACTER_REGEX).matcher(text);
        int specialCharCounter = 0;
        while (specialCharMatcher.find()) {
            specialCharCounter++;
        }
        return specialCharCounter;
    }

    /**
     * Checks whether the given potential skill list element contains suspicious HTML elements that should not be in a
     * skill list element.
     *
     * @param listItem Skill list element to be checked
     * @return <code>true</code> if the element contains suspicious elements, <code>false</code> otherwise
     */
    private boolean containsSuspiciousChildren(Element listItem) {
        return listItem.getElementsByTag("a").size() > 0
                || listItem.getElementsByTag("input").size() > 0
                || listItem.getElementsByTag("button").size() > 0;
    }

    /**
     * Counts number of child elements in given element. Children of children are also included.
     *
     * @param element Element to be analysed
     * @return Number of child elements
     */
    private int getAllChildCount(Element element) {
        int childCounter = 0;
        for (Element child : element.children()) {
            childCounter++;
            childCounter += getAllChildCount(child);
        }
        return childCounter;
    }

    /**
     * Extract last line of given text.
     *
     * @param text Text to be extracted from
     * @return Extracted last line or null if nothing found
     */
    private String getLastLine(String text) {
        text = text.trim();
        String[] lines = text.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];
            line = line.trim();
            if (!line.isEmpty()) {
                return line;
            }
        }
        return null;
    }

    /**
     * Creates a single String of the given skill List to be used as the return value of the {@link #parse(JobOffer)}
     * method.
     *
     * @param skills List of skills to be formatted to a single String
     * @return Single String of skills
     */
    private String generateSkillListPrintout(Collection<String> skills) {
        boolean isFirstLine = true;
        StringBuilder sbSkills = new StringBuilder();
        for (String skill : skills) {
            if (!isFirstLine) {
                sbSkills.append(SkillExtractorConstants.SEPARATOR);
            }
            sbSkills.append(skill);
            isFirstLine = false;
        }

        return sbSkills.toString();
    }
}
