package ch.fhnw.jobannotations.utils;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Hoang
 */

// TODO rename?
public class NlpHelper {
    public static final String NER_TAG_LOCATION = "I-LOC";

    private static NlpHelper instance;
    private StanfordCoreNLP pipeline;
    private MaxentTagger tagger;
    private DependencyParser parser;

    private NlpHelper() {
        pipeline = new StanfordCoreNLP(FileUtils.getStanfordCoreNLPGermanConfiguration());
/*
        Properties germanConfig = FileUtils.getStanfordCoreNLPGermanConfiguration();
        String taggerPath = germanConfig.getProperty("pos.model");
        String modelPath = germanConfig.getProperty("depparse.model");
        tagger = new MaxentTagger(taggerPath);
        parser = DependencyParser.loadFromModelFile(modelPath);*/
    }

    public static NlpHelper getInstance() {
        if (instance == null) {
            instance = new NlpHelper();
        }
        return instance;
    }

    public StanfordCoreNLP getPipeline() {
        return pipeline;
    }

    public List<GrammaticalStructure> getGrammaticalStructures(String text) {
        List<GrammaticalStructure> grammaticalStructures = new ArrayList<>();
        DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
        for (List<HasWord> sentence : tokenizer) {
            List<TaggedWord> tagged = tagger.tagSentence(sentence);
            grammaticalStructures.add(parser.predict(tagged));
        }
        return grammaticalStructures;
    }

    public List<List<HasWord>> getTaggedSentences(String text) {
        List<List<HasWord>> taggedSentences = new ArrayList<>();
        DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
        for (List<HasWord> sentence : tokenizer) {
            taggedSentences.add(sentence);
        }
        return taggedSentences;
    }

    public List<CoreMap> annotateSentences(String text) {
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        return document.get(CoreAnnotations.SentencesAnnotation.class);
    }

    public SemanticGraph getSemanticGraphOfSentence(CoreMap annotatedSentence) {
            return annotatedSentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);


        //((ArrayList<TypedDependency>)annotatedSentences.get(0).get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class).typedDependencies()).get(1).dep().tag()

    }
    public List<CoreLabel> getTokensOfSentence(CoreMap annotatedSentence) {
        // document.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(CoreAnnotations.TokensAnnotation.class).get(10).get(CoreAnnotations.NamedEntityTagAnnotation.class)
        return annotatedSentence.get(CoreAnnotations.TokensAnnotation.class);
    }

    public List<CoreMap> getAnnotatedSentences(String text) {
        Annotation document = new Annotation(text);

        pipeline.annotate(document);
        return document.get(CoreAnnotations.SentencesAnnotation.class);
    }


    private Properties getDependencyParserProperties() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(FileUtils.getFileAsInputStream("configuration/StanfordCoreNLP-german-dependency-parser.properties"), "UTF8"));

            Properties props = new Properties();
            props.load(reader);

            return props;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
