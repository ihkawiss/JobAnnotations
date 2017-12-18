package ch.fhnw.jobannotations.extractors;

import ch.fhnw.jobannotations.domain.JobOffer;

public interface IExtractor {

    String parse(JobOffer offer);

    void learn(String data);

}
