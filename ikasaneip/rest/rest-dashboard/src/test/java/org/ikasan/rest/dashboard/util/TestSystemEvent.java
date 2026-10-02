package org.ikasan.rest.dashboard.util;

import org.ikasan.spec.systemevent.SystemEvent;

import java.util.Date;

/**
 * Test implementation of SystemEvent for unit testing.
 */
public class TestSystemEvent implements SystemEvent {

    private Long id;
    private String moduleName;
    private String action;
    private String actor;
    private String subject;
    private Date timestamp;
    private Date expiry;

    public TestSystemEvent() {
    }

    public TestSystemEvent(Long id, String moduleName, String action, String actor, String subject) {
        this.id = id;
        this.moduleName = moduleName;
        this.action = action;
        this.actor = actor;
        this.subject = subject;
        this.timestamp = new Date();
        this.expiry = new Date(System.currentTimeMillis() + 86400000); // 1 day from now
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public Date getExpiry() {
        return expiry;
    }

    public void setExpiry(Date expiry) {
        this.expiry = expiry;
    }
}
