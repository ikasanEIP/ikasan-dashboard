package org.ikasan.job.orchestration.context.util;

import java.util.*;

import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class SchedulerContextParametersPropertiesProvider extends Properties {
    private final static Logger LOG = LoggerFactory.getLogger(SchedulerContextParametersPropertiesProvider.class);

    private final boolean useSkipJobs;
    private final Map<String, Map<String, Boolean>> jobsToSkip;
    private final boolean replaceContextParameters;
    private final Map<String, Map<String, String>> parametersToReplace;
    private Map<String, String> spelExpressionMap;

    public SchedulerContextParametersPropertiesProvider(boolean useSkipJobs,
                                                        Map<String, Map<String, Boolean>> jobsToSkip,
                                                        boolean replaceContextParameters,
                                                        Map<String, Map<String, String>> parametersToReplace,
                                                        Map<String, String> spelExpressionMap) {

        this.useSkipJobs = useSkipJobs;
        this.jobsToSkip = jobsToSkip == null ? Collections.emptyMap() : jobsToSkip;
        this.replaceContextParameters = replaceContextParameters;
        this.parametersToReplace = parametersToReplace == null ? Collections.emptyMap() : parametersToReplace;
        this.spelExpressionMap = spelExpressionMap == null ? Collections.emptyMap() : spelExpressionMap;

        String message = String.format("Creating SchedulerContextParametersPropertiesProvider configuration with use jobsToSkip %b, " +
            "jobsToSkips %s, replaceContextParameters %b, parametersToReplace %s, spelExpressionMap %s",
            this.useSkipJobs, this.jobsToSkip, this.replaceContextParameters, this.parametersToReplace, this.spelExpressionMap);
        LOG.info(message);
    }

    public boolean isSkipped(String contextName, String jobName) {
        if (useSkipJobs && contextName != null && jobName != null) {
            Map<String, Boolean> jobsToSkipMap = jobsToSkip.get(contextName);
            if (jobsToSkipMap != null) {
                Boolean shouldSkip = jobsToSkipMap.get(jobName);
                return shouldSkip != null && shouldSkip;
            }
        }
        return false;
    }

    public String getContextParameter(String contextName, String paramName) {
        if (replaceContextParameters && contextName != null && paramName != null) {
            Map<String, String> paramMap = parametersToReplace.get(contextName);
            if (paramMap != null) {
                return replaceParameterWithSpel(paramName, paramMap.get(paramName));
            }
        }
        return null;
    }

    public List<ContextParameterInstance> getAllContextParameters(String contextName) {
        List<ContextParameterInstance> params = new ArrayList<>();
        if (contextName != null) {
            Map<String, String> paramMap = parametersToReplace.get(contextName);
            if (paramMap != null) {
                for (String name : paramMap.keySet()) {
                    ContextParameterInstanceImpl param = new ContextParameterInstanceImpl();
                    param.setName(name);
                    String value = paramMap.get(name);
                    value = replaceParameterWithSpel(name, value);
                    param.setValue(value);
                    param.setType("java.lang.String");
                    params.add(param);
                }
            }
        }
        return params;
    }

    private String replaceParameterWithSpel(String name, String value) {
        if (spelExpressionMap != null && !spelExpressionMap.isEmpty()) {
            for (String key : spelExpressionMap.keySet()) {
                if (key.equals(value)) {
                    String spel = spelExpressionMap.get(key);
                    ExpressionParser parser = new SpelExpressionParser();
                    Expression exp = parser.parseExpression(spel);
                    String newValue = exp.getValue(spel, String.class);
                    LOG.info(String.format("Replacing name [%s] parameter [%s] with new value [%s]", name, value, newValue));
                    return newValue;
                }
            }
        }
        return value;
    }

}
