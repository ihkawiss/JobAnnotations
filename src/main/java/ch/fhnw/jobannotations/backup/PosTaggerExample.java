package ch.fhnw.jobannotations.backup;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.WhitespaceTokenizer;

import java.io.FileInputStream;
import java.io.InputStream;

public class PosTaggerExample {

    public static void main(String args[]) throws Exception{

        //Loading Parts of speech-maxent model
        InputStream inputStream = new FileInputStream(PosTaggerExample.class.getClassLoader().getResource("de-pos-maxent.bin").getFile());
        POSModel model = new POSModel(inputStream) ;

        //Instantiating POSTaggerME class
        POSTaggerME tagger = new POSTaggerME(model) ;

        String sentence = "Sie besitzen ausgeprägtes Flair für die Informatik, das industrielle Umfeld sowie eine gute Portion Pioniergeist?\n" +
                "\n" +
                "Zur Erweiterung der Abteilung Technik sucht unser renommierter, erfolgreich national und international tätiger, innovationskräftiger Mandant in der Region Aarau Sie, DIE erfolgreiche Schnittstelle mit Industrie 4.0, als\n" +
                "\n" +
                "Prozessverantwortlicher integrales Datenmanagement m/w, 100%\n" +
                "\n" +
                "Referenz 1415-100793-669-2\n" +
                "\n" +
                "Tätigkeitsgebiet";

        //Tokenizing the sentence using WhitespaceTokenizer class
        WhitespaceTokenizer whitespaceTokenizer= WhitespaceTokenizer.INSTANCE;
        String[] tokens = whitespaceTokenizer.tokenize(sentence) ;

        //Generating tags
        String[] tags = tagger.tag(tokens) ;

        //Instantiating the POSSample class
        POSSample sample = new POSSample(tokens, tags) ;
        System.out.println(sample.toString() );

    }
}
