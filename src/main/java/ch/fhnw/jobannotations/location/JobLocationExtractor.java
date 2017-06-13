package ch.fhnw.jobannotations.location;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Hoang
 */
public class JobLocationExtractor {

    private static final String PLACE_API_URL_TEMPLATE = "https://maps.googleapis.com/maps/api/place/textsearch/json?key=AIzaSyDkQbCtAN-A2yndD75PErvsKt5qryNL55A&type=locality&query=%s";
    private static final String MEDIAWIKI_API_DE_URL_TEMPLATE = "https://de.wikipedia.org/w/api.php?action=query&prop=revisions&rvprop=content&format=jsonfm&rvsection=0&titles=%s";

    private static final String[] LOCATION_FLAGS = {
            "city",
            "town",
            "region",
            "location",
            "locality",
            "state",
            "address",
            "place",
            "map",
            "district",
            "province",
            "village",
            "canton",
            "adresse",
            "ort",
            "stadt",
            "gebiet",
            "viertel",
            "bezirk",
            "quartier",
            "gemeinde",
            "dorf",
            "kanton"
    };

    public String parseJobLocation(Document document) {
        List<String> potentialJobLocations = new ArrayList<>();
        Element bodyElement = document.clone().getElementsByTag("body").first();

        // get potential job locations by tag attributes
        for (String locationFlag : LOCATION_FLAGS) {
            Elements potentialElements = bodyElement.getElementsByAttributeValueContaining("class", locationFlag);
            addPotentialJobLocationToList(potentialJobLocations, potentialElements);

            potentialElements = document.getElementsByAttributeValueContaining("property", locationFlag);
            addPotentialJobLocationToList(potentialJobLocations, potentialElements);

            potentialElements = document.getElementsByAttributeValueContaining("id", locationFlag);
            addPotentialJobLocationToList(potentialJobLocations, potentialElements);
        }

        potentialJobLocations.sort(Comparator.comparingInt(String::length));

        boolean useGooglePlaceApi = false;
        boolean useMediaWikiApi = true;
        for (String potentialJobLocation : potentialJobLocations) {
            System.out.println(potentialJobLocation);
            if (useGooglePlaceApi) {
                String formattedAddress = getFormattedAddress(potentialJobLocation);
                if (formattedAddress != null) {
                    return formattedAddress;
                }
            }

            if (useMediaWikiApi) {
                boolean hasPostcodeOnWikipedia = hasPostcodeOnWikipedia(potentialJobLocation);

            }
        }

        System.out.println("______________________________________");

        // get potential job locations by plain texts
        potentialJobLocations.clear();
        String html = bodyElement.html();
        String[] htmlLines = html.split("\\n");
        boolean checkNextLine = false;
        for (String htmlLine : htmlLines) {
            String text = Jsoup.parse(htmlLine).text();
            if (checkNextLine) {
                checkNextLine = false;
                for (String textParts : text.split(":")) {
                    textParts = textParts.trim();
                    if (!textParts.isEmpty()) {
                        potentialJobLocations.add(textParts);
                    }
                }

            } else {
                for (String locationFlag : LOCATION_FLAGS) {
                    if (text.contains(locationFlag)) {
                        checkNextLine = true;
                        for (String textParts : text.split(":")) {
                            textParts = textParts.trim();
                            if (!textParts.isEmpty()) {
                                potentialJobLocations.add(textParts);
                            }
                        }
                    }
                }
            }
        }

        potentialJobLocations.sort(Comparator.comparingInt(String::length));

        for (String potentialJobLocation : potentialJobLocations) {
            String formattedAddress = getFormattedAddress(potentialJobLocation);
            if (formattedAddress != null) {
                return formattedAddress;
            }
        }


        return "";
    }

    private boolean hasPostcodeOnWikipedia(String potentialLocation) {
        return false;
    }

    private String getFormattedAddress(String potentialLocation) {
        JSONArray jsonArray = doGooglePlaceApiRestCall(potentialLocation);
        if (jsonArray != null) {
            for (Object object : jsonArray) {
                JSONObject jsonObject = (JSONObject) object;
                String name = jsonObject.get("name").toString();

                int ratio = FuzzySearch.ratio(potentialLocation, name);

                if (ratio > 90) {
                    String formattedAddress = jsonObject.get("formatted_address").toString();
                    return formattedAddress;
                }
            }
        }
        return null;
    }

    private void addPotentialJobLocationToList(List<String> textToCheck, Elements potentialElements) {
        for (Element element : potentialElements) {
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

    private JSONArray doGooglePlaceApiRestCall(String text) {
        try {
            String urlEncoded = URLEncoder.encode(text, Charset.defaultCharset().displayName());
            String restURl = String.format(PLACE_API_URL_TEMPLATE, urlEncoded);
            URL url = new URL(restURl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String json = "";
            String output;

            while ((output = br.readLine()) != null) {
                json += output;
            }

            conn.disconnect();

            JSONObject jsonObject = new JSONObject(json);

            JSONArray results = (JSONArray) jsonObject.get("results");

            if (results.length() > 0) {
                return results;
            }

        } catch (IOException e) {

            e.printStackTrace();

        }

        return null;
    }

}
