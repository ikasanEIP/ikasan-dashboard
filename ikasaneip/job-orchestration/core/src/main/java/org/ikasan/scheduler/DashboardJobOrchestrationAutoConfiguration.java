package org.ikasan.scheduler;

import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import org.ikasan.scheduler.broker.InboundScheduledEventBroker;
import org.ikasan.scheduler.context.register.ContextInstanceSchedulerService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * House keeping related configuration required by dashboard.
 * This autoconfig should be excluded from dashboard.
 */
@Configuration
public class DashboardJobOrchestrationAutoConfiguration {

    @Bean
    public IBigQueue inboundQueue() throws IOException {
        String queueDir = "/sandbox/mick/bigquque";
        String queueName = "dashboard-inbound-queue";
        return new BigQueueImpl(queueDir, queueName);
    }

    @Bean
    public InboundScheduledEventBroker inboundScheduledEventBroker(IBigQueue inboundQueue) {
        return new InboundScheduledEventBroker(inboundQueue);
    }

    @Bean
    public ContextInstanceSchedulerService contextInstanceSchedulerService(ScheduledContextService scheduledContextService
        , ScheduledContextInstanceService scheduledContextInstanceService, SchedulerService schedulerService) {
        return new ContextInstanceSchedulerService(SchedulerFactory.getInstance().getScheduler()
            , CachingScheduledJobFactory.getInstance(), scheduledContextService, scheduledContextInstanceService, schedulerService);
    }
}