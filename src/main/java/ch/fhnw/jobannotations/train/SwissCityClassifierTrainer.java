package ch.fhnw.jobannotations.train;

import opennlp.tools.namefind.*;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;

import java.io.*;
import java.nio.charset.Charset;

/**
 * @author Hoang
 */
public class SwissCityClassifierTrainer {
    /*
    static String onlpModelPath = "en-ner-drugs.bin";
    // training data set
    static String trainingDataFilePath = "D:/NLPTools/Datasets/drugsDetails.txt";

    public static void main(String[] args) throws IOException {
        boolean flag = false;
        if (flag) {
            Charset charset = Charset.forName("UTF-8");
            //ObjectStream<String> lineStream = new PlainTextByLineStream(new FileInputStream(trainingDataFilePath), charset);
            ObjectStream<String> lineStream = new PlainTextByLineStream(new MarkableFileInputStreamFactory(new File(trainingDataFilePath)), charset);
            ObjectStream<NameSample> sampleStream = new NameSampleDataStream(
                    lineStream);
            TokenNameFinderModel model = null;
            HashMap<String, Object> mp = new HashMap<String, Object>();
            try {

                model = NameFinderME.train("en", "drugs", sampleStream, Collections.<String, Object>emptyMap(), 100, 4);
                NameFinderME.train("de", "city", sampleStream, )
            } finally {
                sampleStream.close();
            }
            BufferedOutputStream modelOut = null;
            try {
                modelOut = new BufferedOutputStream(new FileOutputStream(onlpModelPath));
                model.serialize(modelOut);
            } finally {
                if (modelOut != null)
                    modelOut.close();
            }
        }


        // 2nd try
        Charset charset = Charset.forName("UTF-8");
        ObjectStream<String> lineStream =
                new PlainTextByLineStream(new FileInputStream("en-ner-person.train"), charset);
        ObjectStream<NameSample> sampleStream = new NameSampleDataStream(lineStream);

        TokenNameFinderModel model;

        try {
            TokenNameFinderFactory nameFinderFactory = null;
            model = NameFinderME.train("en", "person", sampleStream, TrainingParameters.defaultParams(),
                    nameFinderFactory);
        }
        finally {
            sampleStream.close();
        }

        try {
            modelOut = new BufferedOutputStream(new FileOutputStream(modelFile));
            model.serialize(modelOut);
        } finally {
            if (modelOut != null)
                modelOut.close();
        }

    }*/

    private static String modelFileName = "de-ner-city.bin";
    private static String trainFileName = "city.train";

    public static void main(String[] args) throws IOException {

        InputStreamFactory isf = new InputStreamFactory() {
            public InputStream createInputStream() throws IOException {
                return new FileInputStream(getClass().getResource(trainFileName).getFile());
            }
        };

        Charset charset = Charset.forName("UTF-8");
        ObjectStream<String> lineStream = new PlainTextByLineStream(isf, charset);
        ObjectStream<NameSample> sampleStream = new NameSampleDataStream(lineStream);

        TokenNameFinderModel model;
        TokenNameFinderFactory nameFinderFactory = new TokenNameFinderFactory();

        try {
            model = NameFinderME.train("de", "city", sampleStream, TrainingParameters.defaultParams(),
                    nameFinderFactory);
        } finally {
            sampleStream.close();
        }

        BufferedOutputStream modelOut = null;

        try {
            File targetDir = new File(SwissCityClassifierTrainer.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
            File outputDir = new File(targetDir.getPath()+"/output");
            outputDir.mkdirs(); // create necessary parent dirs
            File modelFile = new File(outputDir, modelFileName);
            modelOut = new BufferedOutputStream(new FileOutputStream(modelFile));
            model.serialize(modelOut);
        } finally {
            if (modelOut != null)
                modelOut.close();
        }
    }
}
