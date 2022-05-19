package org.ikasan.dashboard.ui.scheduler.component.filter;

import org.ikasan.spec.scheduled.job.model.SchedulerJobSearchFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchedulerJobSearchFilterImpl implements SchedulerJobSearchFilter {
    public static Map<String, String> JOB_TYPE_MAPPINGS = new HashMap<>();
    public static Map<String, String> JOB_TYPE_MAPPINGS_INVERTED = new HashMap<>();

    static {
        JOB_TYPE_MAPPINGS.put("Scheduled Job", "quartzScheduleDrivenJob");
        JOB_TYPE_MAPPINGS.put("File Watcher Job", "fileEventDrivenJob");
        JOB_TYPE_MAPPINGS.put("Command Execution Job", "internalEventDrivenJob");

        JOB_TYPE_MAPPINGS_INVERTED.put("quartzScheduleDrivenJob", "Scheduled Job");
        JOB_TYPE_MAPPINGS_INVERTED.put("fileEventDrivenJob", "File Watcher Job");
        JOB_TYPE_MAPPINGS_INVERTED.put("internalEventDrivenJob", "Command Execution Job");
    }

    private String jobNameFilter = null;
    private String jobTypeFilter = null;
    private String contextSearchFilter = null;

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
}
