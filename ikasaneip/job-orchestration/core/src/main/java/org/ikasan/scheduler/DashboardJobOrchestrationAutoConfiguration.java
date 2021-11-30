package org.ikasan.scheduler;

import org.ikasan.scheduler.context.register.ContextInstanceSchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * House keeping related configuration required by dashboard.
 * This autoconfig should be excluded from dashboard.
 */
@Configuration
public class DashboardJobOrchestrationAutoConfiguration {


    @Bean
    public ContextInstanceSchedulerService schedulerNotificationSchedulerService(ScheduledContextService scheduledContextService,
                                                                                 ScheduledContextInstanceService scheduledContextInstanceService) {
        return new ContextInstanceSchedulerService(SchedulerFactory.getInstance().getScheduler()
            , CachingScheduledJobFactory.getInstance(), scheduledContextService, scheduledContextInstanceService);
    }
}