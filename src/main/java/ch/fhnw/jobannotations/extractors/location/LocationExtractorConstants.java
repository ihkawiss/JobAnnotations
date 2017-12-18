package ch.fhnw.jobannotations.extractors.location;

/**
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
 */
public class LocationExtractorConstants {

    public static final String GEO_ADMIN_API_URL_TEMPLATE = "https://api3.geo.admin.ch/rest/services/api/SearchServer?type=locations&limit=1&searchText=%s";
    public static final String[] ALLOWED_SPECIAL_LABELS_GEO_ADMIN_API = {"Ort", "Quartier", "Zug", "Bus"};
    public static final int MAX_WORDS_IN_LOCATION_NAME = 10;
    public static final String[] LOCATION_FLAGS = {
            "city", "town", "region", "location", "locality", "state", "address", "place", "map", "district",
            "province", "village", "canton", "adresse", "arbeitsort", "standort", "ort", "stadt", "gebiet",
            "viertel", "bezirk", "quartier", "gemeinde", "dorf", "kanton", "arbeitsregion"
    };
    public static final String[] RELEVANT_LOCATION_HTML_ATTRIBUTES = {"class", "property", "id", "alt", "src"};
    public static final String[] IRRELEVANT_TAGS = {
            "a",
            "input",
            "button"
    };
    public static final int RATING_LOCATION_BY_LOCATION_FLAGS = 150;
    public static final int RATING_LOCATION_BY_ZIP_CODE = 75;
    public static final int RATING_LOCATION_BY_NER = 50;
    public static final int RATING_ELEMENT_IN_FOOTER = -50;
    public static final int RATING_REPETITION = 15;
    public static final int RATING_VALIDATION_CONTAINS_ORIGINAL = 50;
    public static final int RATING_VALIDATION_CONTAINS_ORIGINAL_LOWER_CASE = 25;
    public static final int RATING_VALIDATION_FAILED = -25;
    public static final int MAX_RATING_DICTIONARY = 200;
}
