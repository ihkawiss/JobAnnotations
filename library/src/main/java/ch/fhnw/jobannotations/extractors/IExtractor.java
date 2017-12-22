package ch.fhnw.jobannotations.extractors;

import ch.fhnw.jobannotations.domain.JobOffer;

/**
 * Interface to be implemented by extractors of job offer information.
 *
 * @author Kevin Kirn <kevin.kirn@students.fhnw.ch>
 */
public interface IExtractor {

    /**
     * Parses job offer information from given job offer object.
     *
     * @param offer Job offer object to be parsed
     * @return Parsed information
     */
    String parse(JobOffer offer);

    /**
     * Learns and improves parsing by adding given data to the model that is used to parse job offer information.
     *
     * @param data Data to be added to the model
     */
    void learn(String data);

}
