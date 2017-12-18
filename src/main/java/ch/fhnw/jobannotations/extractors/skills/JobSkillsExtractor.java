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
 * @author Hoang
 */
public class JobSkillsExtractor implements IExtractor {

    final static Logger LOG = Logger.getLogger(JobSkillsExtractor.class);

    private static final List<String> DETERMINERS_YOUR = Arrays.asList("ihre", "ihr", "deine", "dein");
    private static final List<String> DETERMINERS_OUR = Arrays.asList("unsere", "unser");
    private static final List<String> SUBJECTS_YOU = Arrays.asList("sie", "du");
    private static final List<String> SUBJECTS_WE = Arrays.asList("wir", "ich");
    private static final List<String> NOUNS_SKILL_SYNONYM = Arrays.asList(
            "qualifikation",
            "profil",
            "erfahrung",
            "voraussetzung",
            "anforderung",
            "kompetenz",
            "fähigkeit",
            "kenntniss",
            "eigenschaft",
            "talent"
    );
    private static final List<String> NOUNS_JOB_SYNONYM = Arrays.asList(
            "aufgabe",
            "herausforderung",
            "wirkungsfeld",
            "job",
            "funktion",
            "tätigkeit",
            "arbeit"
    );
    private static final List<String> NOUNS_EXPECTATION_SYNONYM = Arrays.asList(
            "anforderung",
            "anforderungen",
            "erwartung",
            "erwartungen"
    );
    private static final List<String> NOUNS_OFFER_SYNONYM = Arrays.asList("angebot");
    private static final List<String> NOUNS_POSSIBILITY_SYNONYM = Arrays.asList("möglichkeit", "perspektive");
    private static final List<String> VERBS_OFFER = Arrays.asList(
            "bringen",
            "bringst",
            "bieten",
            "bietest",
            "geboten",
            "mitbringen",
            "mitbringst",
            "haben",
            "hast"
    );
    private static final List<String> VERBS_EXPECT = Arrays.asList(
            "erwarten",
            "erwartet",
            "erwartest",
            "verlangen",
            "verlangst",
            "verlangt",
            "finden",
            "findest"
    );

    @Override
    public String parse(JobOffer jobOffer) {

        LOG.debug("Started to parse skills from offer by checking lists");

        Map<IntStringPair, List<String>> ratedSkillLists = extractSkillListsByListParsing(jobOffer);

        if (ratedSkillLists.isEmpty()) {
            // no skills found
            LOG.debug("No skills found with list parsing. Started to parse skills from offer by analysing sentences with NLP");

            ratedSkillLists = extractSkillListsBySentenceAnalyzing(jobOffer);
            if (ratedSkillLists.isEmpty()) {
                LOG.debug("No skills found.");
                return null;
            }
        }

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
        FileUtils.addDataToTrainFile(ConfigurationUtil.get("extraction.skills.train.positive"), data);
    }

    private void formatSkills(Map<IntStringPair, List<String>> ratedSkillLists) {
        for (IntStringPair listTitle : ratedSkillLists.keySet()) {
            List<String> skillSentences = ratedSkillLists.get(listTitle);

            List<String> formattedSkills = extractSkills(skillSentences);

            // replace old list
            ratedSkillLists.put(listTitle, formattedSkills);
        }
    }

    private List<String> extractSkills(List<String> skillSentences) {
        List<String> extractedNouns = NlpHelper.getInstance().extractNouns(skillSentences);
        return extractSkillNouns(extractedNouns);
    }

