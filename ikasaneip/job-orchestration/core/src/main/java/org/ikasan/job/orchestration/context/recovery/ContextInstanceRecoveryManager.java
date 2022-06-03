package org.ikasan.job.orchestration.context.recovery;

import javax.annotation.PostConstruct;

import org.ikasan.spec.scheduled.context.service.ContextInstanceRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextInstanceRecoveryManager {
    private static Logger logger = LoggerFactory.getLogger(ContextInstanceRecoveryManager.class);

    private final boolean isContextLifeCycleActive;

    private final ContextInstanceRecoveryService contextInstanceRecoveryService;

    public ContextInstanceRecoveryManager(ContextInstanceRecoveryService contextInstanceRecoveryService, boolean isContextLifeCycleActive) {

        this.contextInstanceRecoveryService = contextInstanceRecoveryService;
        if (this.contextInstanceRecoveryService == null) {
            throw new IllegalArgumentException("contextInstanceRecoveryService cannot be null!");
        }

        this.isContextLifeCycleActive = isContextLifeCycleActive;
    }

    @PostConstruct
    public void recoverContextInstances() {
        // NOTE: This executes before ContextInstanceSchedulerService
        logger.info("Recovering context instances!");
        if (!isContextLifeCycleActive) {
            logger.info("ContextInstanceRecoveryManager not running as usePostConstructs is false");
            return;
        }

        contextInstanceRecoveryService.recoverInstances();
    }
}