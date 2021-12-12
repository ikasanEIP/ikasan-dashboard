package org.ikasan.scheduler.core.event;

import org.ikasan.spec.scheduled.event.model.DryRunParameters;

public class DryRunParametersImpl implements DryRunParameters {

    private long minExecutionTimeMillis = 1000L;
    private long maxExecutionTimeMillis = 20000L;
    private long fixedExecutionTimeMillis = -1;
    private double jobErrorPercentage = 0.0;
    private boolean error = false;

    @Override
    public long getMinExecutionTimeMillis() {
        return minExecutionTimeMillis;
    }

    @Override
    public void setMinExecutionTimeMillis(long minExecutionTimeMillis) {
        this.minExecutionTimeMillis = minExecutionTimeMillis;
    }

    @Override
    public long getMaxExecutionTimeMillis() {
        return this.maxExecutionTimeMillis;
    }

    @Override
    public void setMaxExecutionTimeMillis(long maxExecutionTimeMillis) {
        this.maxExecutionTimeMillis = maxExecutionTimeMillis;
    }

    @Override
    public long getFixedExecutionTimeMillis() {
        return this.fixedExecutionTimeMillis;
    }

    @Override
    public void setFixedExecutionTimeMillis(long fixedExecutionTimeMillis) {
        this.fixedExecutionTimeMillis = fixedExecutionTimeMillis;
    }

    @Override
    public double getJobErrorPercentage() {
        return this.jobErrorPercentage;
    }

    @Override
    public void setJobErrorPercentage(double jobErrorPercentage) {
        this.jobErrorPercentage = jobErrorPercentage;
    }

    @Override
    public boolean isError() {
        return this.error;
    }

    @Override
    public void setError(boolean error) {
        this.error = error;
    }
}
