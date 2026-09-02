package org.ikasan.persistence.initialisation.core;

public interface DataJob {

    /**
     * Retrieves the name of the job.
     *
     * @return the job name as a string.
     */
    String getJobName();

    /**
     * Executes the data job defined by the implementing class.
     * This method represents the core action or task to be performed
     * as part of a Solr-based data processing job.
     */
    void execute() throws DataJobException;
}
