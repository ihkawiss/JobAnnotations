package ch.fhnw.jobannotations.backup;

import ch.fhnw.jobannotations.utils.FileUtils;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.parser.nndep.demo.DependencyParserDemo;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.util.logging.Redwood;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author Hoang
 */
public class Main {

    static String sentenceEn = "Jack London is the author of what novel?";
    static String sentence2 = "Bümpliz, heute ist nähe Zürich eine Bombe explodiert.";
    static String sentence1 = "Wir suche einen kreativen und sehr erfahrenen RF Engineer, um unser Team in Zürich zu erweitern.";
    static String sentence = "Tutorialspoint ist stationiert in Bümpliz";
    static String sentence3 = "Wir setzen Impulse. Weil Leben kostbar ist. BIOTRONIK ist einer der weltweit führenden Hersteller kardio- und endovaskulärer Medizintechnik. Als global tätiges Unternehmen mit Hauptsitz in Berlin bieten wir Spitzenlösungen auf dem neuesten Stand von Forschung und Technologie. Es lohnt sich, als Impulsgeber voranzugehen. Engagierte Mitarbeiter rund um den Globus tun dies bereits.\n" +
            " \n" +
            "Wir suchen Sie:\n" +
            " \n" +
            "    \n" +
            " Quality Engineer Coating & Assembly (w/m) 80 - 100% \n" +
            " \n" +
            " \n" +
            "Die Abteilung Quality Engineering unterstützt den Produkt Life Cycle von der Produktentstehung bis zur Erreichung fähiger Serienprozesse, welche nachhaltige, zuverlässige, konforme und qualitätsgerechteProdukte liefern. Mit Hilfe von präventiven Werkzeugen und reaktiven Massnahmen treiben wir den kontinuierlichen Verbesserungsprozess voran und stellen die Produktqualität unter Berücksichtigung von Wirtschaftlichkeit und Effizienz sicher. \n" +
            " \n" +
            " \n" +
            "Ihre Aufgaben\n" +
            " \tIhr Profil\n" +
            " \n" +
            "Definieren, Entwickeln und Umsetzen der Abteilungsstrategie\n" +
            "Ausbau und Etablierung von Methoden zur Erhaltung bzw. Steigerung der Zuverlässigkeit, Verfügbarkeit und Sicherheit von Produkten und Prozessen\n" +
            "Verantwortung bei Non-Conformity Management, Prozessvalidierung, Prüfplanung und Prozess-FMEA\n" +
            "Gewährleistung und Unterstützung der Bereiche Produktion und R&D, insbesondere bei Requirement Engineering, Fehleranalysen, Korrektur-/Verbesserungs-massnahmen und Change Management\n" +
            "Abgeschlossene Technische Fachhochschule (z.B. Verfahrenstechnik) oder Technische Universität (z.B. Chemie) und mindestens 2 Jahre Berufserfahrung im Qualitätsbereich (cGMP Umfeld)\n" +
            "cGxP Know-How und Kenntnisse von ISO 9001, ISO 13485, 21 CFR 820, MDD 93/42/EEC\n" +
            "Fundierte Kenntnisse in Chemie, Pharmazie (Fokus: Wirkstoffe und Analysen)\n" +
            "Expertise in Datenanalyse, Statistik wünschenswert (Six Sigma von Vorteil)\n" +
            "Erfahrung im Projektmanagement von Vorteil\n" +
            "Verhandlungssicher in Deutsch und Englisch (Wort und Schrift)\n" +
            " \n" +
            "Haben wir Ihr Interesse geweckt? Dann bewerben Sie sich bitte online! Wir freuen uns auf Sie.\n" +
            "    \n" +
            " \t\n" +
            " \n" +
            "Standort: CH-Bülach \n" +
            "Arbeitszeit: Vollzeit                      \n" +
            "Vertragsart: Unbefristet\n" +
            "Kennziffer: 12644\n" +
            "Ansprechpartner: Florian Grützmann                     \n" +
            "Tel.: +41 44 864 5860\n" +
            " \n" +
            " \n" +
            "Es werden nur Direktbewerbungen akzeptiert.\n" +
            " \n" +
            " ";

    static String sentence4 = "Ihre Fähigkeiten";

    /** A logger for this class */
    private static Redwood.RedwoodChannels log = Redwood.channels(DependencyParserDemo.class);


    public static void main(String[] args) {

//        nerOpenNlp();
        //nerCoreNlp();

        //posOpenNlp();
        dependencyParser();
    }

    public static void dependencyParser() {
        Properties germanConfig = FileUtils.getStanfordCoreNLPGermanConfiguration();

        String taggerPath = germanConfig.getProperty("pos.model");
        String modelPath = germanConfig.getProperty("parser.model");

        String text = "Was wir Ihnen anbieten";

        MaxentTagger tagger = new MaxentTagger(taggerPath);
        DependencyParser parser = DependencyParser.loadFromModelFile(modelPath);

        DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
        for (List<HasWord> sentence : tokenizer) {
            List<TaggedWord> tagged = tagger.tagSentence(sentence);
            GrammaticalStructure gs = parser.predict(tagged);

            // TODO: gs.typedDependencies().iterator().next().reln().getShortName().equals(EnglishGrammaticalRelations.DIRECT_OBJECT.shortName)
            // Print typed dependencies
            System.out.println(gs);
        }
    }


