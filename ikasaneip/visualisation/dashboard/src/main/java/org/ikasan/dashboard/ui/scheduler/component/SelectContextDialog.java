package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobSchedulerVisualisation;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SelectContextDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(SelectContextDialog.class);

    // Fields to capture schedule job properties.

    private ContextTemplate contextTemplate;
    private ContextTemplate parentContextTemplate;

    private SystemEventLogger systemEventLogger;



    /**
     * Dialog used for selecting a context to link to a downstream child job plan.
     *
     * @param systemEventLogger        The system event logger used for logging events.
     * @param parentContextTemplate    The parent context template.
     * @param contextTemplate          The current context template.
     * @param jobSchedulerVisualisation The job scheduler visualisation.
     */
    public SelectContextDialog(SystemEventLogger systemEventLogger, ContextTemplate parentContextTemplate, ContextTemplate contextTemplate,
                               JobSchedulerVisualisation jobSchedulerVisualisation) {
        super.showResize(false);
        // todo translation
        super.title.setText("Link to Downstream Child Job Plan");
        this.systemEventLogger = systemEventLogger;
        this.parentContextTemplate = parentContextTemplate;
        this.contextTemplate = contextTemplate;

        this.setHeight("120px");
        this.setWidth("600px");


        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();

        Map<String, Context> contextMap = ContextHelper.getAllContexts(this.parentContextTemplate);

        ComboBox<String> searchCb = new ComboBox<>();
        searchCb.setPlaceholder(getTranslation("label.search-job-plan-link", UI.getCurrent().getLocale()));
        List<String> childJobPlanNames = contextMap.keySet().stream().collect(Collectors.toList());
        Collections.sort(childJobPlanNames);
        searchCb.setItems(childJobPlanNames);
        searchCb.setWidth("500px");

        Button linkButton = new Button("Link");
        linkButton.addClickListener(buttonClickEvent -> {
            ContextTemplate contextToLinkTo = (ContextTemplate) contextMap.get(searchCb.getValue());
            Optional<ContextStartJob> contextStartJob = ContextHelper.getContextStartJobFromContext(contextToLinkTo);
            Optional<ContextTerminalJob> contextTerminalJob = ContextHelper.getContextTerminalJobFromContext(contextTemplate);

            if(!contextStartJob.isPresent()) {
                NotificationHelper.showUserNotification("The child job plan you are attempting link to does not contain a start job.");
            }
            else {
                try {
                    jobSchedulerVisualisation.linkToContext(contextTerminalJob.get(), contextStartJob.get(), contextToLinkTo);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        layout.add(searchCb, linkButton);

        super.content.add(layout);
    }


}
