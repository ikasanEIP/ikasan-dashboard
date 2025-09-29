package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.administration.component.SystemEventDialog;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerJobLogFileViewerDialog;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.ikasan.systemevent.model.SolrSystemEvent;
import org.ikasan.systemevent.model.SolrSystemEventSearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class JobSystemEventHistoryDialog extends ContextInstanceSystemEventHistoryDialog implements SchedulerJobStateChangeEventBroadcastListener {
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
