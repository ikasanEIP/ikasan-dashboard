package org.ikasan.job.orchestration.service;

import org.ikasan.job.orchestration.model.job.ReplacementPairImpl;
import org.ikasan.spec.scheduled.job.model.ReplacementPair;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReplacementPairSpelBuilderTest {

    @Test
    public void test_filename_replacement_success() {
        ReplacementPair replacementPair = new ReplacementPairImpl();
        replacementPair.setJobPlanParameterName("param_name");
        replacementPair.setReplacementToken("<token>");

        ReplacementPairSpelBuilder builder = ReplacementPairSpelBuilder.fileNameReplace();
        builder.withReplacement(replacementPair);

        String result = builder.build();

        assertEquals("#fileNamePattern.replace('<token>', T(org.ikasan.ootb.scheduler.agent.rest.cache.ContextInstanceCache)" +
                ".getContextParameter(#correlatingIdentifier, 'param_name'))"
            , result);
    }

    @Test
    public void test_filename_replacement_multiple_replacement_pairs_success() {
        ReplacementPair replacementPair = new ReplacementPairImpl();
        replacementPair.setJobPlanParameterName("param_name1");
        replacementPair.setReplacementToken("<token1>");

        ReplacementPairSpelBuilder builder = ReplacementPairSpelBuilder.fileNameReplace();
        builder.withReplacement(replacementPair);

        replacementPair = new ReplacementPairImpl();
        replacementPair.setJobPlanParameterName("param_name2");
        replacementPair.setReplacementToken("<token3>");

        builder.withReplacement(replacementPair);

        String result = builder.build();

        assertEquals("#fileNamePattern.replace('<token1>', T(org.ikasan.ootb.scheduler.agent.rest.cache.ContextInstanceCache)" +
                ".getContextParameter(#correlatingIdentifier, 'param_name1'))" +
                ".replace('<token3>', T(org.ikasan.ootb.scheduler.agent.rest.cache.ContextInstanceCache)" +
                ".getContextParameter(#correlatingIdentifier, 'param_name2'))"
            , result);
    }

    @Test(expected = RuntimeException.class)
    public void test_filename_replacement_no_replacement_pairs_provided_exception() {
        ReplacementPairSpelBuilder builder = ReplacementPairSpelBuilder.fileNameReplace();
        builder.build();
    }

    @Test
    public void test_file_path_replacement_success() {
        ReplacementPair replacementPair = new ReplacementPairImpl();
        replacementPair.setJobPlanParameterName("param_name");
        replacementPair.setReplacementToken("<token>");

        ReplacementPairSpelBuilder builder = ReplacementPairSpelBuilder.filePathReplace();
        builder.withReplacement(replacementPair);

        String result = builder.build();

        assertEquals("#filePathPattern.replace('<token>', T(org.ikasan.ootb.scheduler.agent.rest.cache.ContextInstanceCache)" +
                ".getContextParameter(#correlatingIdentifier, 'param_name'))"
            , result);
    }

    @Test
    public void test_file_path_replacement_multiple_replacement_pairs_success() {
        ReplacementPair replacementPair = new ReplacementPairImpl();
        replacementPair.setJobPlanParameterName("param_name1");
        replacementPair.setReplacementToken("<token1>");

        ReplacementPairSpelBuilder builder = ReplacementPairSpelBuilder.filePathReplace();
        builder.withReplacement(replacementPair);

        replacementPair = new ReplacementPairImpl();
        replacementPair.setJobPlanParameterName("param_name2");
        replacementPair.setReplacementToken("<token3>");

        builder.withReplacement(replacementPair);

        String result = builder.build();

        assertEquals("#filePathPattern.replace('<token1>', T(org.ikasan.ootb.scheduler.agent.rest.cache.ContextInstanceCache)" +
                ".getContextParameter(#correlatingIdentifier, 'param_name1'))" +
                ".replace('<token3>', T(org.ikasan.ootb.scheduler.agent.rest.cache.ContextInstanceCache)" +
                ".getContextParameter(#correlatingIdentifier, 'param_name2'))"
            , result);
    }

    @Test(expected = RuntimeException.class)
    public void test_file_path_replacement_no_replacement_pairs_provided_exception() {
        ReplacementPairSpelBuilder builder = ReplacementPairSpelBuilder.filePathReplace();
        builder.build();
    }
}
