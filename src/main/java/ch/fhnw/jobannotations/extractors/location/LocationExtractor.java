package ch.fhnw.jobannotations.extractors.location;

import ch.fhnw.jobannotations.extractors.IExtractor;
import ch.fhnw.jobannotations.domain.JobOffer;
import ch.fhnw.jobannotations.utils.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.util.CoreMap;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hoang
 */
public class LocationExtractor implements IExtractor {

    private static final String GEO_ADMIN_API_URL_TEMPLATE = "https://api3.geo.admin.ch/rest/services/api/SearchServer?type=locations&limit=1&searchText=%s";
    private static final String[] ALLOWED_SPECIAL_LABELS_GEO_ADMIN_API = {"Ort", "Quartier", "Zug", "Bus"};
    private static final int MAX_WORDS_IN_LOCATION_NAME = 10;
    private static final String[] LOCATION_FLAGS = {
            "city", "town", "region", "location", "locality", "state", "address", "place", "map", "district",
            "province", "village", "canton", "adresse", "arbeitsort", "standort", "ort", "stadt", "gebiet",
            "viertel", "bezirk", "quartier", "gemeinde", "dorf", "kanton", "arbeitsregion"
    };
    private static final String[] RELEVANT_LOCATION_HTML_ATTRIBUTES = {"class", "property", "id", "alt", "src"};
    private static final String[] IRRELEVANT_TAGS = {
            "a",
            "input",
            "button"
    };
    private static final String LOCATION_NER_TAG = "LOCATION";
    private static final int RATING_LOCATION_BY_LOCATION_FLAGS = 150;
    private static final int RATING_LOCATION_BY_ZIP_CODE = 75;
    private static final int RATING_LOCATION_BY_NER = 50;
    private static final int RATING_ELEMENT_IN_FOOTER = -50;
    private static final int RATING_REPETITION = 15;
    private static final int RATING_VALIDATION_CONTAINS_ORIGINAL = 50;
    private static final int RATING_VALIDATION_CONTAINS_ORIGINAL_LOWER_CASE = 25;
    private static final int RATING_VALIDATION_FAILED = -25;
    private List<IntStringPair> ratedJobLocations = new ArrayList<>();
    private MaxentTagger tagger;

    @Override
    public String parse(JobOffer jobOffer) {

        if (ConfigurationUtil.isDebugModeEnabled()) {
            System.out.println("\n" + StringUtils.repeat("-", 80));
            System.out.println("[location]\t" + "Started to parse location from offer");
        }

        findPotentialJobLocations(jobOffer);
        return getLocationWithHighestPotential();
    }

    @Override
    public void learn(String data) {
        // TODO: implement usage of file first!
    }

    private void findPotentialJobLocations(JobOffer jobOffer) {
        // TODO search in job title element

        // parse location in body without footer
        System.out.println("[location]\tFind potential job locations in body element without footer");
        findPotentialJobLocations(jobOffer, false);

        // parse location in footer
        System.out.println("[location]\tFind potential job locations in footer element");
        findPotentialJobLocations(jobOffer, true);
    }

