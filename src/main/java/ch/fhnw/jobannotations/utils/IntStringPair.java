package ch.fhnw.jobannotations.utils;

import org.jsoup.nodes.Element;

/**
 * @author Hoang
 */
public class IntStringPair {
    private static final String TO_STRING_FORMAT = "[rating=%d, string=%s]";
    private int intValue;
    private String string;
    private Element element;

    public IntStringPair(int intValue, String string) {
        this.intValue = intValue;
        this.string = string;
    }

    public IntStringPair(int intValue, String string, Element element) {
        this.intValue = intValue;
        this.string = string;
        this.element = element;
    }

    public int getInt() {
        return intValue;
    }

    public void setInt(int intValue) {
        this.intValue = intValue;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    @Override
    public String toString() {
        return String.format(TO_STRING_FORMAT, intValue, string);
    }
}
