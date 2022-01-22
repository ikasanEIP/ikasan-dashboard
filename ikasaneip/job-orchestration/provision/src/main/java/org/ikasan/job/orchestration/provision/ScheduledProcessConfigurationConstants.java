package org.ikasan.job.orchestration.provision;

public class ScheduledProcessConfigurationConstants {

    // Scheduled Consumer Configuration Properties
    public static final String JOB_NAME = "jobName";
    public static final String JOB_GROUP_NAME = "jobGroupName";
    public static final String JOB_DESCRIPTION = "description";
    public static final String CRON_EXPRESSION = "cronExpression";
    public static final String TIMEZONE = "timezone";
    public static final String IGNORE_MISFIRE = "ignoreMisfire";
    public static final String EAGER = "eager";
    public static final String MAX_EAGER_CALLBACKS = "maxEagerCallbacks";
    public static final String PASS_THROUGH_PROPERTIES = "passthroughProperties";

    // File Consumer Configuration Properties
    public static final String FILENAMES = "filenames";

    // Process Execution Broker Configuration Properties
    public static final String COMMAND_LINE = "commandLine";
    public static final String WORKING_DIRECTORY = "workingDirectory";
    public static final String SUCCESSFUL_RETURN_CODES = "successfulReturnCodes";
    public static final String SECONDS_TO_WAIT_FOR_PROCESS_TO_START = "secondsToWaitForProcessStart";
    public static final String STD_ERR = "stdErr";
    public static final String STD_OUT = "stdOut";
    public static final String RETRY_ON_FAIL = "retryOnFail";

    // Blackout Router Configuration Properties
    public static final String CRON_EXPRESSIONS = "cronExpressions";
    public static final String DATE_TIME_RANGES = "dateTimeRanges";


}
