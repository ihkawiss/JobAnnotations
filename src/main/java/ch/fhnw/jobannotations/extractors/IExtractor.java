package ch.fhnw.jobannotations.extractors;

import ch.fhnw.jobannotations.JobOffer;

public interface IExtractor {

    String parse(JobOffer offer);

}
