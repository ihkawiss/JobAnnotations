package ch.fhnw.jobannotations;

import ch.fhnw.jobannotations.utils.HtmlUtils;
import ch.fhnw.jobannotations.utils.NlpHelper;
import edu.stanford.nlp.util.CoreMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hoang
 */
public class JobOffer {
    // job offer document
    private final Document document;
    private final Element headElement;
    private final Element bodyElement;
    private final Element bodyElementWithoutFooter;
    private final Element footerElement;

    // job attributes elements
    private Element titleElement;
    private Element workloadElement;
    private Element locationElement;

    // cleaned job offer attributes
    private String title;
    private String workload;
    private String location;
    private final List<CoreMap> annotatedSentences;
    private final List<CoreMap> annotatedBodySentences;
    private final List<CoreMap> annotatedFooterSentences;
    private String plaintText;
    private List<String> plainTextLines;


    public List<String> getPlainTextLines() {
        return plainTextLines;
    }

    public JobOffer(Document document) {
        this.document = document;

        headElement = this.document.head();

        bodyElement = this.document.body();
        bodyElementWithoutFooter = bodyElement.clone();

        // remove footer elements from body and add to list
        Elements footers = new Elements();
        extractToElementsList(footers, bodyElementWithoutFooter.getElementsByTag("footer"));
        extractToElementList(footers, bodyElementWithoutFooter.getElementById("footer"));
        extractToElementsList(footers, bodyElementWithoutFooter.getElementsByClass("footer"));

        // keep footer separately
        if (footers.size() == 1) {
            footerElement = footers.first();

        } else if (footers.size() == 0) {
            footerElement = null;

        } else {
            // merge multiple footer elements to a single element
            StringBuilder mergedFooterHtml = new StringBuilder("<div>");
            for (Element footer : footers) {
                mergedFooterHtml.append(footer.html());
            }
            mergedFooterHtml.append("</div>");
            footerElement = Jsoup.parse(mergedFooterHtml.toString()).body().child(0);
        }

        System.out.println("[general]\tAnnotating parsed job offer");
        annotatedSentences = new ArrayList<>();
        String bodyElementWithoutFooterPlainText = HtmlUtils.getPlainTextFromHtml(bodyElementWithoutFooter.html());
        String bodySentences = HtmlUtils.extractSentencesFromPlaintText(bodyElementWithoutFooterPlainText);
        annotatedBodySentences = NlpHelper.getInstance().getAnnotatedSentences(bodySentences);
        annotatedSentences.addAll(annotatedBodySentences);

        if (footerElement != null) {
            String footerElementWithoutFooterPlainText = HtmlUtils.getPlainTextFromHtml(footerElement.html());
            String footerSentences = HtmlUtils.extractSentencesFromPlaintText(footerElementWithoutFooterPlainText);
            annotatedFooterSentences = NlpHelper.getInstance().getAnnotatedSentences(footerSentences);
            annotatedSentences.addAll(annotatedFooterSentences);
        } else {
            annotatedFooterSentences = null;
        }

        plaintText = HtmlUtils.getPlainTextFromHtml(document.html());

        plainTextLines = new ArrayList<>();
        for (String line : plaintText.split("\n")) {
            line = line.trim();
            if (!line.isEmpty()) {
                plainTextLines.add(line);
            }
        }


    }

    private void extractToElementsList(Elements elements, Elements elementsToAdd) {
        for (Element element : elementsToAdd) {
            extractToElementList(elements, element);
        }

    }

    private void extractToElementList(Elements elements, Element elementToAdd) {
        if (elementToAdd != null && !elements.contains(elementToAdd)) {
            elements.add(elementToAdd);
            elementToAdd.remove();
        }
    }

    public List<CoreMap> getAnnotatedSentences() {
        return annotatedSentences;
    }

    public List<CoreMap> getAnnotatedBodySentences() {
        return annotatedBodySentences;
    }

    public List<CoreMap> getAnnotatedFooterSentences() {
        return annotatedFooterSentences;
    }

    public Document getDocument() {
        return document;
    }

    public Element getHeadElement() {
        return headElement;
    }

    public Element getBodyElement() {
        return bodyElement;
    }

    public Element getBodyElementWithoutFooter() {
        return bodyElementWithoutFooter;
    }

    public Element getFooterElement() {
        return footerElement;
    }

    public String getPlainText() {
        return plaintText;
    }

    public String getBodyPlainText() {
        return HtmlUtils.getPlainTextFromHtml(bodyElement.html());
    }

    public String getBodyWithoutFooterPlaintText() {
        return HtmlUtils.getPlainTextFromHtml(bodyElementWithoutFooter.html());
    }

    public String getFooterPlaintTExt() {
        if (footerElement == null) {
            return null;
        }
        return HtmlUtils.getPlainTextFromHtml(footerElement.html());
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

}
