package ch.fhnw.jobannotations.train;

import ch.fhnw.jobannotations.utils.FileUtils;
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
public class JobTitleClassifierTrainer {
    private static String modelFileName = "de-ner-jobtitle.bin";
    private static String trainFileName = "jobs-it-v2.train";

    public static void main(String[] args) throws IOException {

        InputStreamFactory isf = new InputStreamFactory() {
            public InputStream createInputStream() throws IOException {
                return FileUtils.getFileAsInputStream(trainFileName);
            }
        };

        Charset charset = Charset.forName("UTF-8");
        ObjectStream<String> lineStream = new PlainTextByLineStream(isf, charset);
        ObjectStream<NameSample> sampleStream = new NameSampleDataStream(lineStream);

        TokenNameFinderModel model;
        TokenNameFinderFactory nameFinderFactory = new TokenNameFinderFactory();

        try {
            model = NameFinderME.train("de", "jobtitle", sampleStream, TrainingParameters.defaultParams(),
                    nameFinderFactory);
        } finally {
            sampleStream.close();
        }

        BufferedOutputStream modelOut = null;

        try {
            File targetDir = new File(JobTitleClassifierTrainer.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
            File projectRootDir = targetDir.getParentFile();
            File resourceDir = new File(projectRootDir.getPath()+"/src/main/resources");
            File outputDir = new File(targetDir.getPath()+"/output");
            outputDir.mkdirs(); // create necessary parent dirs
            File modelFile = new File(resourceDir, modelFileName);
            modelOut = new BufferedOutputStream(new FileOutputStream(modelFile));
            model.serialize(modelOut);
        } finally {
            if (modelOut != null)
                modelOut.close();
        }
    }
}
