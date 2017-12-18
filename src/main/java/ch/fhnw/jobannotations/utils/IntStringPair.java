package ch.fhnw.jobannotations.utils;

/**
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
 */
public class IntStringPair {
    private static final String TO_STRING_FORMAT = "[rating=%d, string=%s]";
    private int intValue;
    private String string;

    public IntStringPair(int intValue, String string) {
        this.intValue = intValue;
        this.string = string;
    }

    public int getInt() {
        return intValue;
    }

    public void setInt(int intValue) {
        this.intValue = intValue;
    }

    public void addInt(int intValue) {
        this.intValue += intValue;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    @Override
    public String toString() {
        return String.format(TO_STRING_FORMAT, intValue, string);
    }
}