    private void findPotentialJobLocations(JobOffer jobOffer, boolean isFooter) {
        int ratingAdjustment = isFooter ? RATING_ELEMENT_IN_FOOTER : 0;

        Element element = isFooter ? jobOffer.getFooterElement() : jobOffer.getBodyElementWithoutFooter();

        if (element == null) {
            return;
        }

        String plainText = HtmlUtils.getPlainTextFromHtml(element.html());

        for (String potentialJobLocation : getPotentialJobLocationByLocationFlags(element, plainText)) {
            IntStringPair ratedJobLocation = new IntStringPair(RATING_LOCATION_BY_LOCATION_FLAGS + ratingAdjustment, potentialJobLocation);
            ratedJobLocations.add(ratedJobLocation);
            System.out.println("[location-indicator]\tFound location by location flags: " + potentialJobLocation);
        }

        // use parsing with zip code regex
        for (String location : getPotentialJobLocationByZipCode(plainText)) {
            System.out.println("[location-indicator]\tFound location with ZIP code: " + location);
            IntStringPair ratedJobLocation = new IntStringPair(RATING_LOCATION_BY_ZIP_CODE + ratingAdjustment, location);
            ratedJobLocations.add(ratedJobLocation);
        }

        // find locations using NLP
        System.out.println("[location-ner]\tUsing Named Entity Recognition...");
        List<CoreMap> annotatedSentences = isFooter ? jobOffer.getAnnotatedFooterSentences() : jobOffer.getAnnotatedBodySentences();
        for (String location : getPotentialJobLocationByNamedEntityRecognition(annotatedSentences)) {
            System.out.println("[location-ner]\tFound location with NER: " + location);
            IntStringPair ratedJobLocation = new IntStringPair(RATING_LOCATION_BY_NER + ratingAdjustment, location);
            ratedJobLocations.add(ratedJobLocation);
        }

        System.out.println("[location]\tRemove entries that contain too many words");
        System.out.println("[location]\tRemove leading lower case words");

        int previousSize = ratedJobLocations.size();
        for (int i = previousSize - 1; i >= 0; i--) {
            String location = ratedJobLocations.get(i).getString();
            if (StringUtils.countMatches(location, " ") > MAX_WORDS_IN_LOCATION_NAME - 1) {
                ratedJobLocations.remove(i);

            } else {
                location = location.trim();
                boolean firstUpperCaseFound = false;
                StringBuilder modifiedLocation = new StringBuilder();
                for (int j = 0; j < location.length(); j++) {
                    char currentChar = location.charAt(j);
                    if (firstUpperCaseFound || Character.isUpperCase(currentChar)) {
                        firstUpperCaseFound = true;
                        modifiedLocation.append(currentChar);
                    }
                }
                location = modifiedLocation.toString().trim();
                if (!location.isEmpty()) {
                    ratedJobLocations.get(i).setString(location);

                } else {
                    // remove empty locations
                    ratedJobLocations.remove(i);
                }
            }
        }
    }

    private List<String> getPotentialJobLocationByLocationFlags(Element bodyElement, String plainText) {
        List<String> potentialJobLocations = new ArrayList<>();

        for (String locationFlag : LOCATION_FLAGS) {
            // get elements with relevant attributes values containing a location flag
            for (String attribute : RELEVANT_LOCATION_HTML_ATTRIBUTES) {
                Elements potentialElements = bodyElement.getElementsByAttributeValueContaining(attribute, locationFlag);
                addPotentialJobLocationToList(potentialJobLocations, potentialElements);
            }

            // by text
            Matcher locationMatcher = Pattern.compile("(?i)\\W" + locationFlag + "\\W[.\\s]*:?[.\\s]*(.*)\\n").matcher(plainText);
            while (locationMatcher.find()) {
                String match = locationMatcher.group(1);
                potentialJobLocations.add(match);

                // check for words in parentheses
                Matcher parenthesesMatcher = Pattern.compile("\\(.*\\)").matcher(match);
                while (parenthesesMatcher.find()) {
                    String cleaned = match.replace(parenthesesMatcher.group(), "");
                    potentialJobLocations.add(cleaned);
                }
            }
        }
        return potentialJobLocations;
    }

    private List<String> getPotentialJobLocationByNamedEntityRecognition(Iterable<? extends CoreMap> annotatedSentences) {
        List<String> potentialJobLocations = new ArrayList<>();

        for (CoreMap sentence : annotatedSentences) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String nerTag = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                if (NlpHelper.NER_TAG_LOCATION.equals(nerTag)) {
                    potentialJobLocations.add(word);
                }
            }
        }

