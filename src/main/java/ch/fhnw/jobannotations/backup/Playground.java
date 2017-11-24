package ch.fhnw.jobannotations.backup;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hoang
 */
public class Playground {

    public static void main(String[] args) {
        boolean skip = true;

        Document parse = Jsoup.parse("<span>Dad &amp; Me!</span>");

        if (skip) {
            return;
        }

        int[] a = {4, 12};
        int[] b = {0, 12};
        int[] c = {0, 3};

        List<int[]> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        list.add(c);
        list.add(new int[]{1, 13});
        list.add(new int[]{5, 13});
        list.add(new int[]{5, 14});

        List<int[]> filteredList = new ArrayList<>(list);

        List<String> strings = new ArrayList<>();
        strings.add("asdfasdf");
        strings.add("asdf");
        strings.add("wer");
        strings.add("David isch ä schnäbi");
        strings.add("David");
        strings.add("qwerqwer");
        strings.add("erq");

        namedLoop: for (int i = strings.size() - 1; i > -1; i--) {
            String currentString = strings.get(i);
            for (int j = i - 1; j > -1; j++) {
                if (strings.get(j).contains(currentString)) {
                    strings.remove(i);
                    continue namedLoop;
                }
            }
        }

        for (String string : strings) {
            System.out.println(string);
        }


        if (skip) {
            return;
        }
        String test = "VZ Depotbank AG\n" +
                "Consultant clients privés bilingue D/F (H/F, 80% - 100%), Zug\n" +
                "Insurance Brokerage\n" +
                "Kundenbetreuer/-in Sach- und Haftpflichtversicherung, Zürich\n" +
                "ME\n" +
                "Junior Finanzplaner/-in, Meilen\n" +
                "Financial Consulting / Banking\n" +
                "Privatkundenberater/-in (80% - 100%), Meilen\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg ins Financial Consulting / Wealth Management, St. Gallen\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg als Relationship Manager (w/m) vermögende Kunden, Solothurn\n" +
                "HypothekenZentrum AG\n" +
                "Immobilienbewerter/-in, Zürich\n" +
                "Finanzbuchhaltung\n" +
                "Mitarbeiter/-in im Controlling (60%), Zürich\n" +
                "Bank i.G. (Deutschland)\n" +
                "Leiter/-in Rechnungswesen und Controlling, München\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg ins Financial Consulting, Team LGBT (w/m), Zürich\n" +
                "Contact Center\n" +
                "Mitarbeiter/-in Contact Center mit akzentfreiem Hochdeutsch, Zürich\n" +
                "Investment Advisory\n" +
                "Associate Investment Advisor, Zug\n" +
                "VZ Depotbank AG\n" +
                "Junior Relationship Manager (w/m, 80% - 100 %), Zürich\n" +
                "HypothekenZentrum AG\n" +
                "Kundenbetreuer/-in Hypotheken (80% - 100%), Zürich\n" +
                "HypothekenZentrum AG\n" +
                "(Senior) Hypothekarspezialist (w/m), Zürich\n" +
                "Insurance Brokerage\n" +
                "Insurance Broker Personenversicherungen und BVG, Zürich\n" +
                "Financial Consulting / Banking\n" +
                "Junior Finanzplaner/-in mit Entwicklungsperspektive, Basel/Liestal\n" +
                "VZ Depotbank AG\n" +
                "Sachbearbeiter/-in Kundenstammdaten, Zürich\n" +
                "IT-Entwicklung\n" +
                "(Senior) Software Engineer CRM (w/m, 80% - 100%), Zürich\n" +
                "Financial Consulting / Banking\n" +
                "Junior Finanzplaner/-in, St. Gallen\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg ins Financial Consulting (80% - 100%), Fribourg\n" +
                "Financial Consulting / Banking\n" +
                "Junior Finanzplaner/-in mit Entwicklungsperspektive (80% - 100%), Fribourg\n" +
                "Financial Consulting / Banking\n" +
                "Kundenberater/-in Private Clients, Uster\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg ins Financial Consulting, Winterthur\n" +
                "Financial Consulting / Banking\n" +
                "Junior Finanzplaner/-in (Aufbau Wallis), Thun\n" +
                "Contact Center\n" +
                "Mitarbeiter/-in Contact Center (40%, flexibel gestaltbar), Zürich\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg ins Financial Consulting/Wealth Management (90% - 100%), Solothurn\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg ins Financial Consulting / Wealth Management (ab 70%), Chur\n" +
                "Financial Consulting / Banking\n" +
                "Finanzplaner/-in, Chur\n" +
                "Financial Consulting / Banking\n" +
                "Chance für Heimwehbündner: Finanzplaner/-in, Chur\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg ins Financial Consulting/Wealth Management (90% - 100%), Aarau\n" +
                "IT-Entwicklung\n" +
                "(Senior) Software Engineer (w/m, 80% - 100%), Zürich\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg Financial Consulting für den Aufbau der Region Wallis, Thun\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg ins Financial Consulting / Wealth Management (ab 70%), Chur\n" +
                "IT-Entwicklung\n" +
                "Junior Software Engineer C# (w/m, 60% - 100%), Zürich\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg als Kundenberater/-in Private Clients, Basel/Liestal\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg ins Financial Consulting / Wealth Management, Bern\n" +
                "HypothekenZentrum AG\n" +
                "Consultant Advanced Real Estate Financing (w/m), Zürich\n" +
                "Financial Consulting / Banking\n" +
                "Junior Finanzplaner/-in mit Entwicklungsperspektive, Zürich\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg Financial Consulting (ab 80% auch während des Studiums), Horgen\n" +
                "Financial Consulting / Banking\n" +
                "Junior Finanzplaner/-in mit Entwicklungsperspektive, Bern\n" +
                "Financial Consulting / Banking\n" +
                "Kundenberater/-in Private Clients, Horgen\n" +
                "Financial Consulting / Banking\n" +
                "Junior Finanzplaner/-in mit Entwicklungsperspektive, Basel/Liestal\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg ins Financial Consulting / Wealth Management (ab 80%), Uster\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg Financial Consulting 80% (während des Studiums), Zürich\n" +
                "Financial Consulting / Banking\n" +
                "Karriere im Financial Consulting (FH oder HF), Basel/Liestal\n" +
                "Financial Consulting / Banking\n" +
                "Kundenberater/-in Private Clients, Zürich\n" +
                "Financial Consulting / Banking\n" +
                "Wealth Manager (w/m), Zürich\n" +
                "Financial Consulting / Banking\n" +
                "Einstieg Financial Consulting, Zürich";

        String[] lines = test.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i % 2 == 1) {
                String description = lines[i];
                //System.out.println(description);

                String[] tmp = description.split(",");

                String trainText;
                /* CITY
                String city = tmp[tmp.length - 1].trim();
                //System.out.println(city);
                trainCityText = "<START:city> " + city + " <END>";
                String trainText = description.replace(city, trainCityText);*/

                String jobTitle = tmp[0].split("\\(w")[0].split("\\(\\d")[0].trim();

                // jobtitle
                String trainJobTitleText = "<START:jobtitle> " + jobTitle + " <END>";
                trainText = description.replace(jobTitle, trainJobTitleText);

                System.out.println(trainText);
            }
        }

    }
}
