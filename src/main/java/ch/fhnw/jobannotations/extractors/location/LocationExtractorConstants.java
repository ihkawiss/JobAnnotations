package ch.fhnw.jobannotations.extractors.location;

/**
 * This class holds constants that are used in {@link LocationExtractor}
 *
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
 */
class LocationExtractorConstants {

    static final String GEO_ADMIN_API_URL_TEMPLATE = "https://api3.geo.admin.ch/rest/services/api/SearchServer?type=locations&limit=1&searchText=%s";
    static final String[] ALLOWED_SPECIAL_LABELS_GEO_ADMIN_API = {"Ort", "Quartier", "Zug", "Bus"};
    static final int MAX_WORDS_IN_LOCATION_NAME = 10;
    static final String[] LOCATION_FLAGS = {
            "city", "town", "region", "location", "locality", "state", "address", "place", "map", "district",
            "province", "village", "canton", "adresse", "arbeitsort", "standort", "ort", "stadt", "gebiet", "viertel",
            "bezirk", "quartier", "gemeinde", "dorf", "kanton", "arbeitsregion"
    };

    static final String[] RELEVANT_LOCATION_HTML_ATTRIBUTES = {"class", "property", "id", "alt", "src"};
    static final String[] IRRELEVANT_TAGS = {"a", "input", "button"};

    static final int RATING_LOCATION_BY_LOCATION_FLAGS = 150;
    static final int RATING_LOCATION_BY_ZIP_CODE = 75;
    static final int RATING_LOCATION_BY_NER = 50;
    static final int RATING_ELEMENT_IN_FOOTER = -50;
    static final int RATING_REPETITION = 15;
    static final int RATING_VALIDATION_CONTAINS_ORIGINAL = 50;
    static final int RATING_VALIDATION_CONTAINS_ORIGINAL_LOWER_CASE = 25;
    static final int RATING_VALIDATION_FAILED = -25;
    static final int MAX_RATING_DICTIONARY = 200;
}
