package org.ikasan.scheduler;

import org.ikasan.scheduler.CachingScheduledJobFactory;
import org.ikasan.scheduler.SchedulerFactory;
import org.ikasan.spec.housekeeping.HousekeepService;
import org.ikasan.spec.housekeeping.HousekeepingJob;
import org.ikasan.spec.housekeeping.HousekeepingSchedulerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

/**
 * House keeping related configuration required by dashboard.
 * This autoconfig should be excluded from dashboard.
 */
@Configuration
public class DashboardJobOrchestrationAutoConfiguration {

}