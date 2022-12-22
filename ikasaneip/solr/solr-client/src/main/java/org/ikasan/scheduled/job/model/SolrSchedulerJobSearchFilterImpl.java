package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.job.model.SchedulerJobSearchFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrSchedulerJobSearchFilterImpl implements SchedulerJobSearchFilter {
    public static Map<String, String> JOB_TYPE_MAPPINGS = new HashMap<>();
    public static Map<String, String> JOB_TYPE_MAPPINGS_INVERTED = new HashMap<>();

    static {
        JOB_TYPE_MAPPINGS.put("Scheduled Job", "quartzScheduleDrivenJob");
        JOB_TYPE_MAPPINGS.put("File Watcher Job", "fileEventDrivenJob");
        JOB_TYPE_MAPPINGS.put("Command Execution Job", "internalEventDrivenJob");
        JOB_TYPE_MAPPINGS.put("Global Event Job", "globalEventJob");

        JOB_TYPE_MAPPINGS_INVERTED.put("quartzScheduleDrivenJob", "Scheduled Job");
        JOB_TYPE_MAPPINGS_INVERTED.put("fileEventDrivenJob", "File Watcher Job");
        JOB_TYPE_MAPPINGS_INVERTED.put("internalEventDrivenJob", "Command Execution Job");
        JOB_TYPE_MAPPINGS_INVERTED.put("globalEventJob", "Global Event Job");
    }

    private String jobNameFilter = null;
    private String jobTypeFilter = null;
    private String contextSearchFilter = null;
    private boolean held;
    private boolean skipped;
    private Boolean targetResidingContextOnly;
    private Boolean participatesInLock = null;

    public String getJobNameFilter()
    {
        return jobNameFilter;
    }

    public void setJobNameFilter(String jobNameFilter)
    {
        this.jobNameFilter = jobNameFilter;
    }

    public String getJobTypeFilter() {
        return jobTypeFilter;
    }

    public void setJobTypeFilter(String jobTypeFilter) {
        this.jobTypeFilter = jobTypeFilter;
    }

    public String getContextSearchFilter() {
        return contextSearchFilter;
    }

    public void setContextSearchFilter(String contextSearchFilter) {
        this.contextSearchFilter = contextSearchFilter;
    }

    public List<String> getTobTypes() {
        return new ArrayList<>(JOB_TYPE_MAPPINGS.keySet());
    }

    @Override
    public boolean isHeld() {
        return held;
    }

    @Override
    public void setHeld(boolean held) {
        this.held = held;
    }

    @Override
    public boolean isSkipped() {
        return skipped;
    }

    @Override
    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    @Override
    public Boolean isTargetResidingContextOnly() {
        return targetResidingContextOnly;
    }


    @Override
    public void setTargetResidingContextOnly(Boolean targetResidingContextOnly) {
        this.targetResidingContextOnly = targetResidingContextOnly;
    }

    @Override
    public Boolean isParticipatesInLock() {
        return participatesInLock;
    }

    @Override
    public void setParticipatesInLock(Boolean participatesInLock) {
        this.participatesInLock = participatesInLock;
    }

    @Override
    public void setStatus(String status) {
        if(status == null || status.isEmpty()) {
            this.held = false;
            this.skipped = false;
        }
        else if(status.equals(InstanceStatus.ON_HOLD.name())) {
            this.held = true;
            this.skipped = false;
        }
        else if(status.equals(InstanceStatus.SKIPPED.name())) {
            this.held = false;
            this.skipped = true;
        }
    }
}
