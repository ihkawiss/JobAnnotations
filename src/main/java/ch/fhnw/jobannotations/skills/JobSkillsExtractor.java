package ch.fhnw.jobannotations.skills;

import ch.fhnw.jobannotations.JobOffer;
import ch.fhnw.jobannotations.Main;
import ch.fhnw.jobannotations.utils.FileUtils;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.TypedDependency;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hoang
 */
public class JobSkillsExtractor {

    private static MaxentTagger tagger;
    private static DependencyParser parser;
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
            "eigenschaft"
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
    private static final List<String> NOUNS_EXPECTATION_SYNONYM = Arrays.asList("anforderung", "erwartung");
    private static final List<String> NOUNS_OFFER_SYNONYM = Arrays.asList("angebot");
    private static final List<String> NOUNS_POSSIBILITY_SYNONYM = Arrays.asList("möglichkeit", "perspektive");
    private static final List<String> VERBS_OFFER = Arrays.asList(
            "bringen",
            "bringst",
            "bieten",
            "bietest",
            "mitbringen",
            "mitbringst"
    );
    private static final List<String> VERBS_EXPECT = Arrays.asList(
            "erwarten",
            "verlangen"
    );

    public String parseJobSkills(JobOffer jobOffer) {

        if (Main.DEBUG) {
            System.out.println("\n" + StringUtils.repeat("-", 80));
            System.out.println("[skills-indicator]\t" + "Started to parse skills from offer");
        }

        Element body = jobOffer.getBodyElement();
        Map<List<String>, Integer> ratedSkillLists = new HashMap<>();

        Elements listElements = new Elements();

        // add ul elements
        listElements.addAll(body.getElementsByTag("ul"));

        // add ol elements
        listElements.addAll(body.getElementsByTag("ol"));

        // calculate rating for skill lists
        for (Element listElement : listElements) {
            int skillListRating = calculateSkillListProbability(jobOffer, listElement);
            if (skillListRating >= 100) {
                List<String> skills = new ArrayList<>();
                for (Element listItemElement : listElement.getElementsByTag("li")) {
                    String skill = listItemElement.text().trim();
                    if (!skills.contains(skill)) {
                        skills.add(skill);
                    }
                }
                ratedSkillLists.put(skills, skillListRating);
            }
        }


        // find fake lists
        String lastBulletPoint = null;
        String lastLine = null;
        String listTitle = null;
        List<String> listItems = new ArrayList<>();
        Map<String, List<String>> lists = new HashMap<>();
        String[] lines = jobOffer.getPlainText().split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                if (listTitle != null) {
                    // list ended
                    lists.put(listTitle, listItems);
                    listItems = new ArrayList<>();

                    listTitle = null;
                    lastBulletPoint = null;
                }
                continue;
            }

            // check for potential list
            Matcher matcher = Pattern.compile("^([^a-zA-ZäöüÄÖÜ])\\s+.*$").matcher(line);
            if (matcher.find()) {
                String bulletPoint = matcher.group(1);

                if (lastBulletPoint == null) {
                    // first list item
                    lastBulletPoint = bulletPoint;
                    listTitle = lastLine;

                    line = line.substring(1, line.length()).trim();
                    listItems.add(line);

                } else if (!bulletPoint.equals(lastBulletPoint)) {
                    if (listTitle != null) {
                        // list ended
                        lists.put(listTitle, listItems);
                        listItems = new ArrayList<>();

                        listTitle = null;
                        lastBulletPoint = null;
                    }
                } else if (bulletPoint.equals(lastBulletPoint)) {
                    // additional list item
                    line = line.substring(1, line.length()).trim();
                    listItems.add(line);

                }
            } else {
                if (listTitle != null) {
                    // list ended
                    lists.put(listTitle, listItems);
                    listItems = new ArrayList<>();

                    listTitle = null;
                    lastBulletPoint = null;
                }
            }

