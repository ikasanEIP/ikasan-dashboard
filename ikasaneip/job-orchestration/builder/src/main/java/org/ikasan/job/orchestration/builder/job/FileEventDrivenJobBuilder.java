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
     * Set the agent name.
     *
     * @param agentName
     * @return
     */
    public FileEventDrivenJobBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    /**
     * Set the job name.
     *
     * @param jobName
     * @return
     */
    public FileEventDrivenJobBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    /**
     * Set the context name.
     *
     * @param contextName
     * @return
     */
    public FileEventDrivenJobBuilder withContextName(String contextName) {
        this.contextName = contextName;

        return this;
    }

    /**
     * Add a child context id.
     *
     * @param childContextId
     * @return
     */
    public FileEventDrivenJobBuilder addChildContextId(String childContextId) {
        if(this.childContextNames == null) {
            this.childContextNames = new ArrayList<>();
        }

        this.childContextNames.add(childContextId);

        return this;
    }

    /**
     * Set the job description.
     *
     * @param description
     * @return
     */
    public FileEventDrivenJobBuilder withDescription(String description) {
        this.description = description;

        return this;
    }

    /**
     * Set the job startupControlType.
     *
     * @param startupControlType
     * @return
     */
    public FileEventDrivenJobBuilder withStartupControlType(String startupControlType) {
        this.startupControlType = startupControlType;

        return this;
    }

    public FileEventDrivenJobBuilder withCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;

        return this;
    }

    public FileEventDrivenJobBuilder withJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;

        return this;
    }

    public FileEventDrivenJobBuilder withTimeZone(String timeZone) {
        this.timeZone = timeZone;

        return this;
    }

    public FileEventDrivenJobBuilder withIgnoreMisfire(boolean ignoreMisfire) {
        this.ignoreMisfire = ignoreMisfire;

        return this;
    }

    public FileEventDrivenJobBuilder withMaxEagerCallbacks(int maxEagerCallbacks) {
        this.maxEagerCallbacks = maxEagerCallbacks;

        return this;
    }

    public FileEventDrivenJobBuilder withEager(boolean eager) {
        this.eager = eager;

        return this;
    }

    public FileEventDrivenJobBuilder withPassthroughProperties(Map<String, String> passthroughProperties) {
        this.passthroughProperties = passthroughProperties;

        return this;
    }

    public FileEventDrivenJobBuilder withPersistentRecovery(boolean persistentRecovery) {
        this.persistentRecovery = persistentRecovery;

        return this;
    }

    public FileEventDrivenJobBuilder withRecoveryTolerance(long recoveryTolerance) {
        this.recoveryTolerance = recoveryTolerance;

        return this;
    }

    public FileEventDrivenJobBuilder withBlackoutWindowCronExpression(String blackoutWindowCronExpression) {
        this.blackoutWindowCronExpressions.add(blackoutWindowCronExpression);
        return this;
    }

    public FileEventDrivenJobBuilder withBlackoutDateTimeWindow(long windowStart, long windowEnd) {
        this.blackoutWindowDateTimeRanges.put(String.valueOf(windowStart), String.valueOf(windowEnd));
        return this;
    }

    public FileEventDrivenJobBuilder withFilePath(String filePath) {
        this.filePath = filePath;

        return this;
    }

    public FileEventDrivenJobBuilder withMoveDirectory(String moveDirectory) {
        this.moveDirectory = moveDirectory;

        return this;
    }

    public FileEventDrivenJobBuilder withFilenames(List<String> filenames) {
        this.filenames = filenames;

        return this;
    }

    public FileEventDrivenJobBuilder withEncoding(String encoding) {
        this.encoding = encoding;

        return this;
    }

    public FileEventDrivenJobBuilder withIncludeHeader(boolean includeHeader) {
        this.includeHeader = includeHeader;

        return this;
    }

    public FileEventDrivenJobBuilder withIncludeTrailer(boolean includeTrailer) {
        this.includeTrailer = includeTrailer;

        return this;
    }

    public FileEventDrivenJobBuilder withSortByModifiedDateTime(boolean sortByModifiedDateTime) {
        this.sortByModifiedDateTime = sortByModifiedDateTime;

        return this;
    }

    public FileEventDrivenJobBuilder withSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;

        return this;
    }

    public FileEventDrivenJobBuilder withDirectoryDepth(int directoryDepth) {
        this.directoryDepth = directoryDepth;

        return this;
    }

    public FileEventDrivenJobBuilder withLogMatchedFilenames(boolean logMatchedFilenames) {
        this.logMatchedFilenames = logMatchedFilenames;

        return this;
    }

    public FileEventDrivenJobBuilder withIgnoreFileRenameWhilstScanning(boolean ignoreFileRenameWhilstScanning) {
        this.ignoreFileRenameWhilstScanning = ignoreFileRenameWhilstScanning;

        return this;
    }

    public FileEventDrivenJobBuilder withMinFileAgeSeconds(int minFileAgeSeconds) {
        this.minFileAgeSeconds = minFileAgeSeconds;

        return this;
    }

    public FileEventDrivenJobBuilder withDisplayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

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
