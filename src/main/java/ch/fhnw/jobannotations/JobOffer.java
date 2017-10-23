package ch.fhnw.jobannotations;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author Hoang
 */
public class JobOffer {
    // job offer document
    private final Document document;
    private final Element bodyElement;
    private final String plainText;
    private final String bodyPlainText;

    // job attributes elements
    private Element titleElement;
    private Element workloadElement;
    private Element locationElement;

    // cleaned job offer attributes
    private String title;
    private String workload;
    private String location;


    public JobOffer(Document document) {
        this.document = document;
        this.bodyElement = this.document.getElementsByTag("body").first();

        // remove footer element
        Elements footers = new Elements();
        footers.addAll(bodyElement.getElementsByTag("footer"));
        footers.add(bodyElement.getElementById("footer"));
        footers.addAll(bodyElement.getElementsByClass("footer"));
        footers.remove(null);
        for (Element footer : footers) {
            footer.remove();
        }

        this.plainText = getPlainTextFromHtml(this.document.html());
        this.bodyPlainText = getPlainTextFromHtml(bodyElement.html());
    }

    public Document getDocument() {
        return document;
    }

    public Element getBodyElement() {
        return bodyElement;
    }

    public String getPlainText() {
        return plainText;
    }

    public String getBodyPlainText() {
        return bodyPlainText;
    }

    public Element getTitleElement() {
        return titleElement;
    }

    public void setTitleElement(Element titleElement) {
        this.titleElement = titleElement;
    }

    public Element getWorkloadElement() {
        return workloadElement;
    }

    public void setWorkloadElement(Element workloadElement) {
        this.workloadElement = workloadElement;
    }

    public Element getLocationElement() {
        return locationElement;
    }

    public void setLocationElement(Element locationElement) {
        this.locationElement = locationElement;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWorkload() {
        return workload;
    }

    public void setWorkload(String workload) {
        this.workload = workload;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPlainTextFromHtml(String text) {
        // replace b-tags with space to prevent line breaks
        text = text.replaceAll("(?i)\\s*\\n*\\s*</?b>\\s*", " ");

        // replace br-tags and line breaks with placeholder
        String breakTagPlaceholder = "%BREAK%";
        text = text.replaceAll("(?i)(<br[^>]*>|\\n)", breakTagPlaceholder);

        // clean html
        text = Jsoup.parse(text).text();

        // replace placeholder with real line breaks
        text = text.replaceAll(breakTagPlaceholder, "\n");

        return text;
    }
}