            // keep last non empty line
            lastLine = line;
        }
        // calculate rating for fake skill lists
        for (String title : lists.keySet()) {
            List<String> items = lists.get(title);
            int skillListRating = calculateSkillListProbability(jobOffer, title, items);
            if (skillListRating >= 100) {
                ratedSkillLists.put(items, skillListRating);
            }
        }

        if (ratedSkillLists.isEmpty()) {
            return null;
        }

        StringBuilder sbSkills = new StringBuilder();
        for (List<String> skills : ratedSkillLists.keySet()) {
            for (String skill : skills) {
                sbSkills.append("\n").append(skill);
            }

        }

        return sbSkills.toString();
    }

    private int calculateSkillListProbability(JobOffer jobOffer, Element listElement) {
        double probability = 100;
        Elements listItems = listElement.getElementsByTag("li");
        int nofListItems = listItems.size();

        // adjust rating based on number of list items
        if (nofListItems == 0) {
            return 0;
        } else if (nofListItems == 1) {
            probability -= 25;
        } else if (nofListItems == 2) {
            probability -= 15;
        }

        initDependencyParser();

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

            // adjust rating based on number of special chars
            String text = listItem.text();
            int nofSpecialChars = getNumberOfSpecialChars(text);
            if (nofSpecialChars > text.length() / 4d) {
                double subtract = 75d / nofListItems;
                subtract *= (double) nofSpecialChars / text.length();
                probability -= subtract;
            }
        }

        // adjust rating based on list titles
        String listTitle = getListTitle(jobOffer, listItems);
        if (listTitle == null) {
            return 0;

        } else {
            probability += rateTitles(jobOffer, listTitle);
        }

        if (Main.DEBUG && probability > 50) {
            System.out.println("[skills]\t" + probability + "\t" + listTitle);
        }

        return (int) probability;
    }

    private int calculateSkillListProbability(JobOffer jobOffer, String listTitle, List<String> listItems) {
        double probability = 100;
        int nofListItems = listItems.size();

        // adjust rating based on number of list items
        if (nofListItems == 0) {
            return 0;
        } else if (nofListItems == 1) {
            probability -= 25;
        } else if (nofListItems == 2) {
            probability -= 15;
        }

        initDependencyParser();

        for (String listItem : listItems) {
            // adjust rating based on number of special chars
            int nofSpecialChars = getNumberOfSpecialChars(listItem);
            if (nofSpecialChars > listItem.length() / 4d) {
                double subtract = 75d / nofListItems;
                subtract *= (double) nofSpecialChars / listItem.length();
                probability -= subtract;
            }
        }

        // adjust rating based on list titles
        probability += rateTitles(jobOffer, listTitle);

        if (Main.DEBUG && probability > 50) {
            System.out.println("[skills]\t" + probability + "\t" + listTitle);
        }

        return (int) probability;
    }

    private void initDependencyParser() {
        if (tagger == null) {
            Properties germanConfig = FileUtils.getStanfordCoreNLPGermanConfiguration();
            String taggerPath = germanConfig.getProperty("pos.model");
            String modelPath = germanConfig.getProperty("depparse.model");
            tagger = new MaxentTagger(taggerPath);
            parser = DependencyParser.loadFromModelFile(modelPath);
        }
    }

    private int rateTitles(JobOffer jobOffer, String title) {
        // check if title is in suspicious element
        Elements titleElements = jobOffer.getDocument().getElementsMatchingOwnText(title);
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

        // adjust rating by type of words
        DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(title));
        for (List<HasWord> sentence : tokenizer) {
            List<TaggedWord> tagged = tagger.tagSentence(sentence);
            GrammaticalStructure gs = parser.predict(tagged);
            for (TypedDependency dependency : gs.typedDependencies()) {
                rating += calculateTitleDependencyRating(dependency);
            }
        }

        return rating;
    }

    private int calculateTitleDependencyRating(TypedDependency dependency) {
        String shortName = dependency.reln().getShortName();
        String tag = dependency.dep().tag();
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
        } else if (EnglishGrammaticalRelations.NOMINAL_SUBJECT.getShortName().equals(shortName)
                && "VVFIN".equals(govTag)) {
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
        } else if ("NN".equals(tag)) {
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
            firstListText = jobOffer.getPlainTextFromHtml(listItem.html()).trim();
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
        Matcher specialCharMatcher = Pattern.compile("[^a-zA-Z .,:]").matcher(text);
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

    private boolean isSkillList(Element listElement) {
        boolean isSkillList = true;
        for (Element listItemElement : listElement.getElementsByTag("li")) {
            if (listItemElement.getElementsByTag("a").size() > 0) {
                isSkillList = false;
            } else if (listItemElement.getElementsByTag("input").size() > 0) {
                isSkillList = false;
            } else if (listItemElement.getElementsByTag("button").size() > 0) {
                isSkillList = false;
            }
        }
        return isSkillList;
    }
}
