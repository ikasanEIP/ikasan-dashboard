package org.ikasan.persistence.initialisation.core;

import jakarta.annotation.PostConstruct;
import org.ikasan.persistence.initialisation.model.DashboardPlatformSetupImpl;
import org.ikasan.persistence.initialisation.model.DashboardSetupItemImpl;
import org.ikasan.spec.persistence.model.DashboardPlatformSetup;
import org.ikasan.spec.persistence.model.DashboardSetupItem;
import org.ikasan.spec.persistence.service.SetupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DataJobManager {
    private static final Logger logger = LoggerFactory.getLogger(DataJobManager.class);

    private final SetupService setupService;
    private final List<DataJob> dataJobs;

    /**
     * Constructs a new instance of {@code DataJobManager}.
     * This constructor initializes the required services and job definitions for
     * managing and executing Solr-based data jobs.
     *
     * @param setupService the {@code SetupService} instance used for managing
     *                     the dashboard platform setup. Must not be {@code null}.
     * @param dataJobs a list of {@code DataJob} instances representing
     *                     the jobs to be executed. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code setupService} or {@code dataJobs} is {@code null}.
     */
    public DataJobManager(SetupService setupService, List<DataJob> dataJobs) {
        this.setupService = setupService;
        if(this.setupService == null) {
            throw new IllegalArgumentException("setupService cannot be null");
        }
        this.dataJobs = dataJobs;
        if(this.dataJobs == null) {
            throw new IllegalArgumentException("dataJobs cannot be null");
        }
    }

    @PostConstruct
    public void execute() throws DataJobException {
        DashboardPlatformSetup dashboardPlatformSetup = this.setupService.getDashboardPlatformSetup();

        for (DataJob dataJob : dataJobs) {
            if(dashboardPlatformSetup == null || dashboardPlatformSetup.getPlatformSetupItems().stream()
                .noneMatch(item ->
                    item.getName() != null &&
                    item.getName().equals(dataJob.getJobName()) &&
                    item.getStatus() != null &&
                    item.getStatus().equals(InitialDataJobStatusConstants.COMPLETE_SUCCESS))) {

                if(dashboardPlatformSetup == null) {
                    dashboardPlatformSetup = new DashboardPlatformSetupImpl();
                }

                try {
                    dataJob.execute();
                }
                catch (DataJobException e) {
                    logger.error("An error has occurred executing solr data job [{}]!", dataJob.getJobName(), e);
                    this.updateDashboardPlatformSetup(dashboardPlatformSetup,
                        dataJob.getJobName(), InitialDataJobStatusConstants.ERROR);
                    throw e;
                }

                this.updateDashboardPlatformSetup(dashboardPlatformSetup,
                    dataJob.getJobName(), InitialDataJobStatusConstants.COMPLETE_SUCCESS);
            }
        }
    }

    /**
     * Updates the dashboard platform setup by adding a new platform setup item and saving the changes.
     *
     * @param dashboardPlatformSetup the {@code DashboardPlatformSetup} object containing the current
     *                                platform setup details. Must not be {@code null}.
     * @param jobName the name of the job to be added to the platform setup. Must not be {@code null}.
     * @param status the status of the job to be added to the platform setup. Must not be {@code null}.
     */
    private void updateDashboardPlatformSetup(DashboardPlatformSetup dashboardPlatformSetup
        , String jobName, String status) {
        List<DashboardSetupItem> platformSetupItems = dashboardPlatformSetup.getPlatformSetupItems();
        DashboardSetupItem dashboardSetupItem = new DashboardSetupItemImpl(jobName
            , status, System.currentTimeMillis());
        platformSetupItems.add(dashboardSetupItem);
        dashboardPlatformSetup.setPlatformSetupItems(platformSetupItems);
        this.setupService.save(dashboardPlatformSetup);
    }
}
