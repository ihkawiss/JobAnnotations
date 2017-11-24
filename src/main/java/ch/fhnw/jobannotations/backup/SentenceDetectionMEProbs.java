package ch.fhnw.jobannotations.backup;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import java.io.FileInputStream;
import java.io.InputStream;

public class SentenceDetectionMEProbs {

    public static void main(String args[]) throws Exception {

        String sentence = "Kanton Zürich\n" +
                "Finanzdirektion\n" +
                "Kantonales Steueramt\n" +
                "Teamleiter/-in 100%\n" +
                "Rund 750 Personen arbeiten im Steueramt des Kantons Zürich. Gemäss dem gesetzlichen Auftrag leitet das Steueramt den Vollzug der Steuergesetze und sorgt für richtige und gleichmässige Steuerveranlagungen und für einen einheitlichen Steuerbezug. Das Steueramt des Kantons Zürich veranlagt und bezieht alle Steuern, die nicht einer anderen Behörde zugewiesen sind.\n" +
                "Stellenbeschreibung\n" +
                "Für die Dienstabteilung Akten- und Datenpflege suchen wir per sofort oder nach Vereinbarung eine/n Teamleiter/-in. In dieser Funktion nehmen Sie die personelle und fachliche Führung des Teams mit 6-8 Mitarbeitenden wahr. Innerhalb der Dienstabteilung stellen wir den reibungslosen Ablauf der zu verarbeitenden Steuererklärungen/Steuer-Verfahren sicher. Zudem sind Sie mit der Ausbildung von neuen Mitarbeitenden und der Einsatzplanung betraut.";

        //Loading sentence detector model
        InputStream inputStream = new FileInputStream(PosTaggerExample.class.getClassLoader().getResource("de-sent.bin").getFile());
        SentenceModel model = new SentenceModel(inputStream);

        //Instantiating the SentenceDetectorME class
        SentenceDetectorME detector = new SentenceDetectorME(model);

        //Detecting the sentence
        String sentences[] = detector.sentDetect(sentence);

        //Printing the sentences
        for(String sent : sentences) {
            System.out.println(sent);
        }
    }
}
