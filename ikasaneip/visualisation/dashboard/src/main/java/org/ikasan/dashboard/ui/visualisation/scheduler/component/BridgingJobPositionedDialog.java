package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.ErrorAcknowledgedPositionedDialog;
import org.ikasan.dashboard.ui.scheduler.component.JsonViewerDialog;
import org.ikasan.dashboard.ui.scheduler.component.LogFileHistoryDialog;
import org.ikasan.dashboard.ui.scheduler.component.TextViewerDialog;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.designer.PositionedDialog;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceSearchFilterImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class BridgingJobPositionedDialog extends PositionedDialog {
    private Logger logger = LoggerFactory.getLogger(BridgingJobPositionedDialog.class);
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;
    private LogStreamingService logStreamingService;
    private ModuleMetaDataService moduleMetaDataService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ContextInstance contextInstance;


    public BridgingJobPositionedDialog(SchedulerJobInstanceRecord schedulerJobInstanceRecord, LogStreamingService logStreamingService,
                                       ModuleMetaDataService moduleMetaDataService, SchedulerJobInstanceService schedulerJobInstanceService,
                                       ScheduledContextInstanceService scheduledContextInstanceService, ContextInstance contextInstance) {
        super(100, 250);
        this.schedulerJobInstanceRecord = schedulerJobInstanceRecord;
        this.logStreamingService = logStreamingService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.contextInstance = contextInstance;
        this.init();
    }

    private void init() {
        HorizontalLayout buttonLayout = new HorizontalLayout();

        if(this.schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.WAITING)) {
            Icon runJobIcon = IconDecorator.decorate(new Icon(VaadinIcon.PLAY), getTranslation("tooltip.execute-bridging-job"
                , UI.getCurrent().getLocale()), "14pt", IkasanColours.IKASAN_ORANGE);
            runJobIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                    try {
                        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
                        contextMachine.broadcastLocalEvent(this.createSchedulerJobInitiationEvent((BridgingJobInstance) this.schedulerJobInstanceRecord.getSchedulerJobInstance()
                            , contextMachine.getContext()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        NotificationHelper.showErrorNotification(getTranslation("error.execute-bridging-job"));
                    }
                    finally {
                        this.close();
                        NotificationHelper.showErrorNotification(getTranslation("message.execute-bridging-job"));
                    }
                }
            });
            buttonLayout.add(runJobIcon);
        }
        else if(this.schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE)) {
            Icon resetJobIcon = IconDecorator.decorate(new Icon(VaadinIcon.ARROW_BACKWARD), getTranslation("tooltip.reset-bridging-job"
                , UI.getCurrent().getLocale()), "14pt", IkasanColours.IKASAN_ORANGE);
            resetJobIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                    try {
                        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
                        contextMachine.resetJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(),
                            schedulerJobInstanceRecord.getChildContextName());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        NotificationHelper.showErrorNotification(getTranslation("error.reset-bridging-job"));
                    }
                    finally {
                        this.close();
                        NotificationHelper.showErrorNotification(getTranslation("message.reset-bridging-job"));
                    }
                }
            });
            buttonLayout.add(resetJobIcon);
        }


        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setWidth("100%");
        NativeLabel label = new NativeLabel(getTranslation("menu-item.bridging-job"));
        buttonLayout.getStyle().set("position", "absolute");
        buttonLayout.getStyle().set("right", "20px");
        verticalLayout.add(label, buttonLayout);
        super.add(verticalLayout);
    }

    private SchedulerJobInitiationEvent createSchedulerJobInitiationEvent(BridgingJobInstance bridgingJobInstance
        , ContextInstance contextInstance) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(bridgingJobInstance.getAgentName());
        schedulerJobInitiationEvent.setJobName(bridgingJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextName(contextInstance.getName());
        schedulerJobInitiationEvent.setContextInstanceId(contextInstance.getId());
        schedulerJobInitiationEvent.setChildContextNames(bridgingJobInstance.getChildContextNames());


        return schedulerJobInitiationEvent;
    }
}
