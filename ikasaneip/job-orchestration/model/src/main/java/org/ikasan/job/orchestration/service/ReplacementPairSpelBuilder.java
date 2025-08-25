package org.ikasan.job.orchestration.service;

import org.ikasan.spec.scheduled.job.model.ReplacementPair;

public class ReplacementPairSpelBuilder {
    private static final String FILE_NAME_REPLACE_VARIABLE =  "#fileNamePattern";
    private static final String FILE_PATH_REPLACE_VARIABLE =  "#filePathPattern";
    private static final String CONTEXT_PARAM_SPEL_REPLACE =".replace('%s', T(org.ikasan.ootb.scheduler.agent.rest.cache.ContextInstanceCache)" +
        ".getContextParameter(#correlatingIdentifier, '%s'))";

    boolean replacementPairProvided = false;

    private StringBuilder stringBuilder;

    /**
     * Constructs a ReplacementPairSpelBuilder with the provided variable.
     *
     * @param variable the initial variable to be used in the ReplacementPairSpelBuilder
     */
    private ReplacementPairSpelBuilder(String variable) {
        this.stringBuilder = new StringBuilder(variable);
    }

    /**
     * Returns a ReplacementPairSpelBuilder with the initial variable set to the predefined FILE_NAME_REPLACE_VARIABLE.
     *
     * @return a ReplacementPairSpelBuilder object with FILE_NAME_REPLACE_VARIABLE as the initial variable
     */
    public static ReplacementPairSpelBuilder fileNameReplace() {
        return new ReplacementPairSpelBuilder(FILE_NAME_REPLACE_VARIABLE);
    }

    /**
     * Creates a ReplacementPairSpelBuilder object with the initial variable set to the predefined FILE_PATH_REPLACE_VARIABLE.
     *
     * @return a ReplacementPairSpelBuilder object with FILE_PATH_REPLACE_VARIABLE as the initial variable
     */
    public static ReplacementPairSpelBuilder filePathReplace() {
        return new ReplacementPairSpelBuilder(FILE_PATH_REPLACE_VARIABLE);
    }

    /**
     * Appends a replacement token to the StringBuilder using the provided ReplacementPair.
     *
     * @param replacementPair the ReplacementPair containing the replacement token
     * @return the current ReplacementPairSpelBuilder instance
     */
    public ReplacementPairSpelBuilder withReplacement(ReplacementPair replacementPair) {
        stringBuilder.append(String.format(CONTEXT_PARAM_SPEL_REPLACE, replacementPair.getReplacementToken()
            , replacementPair.getJobPlanParameterName()));
        replacementPairProvided = true;
        return this;
    }

    /**
     * Returns the built string representation of the ReplacementPairSpelBuilder.
     *
     * @return the built string representation
     */
    public String build() {
        if(!this.replacementPairProvided) {
            throw new RuntimeException("At least one replacement pair needs to be provided!");
        }

        return this.stringBuilder.toString();
    }
}
