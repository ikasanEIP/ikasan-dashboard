package org.ikasan.housekeeping;

import org.ikasan.scheduler.CachingScheduledJobFactory;
import org.ikasan.scheduler.SchedulerFactory;
import org.ikasan.spec.housekeeping.HousekeepService;
import org.ikasan.spec.housekeeping.HousekeepingJob;
import org.ikasan.spec.housekeeping.HousekeepingSchedulerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * House keeping related configuration required by dashboard.
 * This autoconfig should be excluded from dashboard.
 */
@Configuration
public class DashboardHousekeepingAutoConfiguration
{

    @Bean
    public HousekeepingSchedulerService housekeepingSchedulerService(@Qualifier("persistenceHousekeepingJob") HousekeepingJob solrHousekeepingJob)
    {
        HousekeepingSchedulerService housekeepingSchedulerService =  new HousekeepingSchedulerServiceImpl(SchedulerFactory.getInstance().getScheduler(),
            CachingScheduledJobFactory.getInstance(), Arrays.asList(solrHousekeepingJob));

        housekeepingSchedulerService.startScheduler();
        return housekeepingSchedulerService;

    }

    @Bean(name = "persistenceHousekeepingJob")
    public HousekeepingJob housekeepingJob(@Qualifier("housekeepService") HousekeepService housekeepService, Environment environment)
    {
        return new HousekeepingJobImpl("persistenceHousekeepingJob", housekeepService, environment);
    }


}