package ch.fhnw.jobannotations.measure;

/**
 * Starter for precision and recall tests.
 *
 * @author Kevin Kirn <kevin.kirn@students.fhnw.ch>
 */
public class Main {

    public static void main(String args[]) {

        // initialize by test suite file
        Measure m = new Measure("C:\\Users\\kevin\\Desktop\\testsuite.txt");
        m.performJobTitleMeasurement();

    }

}
