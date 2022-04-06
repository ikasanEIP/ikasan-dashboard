package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;

import java.util.ArrayList;
import java.util.List;

public class FileEventDrivenJobBuilder extends QuartzScheduleDrivenJobBuilder {

    private String filePath;
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

    public FileEventDrivenJobBuilder withFilePath(String filePath) {
        this.filePath = filePath;

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

    public FileEventDrivenJob build() {
        FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
        fileEventDrivenJob.setFilePath(this.filePath);
        fileEventDrivenJob.setFilenames(this.filenames);
        fileEventDrivenJob.setAgentName(super.agentName);
        fileEventDrivenJob.setIdentifier(super.agentName+"-"+super.jobName);
        fileEventDrivenJob.setJobDescription(super.description);
        fileEventDrivenJob.setJobName(super.jobName);
        fileEventDrivenJob.setContextId(super.contextId);
        fileEventDrivenJob.setChildContextIds(super.childContextIds);
        fileEventDrivenJob.setCronExpression(super.cronExpression);
        fileEventDrivenJob.setPassthroughProperties(this.passthroughProperties);
        fileEventDrivenJob.setTimeZone(super.timeZone);
        fileEventDrivenJob.setJobGroup(super.jobGroup);
        fileEventDrivenJob.setStartupControlType(super.startupControlType);
        fileEventDrivenJob.setEncoding(this.encoding);
        fileEventDrivenJob.setIncludeHeader(this.includeHeader);
        fileEventDrivenJob.setIncludeTrailer(this.includeTrailer);
        fileEventDrivenJob.setSortByModifiedDateTime(this.sortByModifiedDateTime);
        fileEventDrivenJob.setSortAscending(this.sortAscending);
        fileEventDrivenJob.setDirectoryDepth(this.directoryDepth);
        fileEventDrivenJob.setLogMatchedFilenames(this.logMatchedFilenames);
        fileEventDrivenJob.setIgnoreFileRenameWhilstScanning(this.ignoreFileRenameWhilstScanning);
        fileEventDrivenJob.setMinFileAgeSeconds(this.minFileAgeSeconds);

        return fileEventDrivenJob;
    }
}