    private List<String> extractSkillNouns(List<String> nouns) {
        List<IntStringPair> ratedSkillNouns = new ArrayList<>();
        for (String noun : nouns) {
            IntStringPair ratedSkillNoun = getSkillNounRating(noun);
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

    private IntStringPair getSkillNounRating(String noun) {
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
            skillDistance += 3000;
        }
        if (simplifiedSkillWordDistance != null) {
            skillDistance += simplifiedSkillWordDistance.getInt();
        } else {
            skillDistance += 3000;
        }

        int antiSkillDistance = 0;
        if (antiSkillWordDistance != null) {
            antiSkillDistance += antiSkillWordDistance.getInt();
        } else {
            antiSkillDistance += 3000;
        }
        if (simplifiedAntiSkillWordDistance != null) {
            antiSkillDistance += simplifiedAntiSkillWordDistance.getInt();
        } else {
            antiSkillDistance += 3000;
        }

        if (antiSkillDistance > 2000) {
            // special case for acronyms
            if (noun.contains("-")) {
                for (String part : noun.split("-")) {
                    if (StringUtils.isAllUpperCase(part.trim())) {
                        skillDistance -= 1500;
                    }
                }
            } else if (StringUtils.isAllUpperCase(noun)) {
                skillDistance -= 3000;
            }
        }

        // check diff
        int distanceDiff = antiSkillDistance - skillDistance;
        return new IntStringPair(distanceDiff, noun);
    }

    private IntStringPair getDictionaryNounDistance(TrieDictionary<String> dictionary, String noun) {
        // calculate max distance
        int nounLength = noun.length();
        int maxDistance = 0;
        if (nounLength > 4) {
            maxDistance = 1 + nounLength / 10;
        }
        return NlpHelper.getInstance().calcDistanceWithDictionary(dictionary, noun, maxDistance);
    }

    private List<String> extractSkillNouns(String skillSentence) {
        List<String> formattedSkills = new ArrayList<>();
        TrieDictionary<String> skillsDictionary = NlpHelper.getInstance().getSkillsDictionary();
        Map<String, Integer> chunks = PartOfSpeechUtil.getChunksByDictionary(skillsDictionary, skillSentence, 1);
        for (String skill : chunks.keySet()) {
            Integer distance = chunks.get(skill);
            int nofChars = skill.length();
            if (distance > nofChars / 2) {
                // distance too high for this word length
                continue;
            }

            addSkill(formattedSkills, skill);
        }

        List<String> skillNouns = new ArrayList<>();
        for (String formattedSkill : formattedSkills) {
            skillNouns.addAll(NlpHelper.getInstance().extractNouns(formattedSkill));
        }

        return skillNouns;
    }

    private void addSkill(List<String> formattedSkills, String skill) {
        for (String formattedSkill : formattedSkills) {
            String skillLowerCase = skill.toLowerCase();
            String formattedSkillLowerCase = formattedSkill.toLowerCase();
            if (formattedSkillLowerCase.contains(skillLowerCase)) {
                // there is already a skill in the list containing this skill
                // do nothing
                return;

            } else if (skillLowerCase.contains(formattedSkillLowerCase)) {
                // there is a skill in the list that is contained by this skill
                // replace it
                formattedSkills.remove(formattedSkill);
                formattedSkills.add(skill);
                return;
            }
        }

        // nothing special to handle, just add the new skill
        formattedSkills.add(skill);
    }

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
            int rating = calculateSkillListRating(jobOffer, listTitle, listItems);
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

            int rating = 100 + calculateHtmlSkillListItemsRating(listItems);

            List<String> skills = new ArrayList<>();
            for (Element listItem : listItems) {
                skills.add(listItem.text());
            }

            IntStringPair ratedSkillListTitle = new IntStringPair(rating, listTitle);
            ratedSkillLists.put(ratedSkillListTitle, skills);
        }

