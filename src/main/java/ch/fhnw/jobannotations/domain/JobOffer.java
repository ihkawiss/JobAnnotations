package ch.fhnw.jobannotations.domain;

import ch.fhnw.jobannotations.JobAnnotator;
import ch.fhnw.jobannotations.utils.HtmlUtils;
import ch.fhnw.jobannotations.utils.NlpHelper;
import ch.fhnw.jobannotations.utils.StringUtils;
import edu.stanford.nlp.util.CoreMap;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * This class acts as a wrapper for the job offer document by reading the job offer document and providing utility
 * methods to access information of the document.
 *
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
 */
public class JobOffer {

    private final static Logger LOG = Logger.getLogger(JobAnnotator.class);
    private static final String FOOTER_TAG_NAME = "footer";

    private final Document document;
    private final Element bodyElement;
    private final Element bodyElementWithoutFooter;
    private final Element footerElement;

    private String plaintText;
    private List<String> plainTextLines;
    private final List<CoreMap> annotatedBodySentences;
    private final List<CoreMap> annotatedFooterSentences;

    public JobOffer(Document document) {
        this.document = document;

        bodyElement = this.document.body();

        // keep footer separately and remove from body
        bodyElementWithoutFooter = bodyElement.clone();
        Elements footers = new Elements();
        extractToElementsList(footers, bodyElementWithoutFooter.getElementsByTag(FOOTER_TAG_NAME));
        extractToElementList(footers, bodyElementWithoutFooter.getElementById(FOOTER_TAG_NAME));
        extractToElementsList(footers, bodyElementWithoutFooter.getElementsByClass(FOOTER_TAG_NAME));
        footerElement = mergeFooterElements(footers);

        LOG.debug("Annotating parsed job offer");
        String bodyElementWithoutFooterPlainText = HtmlUtils.getPlainTextFromHtml(bodyElementWithoutFooter.html());
        String bodySentences = StringUtils.extractSentencesFromPlaintText(bodyElementWithoutFooterPlainText);
        annotatedBodySentences = NlpHelper.getInstance().getAnnotatedSentences(bodySentences);

        if (footerElement != null) {
            String footerElementWithoutFooterPlainText = HtmlUtils.getPlainTextFromHtml(footerElement.html());
            String footerSentences = StringUtils.extractSentencesFromPlaintText(footerElementWithoutFooterPlainText);
            annotatedFooterSentences = NlpHelper.getInstance().getAnnotatedSentences(footerSentences);
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

    /**
     * Merges given footer elements to one single footer element.
     *
     * @param footers Footer elements to be merged
     * @return Single merged footer element
     */
    private Element mergeFooterElements(Elements footers) {
        if (footers.size() == 1) {
            return footers.first();

        } else if (footers.size() == 0) {
            return null;

        } else {
            // merge multiple footer elements to a single element
            StringBuilder mergedFooterHtml = new StringBuilder("<div>");
            for (Element footer : footers) {
                mergedFooterHtml.append(footer.html());
            }
            mergedFooterHtml.append("</div>");
            return Jsoup.parse(mergedFooterHtml.toString()).body().child(0);
        }
    }

    /**
     * Removes given elements from body and adds them to the given elements object.
     *
     * @param elementList       Elements object to add removed elements to
     * @param elementsToExtract Elements to be removed and added to the elements object
     */
    private void extractToElementsList(Elements elementList, Elements elementsToExtract) {
        for (Element element : elementsToExtract) {
            extractToElementList(elementList, element);
        }

    }

    /**
     * Removes given element from body and adds it to the given elements object.
     *
     * @param elementList      Elements object to add removed element to
     * @param elementToExtract Element to be removed and added to the elements object
     */
    private void extractToElementList(Elements elementList, Element elementToExtract) {
        if (elementToExtract != null && !elementList.contains(elementToExtract)) {
            elementList.add(elementToExtract);
            elementToExtract.remove();
        }
    }

    /**
     * Returns the whole job offer document.
     *
     * @return Document
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Returns body element of the job offer document.
     *
     * @return Body element
     */
    public Element getBodyElement() {
        return bodyElement;
    }

    /**
     * Returns body element of the job offer document without footer element.
     *
     * @return Body element without footer element
     */
    public Element getBodyElementWithoutFooter() {
        return bodyElementWithoutFooter;
    }

    /**
     * Returns footer element of the job offer document.
     *
     * @return Footer element
     */
    public Element getFooterElement() {
        return footerElement;
    }

    /**
     * Returns the whole job offer document as plain text.
     *
     * @return Document plain text
     */
    public String getPlainText() {
        return plaintText;
    }

    /**
     * Returns whole job offer document as List with every line as List entry in plain text.
     *
     * @return List of plain text lines
     */
    public List<String> getPlainTextLines() {
        return plainTextLines;
    }

    /**
     * Returns annotated sentences of the body element.
     *
     * @return Annotated body sentences
     */
    public List<CoreMap> getAnnotatedBodySentences() {
        return annotatedBodySentences;
    }

    /**
     * Returns annotated sentences of the footer element.
     *
     * @return Annotated footer sentences
     */
    public List<CoreMap> getAnnotatedFooterSentences() {
        return annotatedFooterSentences;
    }
}
