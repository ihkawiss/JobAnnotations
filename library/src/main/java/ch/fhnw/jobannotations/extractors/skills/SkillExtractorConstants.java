package ch.fhnw.jobannotations.extractors.skills;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class holds constants that are used in {@link SkillExtractor}
 *
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
 */
class SkillExtractorConstants {
    static final List<String> DETERMINERS_YOUR = Arrays.asList("ihre", "ihr", "deine", "dein");
    static final List<String> DETERMINERS_OUR = Arrays.asList("unsere", "unser");
    static final List<String> SUBJECTS_YOU = Arrays.asList("sie", "du");
    static final List<String> SUBJECTS_WE = Arrays.asList("wir", "ich");
    static final List<String> NOUNS_SKILL_SYNONYM = Arrays.asList(
            "qualifikation",
            "profil",
            "erfahrung",
            "voraussetzung",
            "anforderung",
            "kompetenz",
            "fähigkeit",
            "kenntniss",
            "eigenschaft",
            "talent"
    );
    static final List<String> NOUNS_JOB_SYNONYM = Arrays.asList(
            "aufgabe",
            "herausforderung",
            "wirkungsfeld",
            "job",
            "funktion",
            "tätigkeit",
            "arbeit"
    );
    static final List<String> NOUNS_EXPECTATION_SYNONYM = Arrays.asList(
            "anforderung",
            "anforderungen",
            "erwartung",
            "erwartungen"
    );
    static final List<String> NOUNS_OFFER_SYNONYM = Collections.singletonList("angebot");
    static final List<String> NOUNS_POSSIBILITY_SYNONYM = Arrays.asList("möglichkeit", "perspektive");
    static final List<String> VERBS_OFFER = Arrays.asList(
            "bringen",
            "bringst",
            "bieten",
            "bietest",
            "geboten",
            "mitbringen",
            "mitbringst",
            "haben",
            "hast",
            "begeistern"
    );
    static final List<String> VERBS_EXPECT = Arrays.asList(
            "erwarten",
            "erwartet",
            "erwartest",
            "verlangen",
            "verlangst",
            "verlangt",
            "finden",
            "findest"
    );
    static final int SKILL_NOUN_DEFAULT_DISTANCE_ADJUST_VALUE = 3000;
    static final String SEPARATOR = ",";
    static final String SPECIAL_CHARACTER_REGEX = "[^a-zA-Z .,:äöüÄÖÜ]";
}