    private static void posOpenNlp() {

        try (InputStream modelIn = FileUtils.getFileAsInputStream("de-pos-maxent.bin")) {

            POSModel posModel = new POSModel(modelIn);

            POSTaggerME tagger = new POSTaggerME(posModel);

            sentence4 = sentence4.replaceAll("\\n", " ");
            sentence4 = sentence4.replaceAll("\\s{2,}", " ");

            String[] words = sentence4.split(" ");
            String[] tags = tagger.tag(words);

            System.out.println();
            for (int i = 0; i < tags.length; i++) {
                String tag = tags[i];
                String word = words[i];
                if (tag.equals("PPOSAT")
                        || tag.equals("PPER")
                        || tag.equals("VVFIN")
                        ) {
                    System.out.println("\n" + tag + ":\t " + word + "\n");

                } else if (word.equalsIgnoreCase("bieten")
                        || word.equalsIgnoreCase("begeistern")
                        || word.equalsIgnoreCase("wir")
                        || word.equalsIgnoreCase("sie")
                        || word.equalsIgnoreCase("ihnen")
                ) {
                    System.out.println("\n" + tag + ":\t " + word + "\n");

                } else {
                    System.out.println("\n" + tag + ":\t " + word + "\n");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void nerCoreNlp() {
        List<String> locations = new ArrayList<>();
        Document doc = new Document(sentence3);
        for (Sentence sent : doc.sentences()) {  // Will iterate over two sentences
            List<String> tags = sent.nerTags();
            for (int i = 0; i < tags.size(); i++) {
                System.out.println(tags.get(i) + ":\t " + sent.word(i));
                if (tags.get(i).equals("LOCATION")) {
                    String location = sent.word(i);
                    locations.add(location);
                }
            }
        }

        System.out.println();

        for (String location : locations) {
            System.out.println(location);
        }
        System.out.println("end");

    }

    private static void nerOpenNlp() {
        InputStream modelInToken = null;
        InputStream modelIn = null;

        try {

            ClassLoader classLoader = Main.class.getClassLoader();

            //1. convert sentence into tokens
            modelInToken = new FileInputStream(classLoader.getResource("de-token.bin").getFile());
            TokenizerModel modelToken = new TokenizerModel(modelInToken);
            Tokenizer tokenizer = new TokenizerME(modelToken);
            String tokens[] = tokenizer.tokenize(sentence3);

            //2. find names
            modelIn = new FileInputStream(classLoader.getResource("en-ner-location.bin").getFile());
            //modelIn = new FileInputStream(classLoader.getResource("de-ner-jobtitle.bin").getFile());
            TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
            NameFinderME nameFinder = new NameFinderME(model);


            Span nameSpans[] = nameFinder.find(tokens);

            for (Span nameSpan : nameSpans) {
                System.out.println("FOUND: " + nameSpan);
                System.out.println(nameSpan.toString() + "  " + tokens[nameSpan.getStart()]);
            }


            //find probabilities for names
            double[] spanProbs = nameFinder.probs(nameSpans);


            String[] entities = Span.spansToStrings(nameSpans, tokens);
            List<String> foundEntities = new ArrayList<String>(Arrays.asList(entities));

            //3. print names
            for (int i = 0; i < nameSpans.length; i++) {
                String token = tokens[nameSpans[i].getStart()].trim();

                int nonAlphabeticCharacterCount = getNonAlphabeticCharacterCount(token);
                boolean mostlyNonAlphabeticCharacters = nonAlphabeticCharacterCount > (token.length() / 2);
                if (token.length() > 1 && !mostlyNonAlphabeticCharacters) {
                    System.out.println("Span: " + nameSpans[i].toString());
                    //System.out.println("Covered text is: " + tokens[nameSpans[i].getStart()] + " " + tokens[nameSpans[i].getStart() + 1]);
                    System.out.println("Covered text is: " + token);
                    System.out.println("Probability is: " + spanProbs[i]);
                } else {
                    foundEntities.remove(token);
                }
            }
            System.out.println("Found entity: " + Arrays.toString(entities));
            System.out.println("Filtered entities: " + foundEntities.toString());


            double[] probsClone = spanProbs.clone();
            Arrays.sort(probsClone);
            int lastIndex = spanProbs.length - 1;

            //Span: [0..2) person
            //Covered text is: Jack London
            //Probability is: 0.7081556539712883
        } catch (Exception ex) {
            System.out.println("exception: " + ex);
        } finally {
            try {
                if (modelInToken != null) modelInToken.close();
            } catch (IOException e) {
                System.out.println("exception: " + e);
            }
            ;
            try {
                if (modelIn != null) modelIn.close();
            } catch (IOException e) {
                System.out.println("exception: " + e);

            }
            ;
        }
    }

    public static int getNonAlphabeticCharacterCount(String s) {
        if (s == null || s.trim().isEmpty()) {
            return 0;
        }
        int theCount = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.substring(i, i + 1).matches("[^A-Za-z]")) {
                theCount++;
            }
        }
        return theCount;
    }
}
