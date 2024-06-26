package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileEventDrivenJobBuilder  {

    protected String agentName;
    protected String jobName;
    protected String displayName;
    protected String contextName;
    protected List<String> childContextNames;
    protected String description;
    protected String startupControlType = "AUTOMATIC";
    protected String cronExpression;
    protected String jobGroup;
    protected String timeZone;
    protected boolean ignoreMisfire = true;
    protected boolean eager = false;
    protected int maxEagerCallbacks;
    protected Map<String,String> passthroughProperties;
    protected boolean persistentRecovery = true;
    protected long recoveryTolerance = 30 * 60 * 1000;
    protected Map<String, String> blackoutWindowDateTimeRanges = new HashMap<>();
    protected List<String> blackoutWindowCronExpressions = new ArrayList<>();

    private String filePath;
    private String moveDirectory;
    private List<String> filenames;
    private String encoding;
    private boolean includeHeader;
    private boolean includeTrailer;
    private boolean sortByModifiedDateTime;
    private boolean sortAscending = true;
    private int directoryDepth = 1;
    private boolean logMatchedFilenames = false;
    private boolean ignoreFileRenameWhilstScanning = true;
    private int minFileAgeSeconds;

    /**
     *
     */
    public FileEventDrivenJobBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    /**
     * Sets the job name for the FileEventDrivenJobBuilder.
     *
     * @param jobName the name of the job
     * @return the FileEventDrivenJobBuilder instance
     */
    public FileEventDrivenJobBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    /**
     * Sets the context name for the FileEventDrivenJobBuilder.
     *
     * @param contextName The name of the context.
     * @return The FileEventDrivenJobBuilder instance.
     */
    public FileEventDrivenJobBuilder withContextName(String contextName) {
        this.contextName = contextName;

        return this;
    }

    /**
     * Adds a child context ID to the FileEventDrivenJobBuilder.
     *
     * @param childContextId the ID of the child context to add
     * @return the FileEventDrivenJobBuilder instance
     */
    public FileEventDrivenJobBuilder addChildContextId(String childContextId) {
        if(this.childContextNames == null) {
            this.childContextNames = new ArrayList<>();
        }

        this.childContextNames.add(childContextId);

        return this;
    }

    /**
     * Sets the description of the FileEventDrivenJobBuilder.
     *
     * @param description the description to set
     * @return the updated FileEventDrivenJobBuilder
     */
    public FileEventDrivenJobBuilder withDescription(String description) {
        this.description = description;

        return this;
    }

    /**
     *
     */
    public FileEventDrivenJobBuilder withStartupControlType(String startupControlType) {
        this.startupControlType = startupControlType;

        return this;
    }

    /**
     * Sets the cron expression for scheduling the job.
     *
     * @param cronExpression the cron expression for scheduling the job
     * @return the current instance of the FileEventDrivenJobBuilder
     */
    public FileEventDrivenJobBuilder withCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;

        return this;
    }

    /**
     * Sets the job group for the file event-driven job builder.
     *
     * @param jobGroup the job group to set
     * @return a reference to the file event-driven job builder
     */
    public FileEventDrivenJobBuilder withJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;

        return this;
    }

    /**
     *
     */
    public FileEventDrivenJobBuilder withTimeZone(String timeZone) {
        this.timeZone = timeZone;

        return this;
    }

    /**
     *
     */
    public FileEventDrivenJobBuilder withIgnoreMisfire(boolean ignoreMisfire) {
        this.ignoreMisfire = ignoreMisfire;

        return this;
    }

    /**
     *
     */
    public FileEventDrivenJobBuilder withMaxEagerCallbacks(int maxEagerCallbacks) {
        this.maxEagerCallbacks = maxEagerCallbacks;

        return this;
    }

    /**
     * Sets the eager flag for the FileEventDrivenJobBuilder.
     *
     * @param eager the eager flag to set
     * @return the updated FileEventDrivenJobBuilder
     */
    public FileEventDrivenJobBuilder withEager(boolean eager) {
        this.eager = eager;

        return this;
    }

    /**
     * Sets the passthrough properties for the job.
     *
     * @param passthroughProperties the passthrough properties to set
     * @return the FileEventDrivenJobBuilder instance
     */
    public FileEventDrivenJobBuilder withPassthroughProperties(Map<String, String> passthroughProperties) {
        this.passthroughProperties = passthroughProperties;

        return this;
    }

    /**
     * Sets the flag indicating whether persistent recovery is enabled or not.
     *
     * @param persistentRecovery the flag indicating whether persistent recovery is enabled or not
     * @return the updated {@link FileEventDrivenJobBuilder} instance
     */
    public FileEventDrivenJobBuilder withPersistentRecovery(boolean persistentRecovery) {
        this.persistentRecovery = persistentRecovery;

        return this;
    }

    /**
     * Sets the recovery tolerance for the file event driven job builder.
     *
     * @param recoveryTolerance the recovery tolerance value
     * @return the file event driven job builder instance
     */
    public FileEventDrivenJobBuilder withRecoveryTolerance(long recoveryTolerance) {
        this.recoveryTolerance = recoveryTolerance;

        return this;
    }

    /**
     *
     */
    public FileEventDrivenJobBuilder withBlackoutWindowCronExpression(String blackoutWindowCronExpression) {
        this.blackoutWindowCronExpressions.add(blackoutWindowCronExpression);
        return this;
    }

    /**
     *
     */
    public FileEventDrivenJobBuilder withBlackoutDateTimeWindow(long windowStart, long windowEnd) {
        this.blackoutWindowDateTimeRanges.put(String.valueOf(windowStart), String.valueOf(windowEnd));
        return this;
    }

    /**
     * Sets the file path for the FileEventDrivenJobBuilder.
     *
     * @param filePath the file path to set
     * @return the FileEventDrivenJobBuilder object
     */
    public FileEventDrivenJobBuilder withFilePath(String filePath) {
        this.filePath = filePath;

        return this;
    }

    /**
     * Sets the directory to which the files will be moved.
     *
     * @param moveDirectory the directory to move the files to
     * @return the current instance of the FileEventDrivenJobBuilder
     */
    public FileEventDrivenJobBuilder withMoveDirectory(String moveDirectory) {
        this.moveDirectory = moveDirectory;

        return this;
    }

    /**
     * Sets the list of filenames for the FileEventDrivenJobBuilder.
     *
     * @param filenames the list of filenames to set
     * @return the FileEventDrivenJobBuilder instance
     */
    public FileEventDrivenJobBuilder withFilenames(List<String> filenames) {
        this.filenames = filenames;

        return this;
    }

    /**
     * Sets the encoding for the FileEventDrivenJobBuilder.
     *
     * @param encoding the encoding to be set
     * @return the FileEventDrivenJobBuilder instance
     */
    public FileEventDrivenJobBuilder withEncoding(String encoding) {
        this.encoding = encoding;

        return this;
    }

    /**
     *
     */
    public FileEventDrivenJobBuilder withIncludeHeader(boolean includeHeader) {
        this.includeHeader = includeHeader;

        return this;
    }

    /**
     * Sets whether to include a trailer in the file event-driven job.
     *
     * @param includeTrailer boolean value indicating whether to include a trailer
     * @return the FileEventDrivenJobBuilder instance
     */
    public FileEventDrivenJobBuilder withIncludeTrailer(boolean includeTrailer) {
        this.includeTrailer = includeTrailer;

        return this;
    }

    /**
     * Sets the flag indicating whether to sort the files by modified date and time.
     *
     * @param sortByModifiedDateTime true to enable sorting by modified date and time, false otherwise
     * @return the FileEventDrivenJobBuilder instance
     */
    public FileEventDrivenJobBuilder withSortByModifiedDateTime(boolean sortByModifiedDateTime) {
        this.sortByModifiedDateTime = sortByModifiedDateTime;

        return this;
    }

    /**
     * Sets the sort order for the files.
     *
     * @param sortAscending {@code true} to sort the files in ascending order, {@code false} to sort them in descending order
     * @return the {@code FileEventDrivenJobBuilder} object for method chaining
     */
    public FileEventDrivenJobBuilder withSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;

        return this;
    }

    /**
     * Sets the directory depth for scanning files.
     *
     * @param directoryDepth the depth of directories to scan
     * @return the FileEventDrivenJobBuilder instance
     */
    public FileEventDrivenJobBuilder withDirectoryDepth(int directoryDepth) {
        this.directoryDepth = directoryDepth;

        return this;
    }

    /**
     * Sets the flag to log matched filenames during job execution.
     *
     * @param logMatchedFilenames true to log matched filenames, false otherwise
     * @return the FileEventDrivenJobBuilder instance
     */
    public FileEventDrivenJobBuilder withLogMatchedFilenames(boolean logMatchedFilenames) {
        this.logMatchedFilenames = logMatchedFilenames;

        return this;
    }

    /**
     * Sets whether to ignore file rename whilst scanning.
     *
     * @param ignoreFileRenameWhilstScanning true to ignore file rename whilst scanning, false otherwise
     * @return the FileEventDrivenJobBuilder instance
     */
    public FileEventDrivenJobBuilder withIgnoreFileRenameWhilstScanning(boolean ignoreFileRenameWhilstScanning) {
        this.ignoreFileRenameWhilstScanning = ignoreFileRenameWhilstScanning;

        return this;
    }

    /**
     * Sets the minimum file age in seconds.
     *
     * @param minFileAgeSeconds the minimum file age in seconds to set
     * @return the FileEventDrivenJobBuilder instance
     */
    public FileEventDrivenJobBuilder withMinFileAgeSeconds(int minFileAgeSeconds) {
        this.minFileAgeSeconds = minFileAgeSeconds;

        return this;
    }

    /**
     * Sets the display name for the FileEventDrivenJobBuilder.
     *
     * @param displayName the display name to set
     * @return the updated FileEventDrivenJobBuilder instance
     */
    public FileEventDrivenJobBuilder withDisplayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

    /**
     * Builds a FileEventDrivenJob object with the specified properties.
     *
     * @return a FileEventDrivenJob object
     */
    public FileEventDrivenJob build() {
        FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
        fileEventDrivenJob.setFilePath(this.filePath);
        fileEventDrivenJob.setMoveDirectory(this.moveDirectory);
        fileEventDrivenJob.setFilenames(this.filenames);
        fileEventDrivenJob.setAgentName(this.agentName);
        fileEventDrivenJob.setIdentifier(this.agentName+"-"+this.jobName);
        fileEventDrivenJob.setJobDescription(this.description);
        fileEventDrivenJob.setJobName(this.jobName);
        fileEventDrivenJob.setContextName(this.contextName);
        fileEventDrivenJob.setChildContextNames(this.childContextNames);
        fileEventDrivenJob.setCronExpression(this.cronExpression);
        fileEventDrivenJob.setPassthroughProperties(this.passthroughProperties);
        fileEventDrivenJob.setTimeZone(this.timeZone);
        fileEventDrivenJob.setJobGroup(this.jobGroup);
        fileEventDrivenJob.setStartupControlType(this.startupControlType);
        fileEventDrivenJob.setBlackoutWindowCronExpressions(this.blackoutWindowCronExpressions);
        fileEventDrivenJob.setBlackoutWindowDateTimeRanges(this.blackoutWindowDateTimeRanges);
        fileEventDrivenJob.setEncoding(this.encoding);
        fileEventDrivenJob.setIncludeHeader(this.includeHeader);
        fileEventDrivenJob.setIncludeTrailer(this.includeTrailer);
        fileEventDrivenJob.setSortByModifiedDateTime(this.sortByModifiedDateTime);
        fileEventDrivenJob.setSortAscending(this.sortAscending);
        fileEventDrivenJob.setDirectoryDepth(this.directoryDepth);
        fileEventDrivenJob.setLogMatchedFilenames(this.logMatchedFilenames);
        fileEventDrivenJob.setIgnoreFileRenameWhilstScanning(this.ignoreFileRenameWhilstScanning);
        fileEventDrivenJob.setMinFileAgeSeconds(this.minFileAgeSeconds);
        fileEventDrivenJob.setDisplayName(this.displayName);

        return fileEventDrivenJob;
    }
}
