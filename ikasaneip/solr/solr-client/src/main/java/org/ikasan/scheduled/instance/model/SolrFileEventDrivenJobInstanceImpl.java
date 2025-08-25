package org.ikasan.scheduled.instance.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.instance.model.FileEventDrivenJobInstance;
import org.ikasan.spec.scheduled.job.model.ReplacementPair;

import java.util.*;

public class SolrFileEventDrivenJobInstanceImpl extends SolrQuartzScheduleDrivenJobInstanceImpl implements FileEventDrivenJobInstance {
    private String filePath;

    private String moveDirectory;

    /** filenames to be processed */
    private List<String> filenames = new ArrayList<String>();

    /** encoding of the files */
    private String encoding;

    /** include header when processing files */
    private boolean includeHeader;

    /** include trailer when processing files */
    private boolean includeTrailer;

    /** sort based on the lastModifiedDateTime */
    private boolean sortByModifiedDateTime;

    /** sort ascending = true; descending = false */
    private boolean sortAscending = true;

    /** depth of the directory tree to walk */
    private int directoryDepth = 1;

    /** log filenames found */
    private boolean logMatchedFilenames = false;

    private boolean ignoreFileRenameWhilstScanning = true;

    private int minFileAgeSeconds;

    /** sla for file availability **/
    private String slaCronExpression;

    private boolean isDynamic;
    private String filePathSpel;
    private String filenameSpel;

    private Set<ReplacementPair> filenameReplacementPairs = new HashSet<>();
    private Set<ReplacementPair> filePathReplacementPairs = new HashSet<>();

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String getMoveDirectory() {
        return moveDirectory;
    }

    @Override
    public void setMoveDirectory(String moveDirectory) {
        this.moveDirectory = moveDirectory;
    }

    @Override
    public List<String> getFilenames() {
        return filenames;
    }

    @Override
    public void setFilenames(List<String> filenames) {
        this.filenames = filenames;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public boolean isIncludeHeader() {
        return includeHeader;
    }

    @Override
    public void setIncludeHeader(boolean includeHeader) {
        this.includeHeader = includeHeader;
    }

    @Override
    public boolean isIncludeTrailer() {
        return includeTrailer;
    }

    @Override
    public void setIncludeTrailer(boolean includeTrailer) {
        this.includeTrailer = includeTrailer;
    }

    @Override
    public boolean isSortByModifiedDateTime() {
        return sortByModifiedDateTime;
    }

    @Override
    public void setSortByModifiedDateTime(boolean sortByModifiedDateTime) {
        this.sortByModifiedDateTime = sortByModifiedDateTime;
    }

    @Override
    public boolean isSortAscending() {
        return sortAscending;
    }

    @Override
    public void setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
    }

    @Override
    public int getDirectoryDepth() {
        return directoryDepth;
    }

    @Override
    public void setDirectoryDepth(int directoryDepth) {
        this.directoryDepth = directoryDepth;
    }

    @Override
    public boolean isLogMatchedFilenames() {
        return logMatchedFilenames;
    }

    @Override
    public void setLogMatchedFilenames(boolean logMatchedFilenames) {
        this.logMatchedFilenames = logMatchedFilenames;
    }

    @Override
    public boolean isIgnoreFileRenameWhilstScanning() {
        return ignoreFileRenameWhilstScanning;
    }

    @Override
    public void setIgnoreFileRenameWhilstScanning(boolean ignoreFileRenameWhilstScanning) {
        this.ignoreFileRenameWhilstScanning = ignoreFileRenameWhilstScanning;
    }

    @Override
    public int getMinFileAgeSeconds() {
        return minFileAgeSeconds;
    }

    @Override
    public void setMinFileAgeSeconds(int minFileAgeSeconds) {
        this.minFileAgeSeconds = minFileAgeSeconds;
    }

    @Override
    public String getSlaCronExpression() {
        return slaCronExpression;
    }

    @Override
    public void setSlaCronExpression(String slaCronExpression) {
        this.slaCronExpression = slaCronExpression;
    }

    @Override
    public boolean isDynamic() {
        return isDynamic;
    }

    @Override
    public void setDynamic(boolean dynamic) {
        isDynamic = dynamic;
    }

    @Override
    public String getFilePathSpel() {
        return filePathSpel;
    }

    @Override
    public void setFilePathSpel(String filePathSpel) {
        this.filePathSpel = filePathSpel;
    }

    @Override
    public String getFilenameSpel() {
        return filenameSpel;
    }

    @Override
    public void setFilenameSpel(String filenameSpel) {
        this.filenameSpel = filenameSpel;
    }

    @Override
    public Set<ReplacementPair> getFilenameReplacementPairs() {
        return filenameReplacementPairs;
    }

    @Override
    public void setFilenameReplacementPairs(Set<ReplacementPair> filenameReplacementPairs) {
        this.filenameReplacementPairs = filenameReplacementPairs;
    }

    @Override
    public Set<ReplacementPair> getFilePathReplacementPairs() {
        return filePathReplacementPairs;
    }

    @Override
    public void setFilePathReplacementPairs(Set<ReplacementPair> filePathReplacementPairs) {
        this.filePathReplacementPairs = filePathReplacementPairs;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileEventDrivenJobInstance)) return false;
        if (!super.equals(o)) return false;
        FileEventDrivenJobInstance that = (FileEventDrivenJobInstance) o;
        return Objects.equals(super.jobName, that.getJobName())
            && Objects.equals(super.contextName, that.getContextName())
            && Objects.equals(super.getChildContextName(), that.getChildContextName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.jobName, super.contextName, super.getChildContextName());
    }
}
