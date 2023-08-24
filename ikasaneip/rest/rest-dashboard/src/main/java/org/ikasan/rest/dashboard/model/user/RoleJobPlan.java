package org.ikasan.rest.dashboard.model.user;

import java.io.Serializable;
import java.util.Date;

public class RoleJobPlan implements Serializable
{
    private Long id;
    private String jobPlanName;
    private Role role;

    /**
     * The data time stamp when an instance was first created
     */
    private Date createdDateTime;

    /**
     * The data time stamp when an instance was last updated
     */
    private Date updatedDateTime;

    public RoleJobPlan()
    {
        long now = System.currentTimeMillis();
        this.createdDateTime = new Date(now);
        this.updatedDateTime = new Date(now);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobPlanName() {
        return jobPlanName;
    }

    public void setJobPlanName(String jobPlanName) {
        this.jobPlanName = jobPlanName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public Date getUpdatedDateTime() {
        return updatedDateTime;
    }

    public void setUpdatedDateTime(Date updatedDateTime) {
        this.updatedDateTime = updatedDateTime;
    }
}
