package org.ikasan.dashboard.ui.scheduler.service;

import org.ikasan.business.stream.metadata.model.BusinessStreamMetaDataImpl;
import org.ikasan.dashboard.ui.scheduler.model.JobExecution;
import org.ikasan.dashboard.ui.scheduler.model.UpcomingJobExecutionSearchResults;
import org.ikasan.spec.metadata.BusinessStreamMetaData;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.stream.IntStream;

public class JobExecutionService {

    public UpcomingJobExecutionSearchResults getJobExecutions() {
        ArrayList<JobExecution> results = new ArrayList<>();

        ArrayList<BusinessStreamMetaData> businessStreamMetaDataList = new ArrayList();

        BusinessStreamMetaData businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setName("Fixed Income Trading");
        businessStreamMetaDataList.add(businessStreamMetaData);
        businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setName("Equity Trading");
        businessStreamMetaDataList.add(businessStreamMetaData);

        IntStream.range(0, 20).forEach(i -> {
            results.add(new JobExecution("Agent Name " + i,
                "Job Name " + i,
                "This is a description of the scheduled job. " + i,
                businessStreamMetaDataList,
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                "Scheduler Status " +i,
                "Success"));
        });

        return new UpcomingJobExecutionSearchResults(results, results.size(), 210L);
    }
}
