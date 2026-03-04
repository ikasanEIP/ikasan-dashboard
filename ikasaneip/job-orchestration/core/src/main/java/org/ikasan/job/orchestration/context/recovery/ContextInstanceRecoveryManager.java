package org.ikasan.job.orchestration.context.recovery;

import org.ikasan.spec.scheduled.context.service.ContextInstanceRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextInstanceRecoveryManager {
    private static Logger logger = LoggerFactory.getLogger(ContextInstanceRecoveryManager.class);

    private final boolean isContextLifeCycleActive;

    private final boolean isIkasanEnterpriseSchedulerInstance;

    private final ContextInstanceRecoveryService contextInstanceRecoveryService;

    /**
     * Constructor for ContextInstanceRecoveryManager.
     *
     * @param contextInstanceRecoveryService the service responsible for recovering context instances
     * @param isContextLifeCycleActive flag indicating if the context lifecycle is active
     * @param isIkasanEnterpriseSchedulerInstance flag indicating if the instance is an Ikasan Enterprise Scheduler instance
     */
    public ContextInstanceRecoveryManager(ContextInstanceRecoveryService contextInstanceRecoveryService, boolean isContextLifeCycleActive,
                                          boolean isIkasanEnterpriseSchedulerInstance) {

        this.contextInstanceRecoveryService = contextInstanceRecoveryService;
        if (this.contextInstanceRecoveryService == null) {
            throw new IllegalArgumentException("contextInstanceRecoveryService cannot be null!");
        }

        this.isContextLifeCycleActive = isContextLifeCycleActive;
        this.isIkasanEnterpriseSchedulerInstance = isIkasanEnterpriseSchedulerInstance;
    }

    /**
     * Method to recover context instances. This method is designed to be called before the execution of
     * {@link ContextInstanceSchedulerServiceImpl}.
     *
     * The method first checks if the current instance is an Ikasan Enterprise Scheduler instance and if the
     * context lifecycle is active. If any of these conditions are not met, the method will log an info message
     * and return without further processing.
     *
     * If the conditions are met, the method will attempt to recover instances using the provided
     * {@link ContextInstanceRecoveryService}. Any exceptions that occur during the recovery process will be
     * caught and logged as an error message.
     */
    public void recoverContextInstances() {
        // NOTE: This executes before ContextInstanceSchedulerServiceImpl
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