/*
        String nerModel = FileUtils.getStanfordCoreNLPGermanConfiguration().getProperty("ner.model");
        CRFClassifier<CoreMap> classifier = CRFClassifier.getClassifierNoExceptions(nerModel);
        List<List<CoreMap>> sentences = classifier.classify(plainText);
        for (List<CoreMap> sentence : sentences) {
            for (CoreMap word : sentence) {
                String nerTag = word.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                if (LOCATION_NER_TAG.equals(nerTag)) {
                    String string = word.toString();
                    potentialJobLocations.add(string);
                }
            }

        }


        DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(plainText));
        for (List<HasWord> sentence : tokenizer) {
            List<TaggedWord> taggedWords = tagger.tagSentence(sentence);
            for (TaggedWord taggedWord : taggedWords) {
                String tag = taggedWord.tag();
                if (tag.equals(LOCATION_NER_TAG)) {
                    String word = taggedWord.word();
                    potentialJobLocations.add(word);
                }
            }
        }*/

        /*
        edu.stanford.nlp.simple.Document document = new edu.stanford.nlp.simple.Document(plainText);
        for (Sentence sentence : document.sentences()) {  // Will iterate over two sentences
            List<String> tags = sentence.nerTags();
            for (int i = 0; i < tags.size(); i++) {
                if (LOCATION_NER_TAG.equals(tags.get(i))) {
                    String location = sentence.word(i);
                    potentialJobLocations.add(location);
                }
            }
        }*/
        return potentialJobLocations;
    }

    private void initNer() {
        Properties germanConfig = FileUtils.getStanfordCoreNLPGermanConfiguration();
        String taggerPath = germanConfig.getProperty("pos.model");
        tagger = new MaxentTagger(taggerPath);
    }

    private List<String> getPotentialJobLocationByZipCode(String plainText) {
        List<String> potentialLocations = new ArrayList<>();
        Matcher zipCodeMatcher = Pattern.compile("\\d{4,5}\\s(.{2,})\\W").matcher(plainText);
        while (zipCodeMatcher.find()) {
            potentialLocations.add(zipCodeMatcher.group(1));
        }
        return potentialLocations;
    }

    private String getLocationWithHighestPotential() {
        if (ratedJobLocations.isEmpty()) {
            System.out.println("[location]\tNo locations found. Returning null.");
            return null;
        }

        validateLocations();
        removeDuplications();
        filterByRatings();

        // if there are still multiple locations left, just return location with shortest name
        if (ratedJobLocations.size() > 1) {
            ratedJobLocations.sort(Comparator.comparingInt(o -> o.getString().length()));
        }
        return ratedJobLocations.get(0).getString();
    }


    /**
     * Removes duplicated entries and adjust rating by number of duplications.
     */
    private void removeDuplications() {
        System.out.println("[location]\tRemove duplicates and adjust rating by number of identical entries");
        ratedJobLocations.sort(Comparator.comparing(IntStringPair::getString));
        String lastLocation = null;
        for (int i = ratedJobLocations.size() - 1; i >= 0; i--) {
            IntStringPair ratedJobLocation = ratedJobLocations.get(i);
            if (ratedJobLocation.getString().equals(lastLocation)) {
                int ratingDifference = ratedJobLocations.get(i + 1).getInt() - ratedJobLocation.getInt();
                if (ratingDifference < 0) {
                    ratingDifference = 0;
                }
                int additionalRating = RATING_REPETITION + ratingDifference;
                ratedJobLocations.remove(i + 1);
                ratedJobLocation.setInt(ratedJobLocation.getInt() + additionalRating);

            }
            lastLocation = ratedJobLocation.getString();
        }
    }

    private void validateLocations() {
        System.out.println("[location]\tValidate location names");
        boolean addressValidated = false;
        for (IntStringPair ratedLocation : ratedJobLocations) {
            String location = ratedLocation.getString();
            String validatedAddress = getValidatedAddressFromGeoApi(location);
            if (validatedAddress != null) {
                System.out.println("[location]\tLocation validated: [" + location + "] => [" + validatedAddress + "]");
                addressValidated = true;
                int validationRating = calculateValidationRating(location, validatedAddress);
                ratedLocation.setString(validatedAddress);
                ratedLocation.setInt(ratedLocation.getInt() + validationRating);

            } else {
                System.out.println("[location]\tFailed to validate location: [" + location + "]");
                ratedLocation.setInt(ratedLocation.getInt() + RATING_VALIDATION_FAILED);
            }
        }

        // if failed to validate address, try again with separate words of each location string
        if (!addressValidated) {
            System.out.println("[location]\tNo locations could be validated. Validating separate words in location names now...");
            int previousListSize = ratedJobLocations.size();
            for (int i = 0; i < previousListSize; i++) {
                IntStringPair ratedLocation = ratedJobLocations.get(i);
                String location = ratedLocation.getString();
                Matcher wordMatcher = Pattern.compile("\\w+").matcher(location);
                while (wordMatcher.find()) {
                    String partialLocationName = wordMatcher.group();
                    if (partialLocationName.length() > 1
                            && Character.isUpperCase(partialLocationName.charAt(0))) {
                        String validatedAddress = getValidatedAddressFromGeoApi(partialLocationName);
                        if (validatedAddress != null) {
                            System.out.println("[location]\tLocation validated: [" + location + "] => [" + partialLocationName + "] => [" + validatedAddress + "]");
                            int partialLocationRating = ratedLocation.getInt() - 25;
                            partialLocationRating += calculateValidationRating(partialLocationName, validatedAddress);
                            IntStringPair ratedPartialLocation = new IntStringPair(partialLocationRating, validatedAddress);
                            ratedJobLocations.add(ratedPartialLocation);
                        }
                    }
                }
            }
        }
    }

    /**
     * Only keep entries with highest ratings
     */
    private void filterByRatings() {
        // sort by rating
        ratedJobLocations.sort((o1, o2) -> o2.getInt() - o1.getInt());

        // remove entries with lower ratings
        int highestRating = ratedJobLocations.get(0).getInt();
        for (int i = ratedJobLocations.size() - 1; i > 0; i--) {
            if (ratedJobLocations.get(i).getInt() < highestRating) {
                ratedJobLocations.remove(i);
            }
        }
    }

    private int calculateValidationRating(String originalLocationName, String validatedLocationName) {
        // remove text in parentheses for better rating calculation
        validatedLocationName = validatedLocationName.replaceAll("\\s*\\(.*\\)\\s*", "");

        int rating = FuzzySearch.ratio(originalLocationName, validatedLocationName);
        rating -= 50;
        rating *= 2;

        if (validatedLocationName.contains(originalLocationName)) {
            rating += RATING_VALIDATION_CONTAINS_ORIGINAL;

        } else if (validatedLocationName.toLowerCase().contains(originalLocationName.toLowerCase())) {
            rating += RATING_VALIDATION_CONTAINS_ORIGINAL_LOWER_CASE;
        }

        return rating;
    }


    private String getValidatedAddressFromGeoApi(String location) {
        JSONArray jsonArray = doApiRestCall(GEO_ADMIN_API_URL_TEMPLATE, location);
        if (jsonArray != null && jsonArray.length() > 0) {
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            JSONObject attrs = jsonObject.getJSONObject("attrs");
            String validatedAddress = (attrs).get("label").toString();

            if (validatedAddress.contains("<i>")) {
                boolean allowedSpecialLabel = false;
                for (String specialLabel : ALLOWED_SPECIAL_LABELS_GEO_ADMIN_API) {
                    String specialLabelTag = "<i>" + specialLabel + "</i>";
                    if (validatedAddress.contains(specialLabelTag)) {
                        allowedSpecialLabel = true;
                        validatedAddress = validatedAddress.replace(specialLabelTag, "");
                    }
                }
                if (!allowedSpecialLabel) {
                    // special label that is not allowed
                    // could be a store name. return null
                    return null;
                }
            }
            validatedAddress = Jsoup.clean(validatedAddress, Whitelist.none());
            return validatedAddress;
        }
        return null;
    }

    private JSONArray doApiRestCall(String urlTemplate, String value) {
        HttpURLConnection conn = null;
        try {
            String urlEncoded = URLEncoder.encode(value, Charset.defaultCharset().displayName());
            String restURl = String.format(urlTemplate, urlEncoded);
            URL url = new URL(restURl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                System.err.println("[location]\tFailed to validate location: [" + value + "] HTTP error code: " + conn.getResponseCode());
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder json = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                json.append(output);
            }

            conn.disconnect();

            JSONObject jsonObject = new JSONObject(json.toString());

            JSONArray results = (JSONArray) jsonObject.get("results");

            if (results.length() > 0) {
                return results;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;
    }

    private void addPotentialJobLocationToList(List<String> textToCheck, Elements potentialElements) {
        for (Element element : potentialElements) {
            String tagName = element.tagName();
            if (Arrays.asList(IRRELEVANT_TAGS).contains(tagName)) {
                return;
            }
            String text = element.text();
            for (String textParts : text.split(":")) {
                textParts = textParts.trim();
                if (!textParts.isEmpty()) {
                    textToCheck.add(textParts);
                }
            }
            text = element.parent().text();
            for (String textParts : text.split(":")) {
                textParts = textParts.trim();
                if (!textParts.isEmpty()) {
                    textToCheck.add(textParts);
                }
            }
        }
    }

    private boolean elementContainsText(Element element, String text) {
        return element.text().contains(text);
    }

    private boolean elementIsInsideOtherElement(Element parent, Element child) {
        for (Element element : parent.children()) {
            if (element.equals(child)) {
                return true;
            } else if (elementIsInsideOtherElement(element, child)) {
                return true;
            }
        }
        return false;
    }
}
