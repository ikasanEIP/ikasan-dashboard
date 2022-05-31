package org.ikasan.notification.monitor;

import org.ikasan.job.orchestration.model.notification.Monitor;
import org.ikasan.job.orchestration.model.notification.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public abstract class AbstractMonitorBase<T> implements Monitor<T> {

    /** logger instance */
    private static Logger logger = LoggerFactory.getLogger(AbstractMonitorBase.class);

    // executor service for thread dispatching
    protected final ExecutorService executorService;

    /** list of notifiers to be informed */
    protected List<Notifier> notifiers = new ArrayList<>();

    /**
     * Constructor
     * @param executorService
     */
    public AbstractMonitorBase(ExecutorService executorService)
    {
        this.executorService = executorService;
        if(executorService == null)
        {
            throw new IllegalArgumentException("executorService cannot be 'null'");
        }
    }

    public void invoke(final T notification) {
        if (this.notifiers == null || this.notifiers.size() == 0) {
            logger.info("Monitor has no registered notifiers");
            return;
        }

        if (executorService == null || executorService.isShutdown()) {
            logger.warn("Cannot invoke Monitor after destroy has been called - executorService is null or shutdown");
            return;
        }

        for (final Notifier notifier: notifiers) {

            executorService.execute(() -> {
                try {
                        notifier.invoke(notification);
                    }
                    catch(RuntimeException e)
                    {
                        logger.warn("Failed to invoke notifier[" + notifier.getClass().getName() + "]", e);
                    }
            });
        }
    }



    @Override
    public void destroy()
    {
        if (executorService != null)
        {
            logger.info("Monitor shutting down executorService");
            executorService.shutdown();
        }
    }


    @Override
    public void setNotifiers(List<Notifier> notifiers) {
        this.notifiers = notifiers;
    }

    @Override
    public List<Notifier> getNotifiers()
    {
        return this.notifiers;
    }

    @Override
    public void addNotifier(Notifier notifier) {
        getNotifiers().add(notifier);
    }

}
