package org.ikasan.dashboard.ui.scheduler.view;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerJobLogFileViewerDialog;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;

//@Push
@Route(value = "schedulerJobLogFile")
@UIScope
@Component
@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@PermitAll
@PreserveOnRefresh
public class SchedulerJobLogFileView extends VerticalLayout implements BeforeEnterObserver, HasUrlParameter<String>
{
    Logger logger = LoggerFactory.getLogger(SchedulerJobLogFileView.class);

    @Resource(name = "moduleMetadataService")
    private ModuleMetaDataService moduleMetaDataService;

    @Resource
    private LogStreamingService logStreamingService;

    @Resource
    private SchedulerJobInstanceService schedulerJobInstanceService;

    private String uuid;
    private String childContextName;
    private String jobName;
    private boolean isErrorLog;

    private SchedulerJobLogFileViewerDialog schedulerJobLogFileViewerDialog;

    /**
     * Constructor
     */
    public SchedulerJobLogFileView() {
        this.setSpacing(false);
        this.setMargin(false);
    }

    /**
     * Initialise the internals of the object.
     */
    private void init() {

       SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(
            this.uuid, this.jobName, this.childContextName);

        ModuleMetaData agent = this.moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName());

        String host = agent.getUrl();
        String endPoint = "/rest/logs";

        String outputLog = isErrorLog ? schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent().getResultError() :
                                        schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent().getResultOutput();

        schedulerJobLogFileViewerDialog = new SchedulerJobLogFileViewerDialog(this.logStreamingService, host, endPoint, outputLog);
        schedulerJobLogFileViewerDialog.open();

        this.getStyle().set("padding-top", "0px");
        this.add(schedulerJobLogFileViewerDialog);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        init();
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, String param) {

        // uuid:childContextName:jobName:isErrorLog
        if(param.contains(":::")) {
            String[] tokens = param.split(":::");
            this.uuid = tokens[0];
            this.childContextName = tokens[1];
            this.jobName = tokens[2];
            this.isErrorLog = Boolean.parseBoolean(tokens[3]);
        }
    }
}