        return ratedSkillLists;
    }

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

            int rating = rateTitle(jobOffer, line);
            if (rating > 0) {
                parsingSkills = true;
                lastTitle = new IntStringPair(0, line);
                skillLists.put(lastTitle, new ArrayList<>());
            }

        }
        return skillLists;
    }


    private String generateSkillListPrintout(Collection<String> skills) {
        boolean isFirstLine = true;
        StringBuilder sbSkills = new StringBuilder();
        for (String skill : skills) {
            if (!isFirstLine) {
                sbSkills.append("\n");
            }
            sbSkills.append(skill);
            isFirstLine = false;
        }

        return sbSkills.toString();
    }

    private Elements getListElements(Element body) {
        Elements listElements = new Elements();

        // TODO check for sub lists
        // add ul elements
        listElements.addAll(body.getElementsByTag("ul"));

        // add ol elements
        listElements.addAll(body.getElementsByTag("ol"));

        return listElements;
    }

    private int calculateHtmlSkillListItemsRating(Elements listItems) {
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


    private int calculateSkillListRating(JobOffer jobOffer, String skillListTitle, List<String> skills) {
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
        rating += rateTitle(jobOffer, skillListTitle);

        LOG.debug(rating + "\t" + skillListTitle);

        return (int) rating;
    }

    private int rateTitle(JobOffer jobOffer, String title) {
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
                    rating += calculateSingleWordTitleDependencyRating(typedDependency);
                }

            } else {
                for (TypedDependency typedDependency : typedDependencies) {
                    rating += calculateTitleDependencyRating(typedDependency);
                }
            }

        }

        return rating;
    }

    private int calculateTitleDependencyRating(TypedDependency dependency) {
        String shortName = dependency.reln().getShortName();
        String word = dependency.dep().backingLabel().value().toLowerCase();

        IndexedWord gov = dependency.gov();
        String govTag = gov.tag();
        String govWord = gov.backingLabel().value().toLowerCase();

        if (EnglishGrammaticalRelations.DETERMINER.getShortName().equals(shortName)
                && "NN".equals(govTag)) {
            if (DETERMINERS_YOUR.contains(word)) {
                if (wordContainsListItem(NOUNS_SKILL_SYNONYM, govWord)) {
                    return 25;
                } else if (wordContainsListItem(NOUNS_JOB_SYNONYM, govWord)) {
                    return -25;
                } else if (wordContainsListItem(NOUNS_POSSIBILITY_SYNONYM, govWord)) {
                    return -25;
                }
            } else if (DETERMINERS_OUR.contains(word)) {
                if (wordContainsListItem(NOUNS_EXPECTATION_SYNONYM, govWord)) {
                    return 25;
                } else if (wordContainsListItem(NOUNS_OFFER_SYNONYM, govWord)) {
                    return -25;
                }
            }
        } else if ((EnglishGrammaticalRelations.NOMINAL_SUBJECT.getShortName().equals(shortName) ||
                EnglishGrammaticalRelations.DIRECT_OBJECT.getShortName().equals(shortName)) &&
                "VVFIN".equals(govTag)) {
            if (SUBJECTS_YOU.contains(word)) {
                if (VERBS_OFFER.contains(govWord)) {
                    return 50;
                } else if (VERBS_EXPECT.contains(govWord)) {
                    return -50;
                }
            } else if (SUBJECTS_WE.contains(word)) {
                if (VERBS_EXPECT.contains(govWord)) {
                    return 50;
                } else if (VERBS_OFFER.contains(govWord)) {
                    return -50;
                }
            }
        } else if ((EnglishGrammaticalRelations.PUNCTUATION.getShortName().equals(shortName) ||
                "root".equals(shortName))
                && "NN".equals(govTag)) {
            if (wordContainsListItem(NOUNS_SKILL_SYNONYM, govWord)) {
                return 25;
            } else if (wordContainsListItem(NOUNS_EXPECTATION_SYNONYM, govWord)) {
                return 25;
            } else if (wordContainsListItem(NOUNS_JOB_SYNONYM, govWord)) {
                return -35;
            } else if (wordContainsListItem(NOUNS_OFFER_SYNONYM, govWord)) {
                return -35;
            } else if (wordContainsListItem(NOUNS_POSSIBILITY_SYNONYM, govWord)) {
                return -35;
            }
        }

        return 0;
    }

    private int calculateSingleWordTitleDependencyRating(TypedDependency dependency) {
        String tag = dependency.dep().tag();
        String word = dependency.dep().backingLabel().value().toLowerCase();

        if ("NN".equals(tag)) {
            if (wordContainsListItem(NOUNS_SKILL_SYNONYM, word)) {
                return 25;
            } else if (wordContainsListItem(NOUNS_EXPECTATION_SYNONYM, word)) {
                return 25;
            } else if (wordContainsListItem(NOUNS_JOB_SYNONYM, word)) {
                return -35;
            } else if (wordContainsListItem(NOUNS_OFFER_SYNONYM, word)) {
                return -35;
            } else if (wordContainsListItem(NOUNS_POSSIBILITY_SYNONYM, word)) {
                return -35;
            }
        }
        return 0;
    }

    private boolean wordContainsListItem(List<String> list, String term) {
        for (String listItem : list) {
            if (term.contains(listItem)) {
                return true;
            }
        }
        return false;
    }

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

    private int getNumberOfSpecialChars(String text) {
        Matcher specialCharMatcher = Pattern.compile("[^a-zA-Z .,:äöüÄÖÜ]").matcher(text);
        int specialCharCounter = 0;
        while (specialCharMatcher.find()) {
            specialCharCounter++;
        }
        return specialCharCounter;
    }

    private boolean containsSuspiciousChildren(Element listItem) {
        return listItem.getElementsByTag("a").size() > 0
                || listItem.getElementsByTag("input").size() > 0
                || listItem.getElementsByTag("button").size() > 0;
    }

    private int getAllChildCount(Element element) {
        int childCounter = 0;
        for (Element child : element.children()) {
            childCounter++;
            childCounter += getAllChildCount(child);
        }
        return childCounter;
    }

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
}
