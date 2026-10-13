package org.ikasan.job.orchestration.context.cache;

import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;

/**
 * Builder class for constructing unique job lock identifiers.
 * Provides utility methods to create identifiers from various combinations of job and context information.
 */
public class JobLockIdentifierBuilder {

    public static final String PARENT_CONTEXT_NAME = ":parent-context-name:";
    public static final String CONTEXT_NAME = ":context-name:";
    public static final String CONTEXT_ID = ":context-id:";

    /**
     * Constructs a unique identifier by concatenating the given parameters with predefined key constants.
     *
     * @param jobIdentifier        the identifier for the job
     * @param parentContextName    the name of the parent context
     * @param contextName          the name of the current context
     * @param contextInstanceId    the identifier for the context instance
     * @return a concatenated string that represents the unique identifier
     */
    public static String getIdentifier(String jobIdentifier
        , String parentContextName, String contextName, String contextInstanceId) {
        return jobIdentifier + PARENT_CONTEXT_NAME + parentContextName
            + CONTEXT_NAME + contextName + CONTEXT_ID + contextInstanceId;
    }

    /**
     * Constructs a unique identifier by concatenating the given parameters with predefined key constants.
     *
     * @param jobIdentifier     the identifier for the job
     * @param parentContextName the name of the parent context
     * @param contextName       the name of the current context
     * @return a concatenated string that represents the unique identifier
     */
    public static String getIdentifier(String jobIdentifier
        , String parentContextName, String contextName) {
        return jobIdentifier + PARENT_CONTEXT_NAME + parentContextName
            + CONTEXT_NAME + contextName;
    }

    /**
     * Constructs a unique identifier for the given job by utilizing its properties.
     *
     * @param job the job participant containing the identifier and context information
     * @return a concatenated string representing the unique identifier for the job
     */
    public static String getIdentifier(SchedulerJobLockParticipant job) {
        return getIdentifier(job.getIdentifier(), job.getContextName(), job.getChildContextName());
    }

    /**
     * Extracts the job identifier from the uber identifier.
     *
     * @param uberIdentifier the complete identifier containing job and context information
     * @return the job identifier portion, or null if the uber identifier doesn't contain the expected format
     */
    public static String getJobIdentifier(String uberIdentifier) {
        if (uberIdentifier == null) {
            return null;
        }
        int index = uberIdentifier.indexOf(PARENT_CONTEXT_NAME);
        return index >= 0 ? uberIdentifier.substring(0, index) : uberIdentifier;
    }

    /**
     * Extracts the parent context name from the uber identifier.
     *
     * @param uberIdentifier the complete identifier containing job and context information
     * @return the parent context name portion, or null if not found
     */
    public static String getParentContextName(String uberIdentifier) {
        if (uberIdentifier == null) {
            return null;
        }
        int startIndex = uberIdentifier.indexOf(PARENT_CONTEXT_NAME);
        if (startIndex < 0) {
            return null;
        }
        startIndex += PARENT_CONTEXT_NAME.length();

        int endIndex = uberIdentifier.indexOf(CONTEXT_NAME, startIndex);
        if (endIndex < 0) {
            return uberIdentifier.substring(startIndex);
        }
        return uberIdentifier.substring(startIndex, endIndex);
    }

    /**
     * Extracts the context name from the uber identifier.
     *
     * @param uberIdentifier the complete identifier containing job and context information
     * @return the context name portion, or null if not found
     */
    public static String getContextName(String uberIdentifier) {
        if (uberIdentifier == null) {
            return null;
        }
        int startIndex = uberIdentifier.indexOf(CONTEXT_NAME);
        if (startIndex < 0) {
            return null;
        }
        startIndex += CONTEXT_NAME.length();

        int endIndex = uberIdentifier.indexOf(CONTEXT_ID, startIndex);
        if (endIndex < 0) {
            return uberIdentifier.substring(startIndex);
        }
        return uberIdentifier.substring(startIndex, endIndex);
    }

    /**
     * Extracts the context instance ID from the uber identifier.
     *
     * @param uberIdentifier the complete identifier containing job and context information
     * @return the context instance ID portion, or null if not found
     */
    public static String getContextInstanceId(String uberIdentifier) {
        if (uberIdentifier == null) {
            return null;
        }
        int startIndex = uberIdentifier.indexOf(CONTEXT_ID);
        if (startIndex < 0) {
            return null;
        }
        startIndex += CONTEXT_ID.length();
        return uberIdentifier.substring(startIndex);
    }
}
