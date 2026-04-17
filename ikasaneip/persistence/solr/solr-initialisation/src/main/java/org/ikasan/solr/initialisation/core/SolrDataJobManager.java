package org.ikasan.solr.initialisation.core;

import jakarta.annotation.PostConstruct;
import org.ikasan.setup.model.DashboardPlatformSetup;
import org.ikasan.setup.model.DashboardSetupItem;
import org.ikasan.setup.model.SolrDashboardPlatformSetupImpl;
import org.ikasan.setup.model.SolrDashboardSetupItemImpl;
import org.ikasan.setup.service.SetupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrDataJobManager {
    private static final Logger logger = LoggerFactory.getLogger(SolrDataJobManager.class);

    private final SetupService setupService;
    private final List<SolrDataJob> solrDataJobs;

    /**
     * Constructs a new instance of {@code SolrDataJobManager}.
     * This constructor initializes the required services and job definitions for
     * managing and executing Solr-based data jobs.
     *
     * @param setupService the {@code SetupService} instance used for managing
     *                     the dashboard platform setup. Must not be {@code null}.
     * @param solrDataJobs a list of {@code SolrDataJob} instances representing
     *                     the jobs to be executed. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code setupService} or {@code solrDataJobs} is {@code null}.
     */
    public SolrDataJobManager(SetupService setupService, List<SolrDataJob> solrDataJobs) {
        this.setupService = setupService;
        if(this.setupService == null) {
            throw new IllegalArgumentException("setupService cannot be null");
        }
        this.solrDataJobs = solrDataJobs;
        if(this.solrDataJobs == null) {
            throw new IllegalArgumentException("solrDataJobs cannot be null");
        }
    }

    @PostConstruct
    public void execute() throws SolrDataJobException {
        DashboardPlatformSetup dashboardPlatformSetup = this.setupService.getDashboardPlatformSetup();

        for (SolrDataJob solrDataJob : solrDataJobs) {
            if(dashboardPlatformSetup == null || dashboardPlatformSetup.getPlatformSetupItems().stream()
                .noneMatch(item ->
                    item.getName() != null &&
                    item.getName().equals(solrDataJob.getJobName()) &&
                    item.getStatus() != null &&
                    item.getStatus().equals(SolrInitialDataJobStatusConstants.COMPLETE_SUCCESS))) {

                if(dashboardPlatformSetup == null) {
                    dashboardPlatformSetup = new SolrDashboardPlatformSetupImpl();
                }

                try {
                    solrDataJob.execute();
                }
                catch (SolrDataJobException e) {
                    logger.error("An error has occurred executing solr data job [{}]!", solrDataJob.getJobName(), e);
                    this.updateDashboardPlatformSetup(dashboardPlatformSetup,
                        solrDataJob.getJobName(), SolrInitialDataJobStatusConstants.ERROR);
                    throw e;
                }

                this.updateDashboardPlatformSetup(dashboardPlatformSetup,
                    solrDataJob.getJobName(), SolrInitialDataJobStatusConstants.COMPLETE_SUCCESS);
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
        DashboardSetupItem dashboardSetupItem = new SolrDashboardSetupItemImpl(jobName
            , status, System.currentTimeMillis());
        platformSetupItems.add(dashboardSetupItem);
        dashboardPlatformSetup.setPlatformSetupItems(platformSetupItems);
        this.setupService.save(dashboardPlatformSetup);
    }
}
