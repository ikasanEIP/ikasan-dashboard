package org.ikasan.job.orchestration.context.recovery;

import javax.annotation.PostConstruct;

import org.ikasan.spec.scheduled.context.service.ContextInstanceRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextInstanceRecoveryManager {
    private static Logger logger = LoggerFactory.getLogger(ContextInstanceRecoveryManager.class);

    private final boolean isContextLifeCycleActive;

    private final boolean isIkasanEnterpriseSchedulerInstance;

    private final ContextInstanceRecoveryService contextInstanceRecoveryService;

    public ContextInstanceRecoveryManager(ContextInstanceRecoveryService contextInstanceRecoveryService, boolean isContextLifeCycleActive,
                                          boolean isIkasanEnterpriseSchedulerInstance) {

        this.contextInstanceRecoveryService = contextInstanceRecoveryService;
        if (this.contextInstanceRecoveryService == null) {
            throw new IllegalArgumentException("contextInstanceRecoveryService cannot be null!");
        }

        this.isContextLifeCycleActive = isContextLifeCycleActive;
        this.isIkasanEnterpriseSchedulerInstance = isIkasanEnterpriseSchedulerInstance;
    }

    @PostConstruct
    public void recoverContextInstances() {
        // NOTE: This executes before ContextInstanceSchedulerService
        logger.info("Recovering context instances!");
        if (!isIkasanEnterpriseSchedulerInstance) {
            logger.info("ContextInstanceRecoveryManager not running as an Ikasan Enterprise Scheduler instance");
            return;
        }
        if (!isContextLifeCycleActive) {
            logger.info("ContextInstanceRecoveryManager not running as context lifecycle is false");
            return;
        }

        try {
            contextInstanceRecoveryService.recoverInstances();
        } catch (Exception e) {
            // todo need to add some notifications here
            logger.error(String.format("An exception has occurred recovering contexts [%s]", e.getMessage()), e);
        }
    }
}