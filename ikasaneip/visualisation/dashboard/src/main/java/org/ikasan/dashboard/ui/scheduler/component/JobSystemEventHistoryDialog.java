package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobSystemEventHistoryDialog extends ContextInstanceSystemEventHistoryDialog implements SchedulerJobStateChangeEventLocalBroadcastListener {
    private Logger logger = LoggerFactory.getLogger(JobSystemEventHistoryDialog.class);
    private SchedulerJobInstance schedulerJobInstance;
    private UI ui;

    public JobSystemEventHistoryDialog(ContextInstance contextInstance, SchedulerJobInstance schedulerJobInstance
        , SystemEventSearchService systemEventSearchService) {
        super(contextInstance, systemEventSearchService);
        this.schedulerJobInstance = schedulerJobInstance;
        super.filter.setSearchTerm(this.schedulerJobInstance.getJobName() + " " + this.contextInstance.getId());
        super.noSystemEventsMessage = getTranslation("paragraph.no-system-events-for-job", UI.getCurrent().getLocale());
    }
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();

        SchedulerJobStateChangeEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        logger.debug("Detaching ContextInstanceTreeView");
        this.ui = null;

        SchedulerJobStateChangeEventBroadcaster.unregister(this);

        logger.debug("Finished detaching ContextInstanceTreeView");
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent event) {
        if(event.getSchedulerJobInstance().getJobName().equals(this.schedulerJobInstance.getJobName()) &&
            event.getSchedulerJobInstance().getContextInstanceId().equals(this.schedulerJobInstance.getContextInstanceId())) {
            ui.access(() -> this.systemEventGrid.getDataProvider().refreshAll());
        }
    }
}
