package org.ikasan.dashboard.ui.general.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import org.ikasan.dashboard.ui.search.component.SearchFilteringGrid;
import org.ikasan.dashboard.ui.search.model.hospital.ExclusionEventActionImpl;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.VaadinThreadFactory;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.hospital.model.ExclusionEventAction;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ResubmissionService;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HospitalDialog extends AbstractEntityViewDialog<IkasanESBDocument>
{
    private static final Logger logger = LoggerFactory.getLogger(HospitalDialog.class);

    private TextField moduleNameTf;
    private TextField flowNameTf;
    private TextField eventIdTf;
    private TextField errorUriTf;
    private TextField errorActionTf;
    private TextField dateTimeTf;

    private IkasanESBDocument errorOccurrence;
    private ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService;
    private String exclusionPayload;

    private Button resubmitButton;
    private Button ignoreButton;

    private HospitalAuditService hospitalAuditService;

    private IkasanESBDocument hospitalEvent;

    private Button downloadButton;

    private ResubmissionService resubmissionRestService;
    private ModuleMetaDataService moduleMetadataService;

    private String translatedEventActionMessage;

    private SearchFilteringGrid searchResultsGrid;

    private DateFormatter dateFormatter;

    public HospitalDialog(ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService, HospitalAuditService hospitalAuditService,
                          ResubmissionService resubmissionRestService, ModuleMetaDataService moduleMetadataService, SearchFilteringGrid searchResultsGrid,
                          DateFormatter dateFormatter)
    {
        this.esbSearchService = esbSearchService;
        if(this.esbSearchService == null)
        {
            throw new IllegalArgumentException("errorReportingService cannot be null!");
        }
        this.hospitalAuditService = hospitalAuditService;
        if(this.hospitalAuditService == null)
        {
            throw new IllegalArgumentException("hospitalAuditService cannot be null!");
        }
        this.resubmissionRestService = resubmissionRestService;
        if(this.resubmissionRestService == null)
        {
            throw new IllegalArgumentException("resubmissionRestService cannot be null!");
        }
        this.moduleMetadataService = moduleMetadataService;
        if(this.moduleMetadataService == null)
        {
            throw new IllegalArgumentException("moduleMetadataService cannot be null!");
        }
        this.searchResultsGrid = searchResultsGrid;
        if(this.searchResultsGrid == null)
        {
            throw new IllegalArgumentException("searchResultsGrid cannot be null!");
        }
        this.dateFormatter = dateFormatter;
        if(this.dateFormatter == null)
        {
            throw new IllegalArgumentException("dateFormatter cannot be null!");
        }

        moduleNameTf = new TextField(getTranslation("text-field.module-name", UI.getCurrent().getLocale(), null));
        flowNameTf = new TextField(getTranslation("text-field.flow-name", UI.getCurrent().getLocale(), null));
        eventIdTf = new TextField(getTranslation("text-field.event-id", UI.getCurrent().getLocale(), null));
        errorUriTf = new TextField(getTranslation("text-field.error-uri", UI.getCurrent().getLocale(), null));
        dateTimeTf = new TextField(getTranslation("text-field.date-time", UI.getCurrent().getLocale(), null));
        errorActionTf = new TextField(getTranslation("text-field.error-action", UI.getCurrent().getLocale(), null));

        translatedEventActionMessage = getTranslation("message.resubmission-event-action"
            , UI.getCurrent().getLocale());

        resubmitButton = new Button(getTranslation("button.resubmit", UI.getCurrent().getLocale(), null));
        ignoreButton = new Button(getTranslation("button.ignore", UI.getCurrent().getLocale(), null));
    }


    public Component getEntityDetailsLayout()
    {
        Image hospitalImage = new Image("/frontend/images/hospital-service.png", "");
        hospitalImage.setHeight("70px");

        H3 hospitalLabel = new H3(getTranslation("label.hospital-event-details", UI.getCurrent().getLocale(), null));

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setSpacing(true);
        headerLayout.add(hospitalImage, hospitalLabel);

        FormLayout formLayout = new FormLayout();

        moduleNameTf.setReadOnly(true);
        formLayout.add(moduleNameTf);

        flowNameTf.setReadOnly(true);
        formLayout.add(flowNameTf);

        eventIdTf.setReadOnly(true);
        formLayout.add(eventIdTf);

        errorUriTf.setReadOnly(true);
        formLayout.add(errorUriTf);

        errorActionTf.setReadOnly(true);
        formLayout.add(errorActionTf);

        dateTimeTf.setReadOnly(true);
        formLayout.add(dateTimeTf);

        formLayout.setSizeFull();

        downloadButton = new TableButton(VaadinIcon.DOWNLOAD.create());
        downloadButton.getElement().setAttribute("title"
            , getTranslation("tooltip.download-hospital-event", UI.getCurrent().getLocale()));

        Anchor downloadAnchor = new Anchor(DownloadHandler.fromInputStream(downloadEvent
            -> new DownloadResponse(new ByteArrayInputStream(super.aceEditor.getValue().getBytes())
            , "exclusion.txt",
            "plain/test", -1)), "");
        downloadAnchor.add(downloadButton);

        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        resubmitButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
        {
            final ExclusionEventAction exclusionEventAction = new ExclusionEventActionImpl();

            HospitalCommentsDialog commentsDialog = new HospitalCommentsDialog(exclusionEventAction, ExclusionEventAction.RESUBMIT);
            commentsDialog.open();

            commentsDialog.addOpenedChangeListener(dialogOpenedChangeEvent ->
            {
                if (!dialogOpenedChangeEvent.isOpened() && commentsDialog.isActioned())
                {
                    final UI current = UI.getCurrent();

                    ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(true);
                    progressIndicatorDialog.open(getTranslation("notification.re-submitting-hospital-event", UI.getCurrent().getLocale()), null);

                    this.actionExclusion("resubmit", ExclusionEventAction.RESUBMIT, exclusionEventAction.getComment()
                        , "error.exclusion-resubmission-error", "notification.hospital-event-resubmit-success"
                        , progressIndicatorDialog, current, authentication.getName());
                }
            });
        });


        ignoreButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
        {

            ExclusionEventActionImpl exclusionEventAction = new ExclusionEventActionImpl();

            HospitalCommentsDialog commentsDialog = new HospitalCommentsDialog(exclusionEventAction, ExclusionEventAction.IGNORED);
            commentsDialog.open();

            commentsDialog.addOpenedChangeListener(dialogOpenedChangeEvent ->
            {
                if (!dialogOpenedChangeEvent.isOpened() && commentsDialog.isActioned())
                {
                    final UI current = UI.getCurrent();

                    ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(true);
                    progressIndicatorDialog.open(String.format(getTranslation("notification.ignoring-hospital-event", UI.getCurrent().getLocale())), null);

                    this.actionExclusion("ignore", ExclusionEventAction.IGNORED, exclusionEventAction.getComment()
                        , "error.exclusion-ignore-error", "notification.hospital-event-ignore-success"
                        , progressIndicatorDialog, current, authentication.getName());
                }
            });
        });

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(resubmitButton, ignoreButton);

        ComponentSecurityVisibility.applySecurity(buttonLayout
            , SecurityConstants.EXCLUSION_ADMIN
            , SecurityConstants.EXCLUSION_WRITE
            , SecurityConstants.EXCLUSION_ALL_MODULES_WRITE
            , SecurityConstants.EXCLUSION_ALL_MODULES_ADMIN
            , SecurityConstants.ALL_AUTHORITY);


        Tab exclusionTab = new Tab(getTranslation("tab-label.exclusion", UI.getCurrent().getLocale(), null));
        Tab errorTab = new Tab(getTranslation("tab-label.error", UI.getCurrent().getLocale(), null));
        Tabs tabs = new Tabs(exclusionTab, errorTab);

        tabs.addSelectedChangeListener(event ->
        {
            // the tab's content is what the format selector's None restores (and the selected format applies to it)
            if(tabs.getSelectedTab().equals(exclusionTab))
            {
                super.showContent(exclusionPayload);
            }
            else
            {
                super.showContent(this.errorDetail());
            }
        });

        super.aceEditor.setHeight("100%");

        Button newWindowButton = new TableButton(VaadinIcon.EXTERNAL_LINK.create());
        newWindowButton.addClickListener(buttonClickEvent -> {
            EntityContentsViewDialog entityContentsViewDialog = new EntityContentsViewDialog("Exclusion " + hospitalEvent.getEventId());
            if(tabs.getSelectedTab().equals(exclusionTab)) {
                entityContentsViewDialog.populate(this.hospitalEvent);
            }
            else {
                entityContentsViewDialog.open(this.errorDetail());
            }
        });
        HorizontalLayout iconLayout = new HorizontalLayout();
        iconLayout.add(super.select, downloadAnchor, newWindowButton);
        iconLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, super.select);
        iconLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, downloadAnchor, newWindowButton);

        VerticalLayout layout = new VerticalLayout();
        layout.add(headerLayout, formLayout, iconLayout, buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, iconLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);

        layout.add(tabs);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.START, tabs);

        return layout;
    }

    @Override
    public void populate(IkasanESBDocument hospitalEvent)
    {
        this.hospitalEvent = hospitalEvent;

        super.title.setText("Exclusion " + hospitalEvent.getEventId());

        this.moduleNameTf.setValue(Optional.ofNullable(hospitalEvent.getModuleName()).orElse(""));
        this.flowNameTf.setValue(Optional.ofNullable(hospitalEvent.getFlowName()).orElse(""));
        this.eventIdTf.setValue(Optional.ofNullable(hospitalEvent.getEventId()).orElse(""));
        this.errorUriTf.setValue(Optional.ofNullable(this.getErrorUri(hospitalEvent.getId())).orElse(""));
        this.dateTimeTf.setValue(this.dateFormatter.getFormattedDate(hospitalEvent.getTimestamp()));

        this.errorOccurrence = this.esbSearchService
            .findByErrorUri("error", this.getErrorUri(hospitalEvent.getId()));
        this.exclusionPayload = hospitalEvent.getEvent();
        // the error the exclusion was made for may have expired or been removed: show the exclusion without it
        this.errorActionTf.setValue(this.errorOccurrence == null ? "" : Optional.ofNullable(this.errorOccurrence.getErrorAction()).orElse(""));

        ComponentSecurityVisibility.applySecurity(resubmitButton
            , SecurityConstants.EXCLUSION_WRITE
            , SecurityConstants.EXCLUSION_ADMIN
            , SecurityConstants.EXCLUSION_ALL_MODULES_WRITE
            , SecurityConstants.EXCLUSION_ALL_MODULES_ADMIN
            , SecurityConstants.ALL_AUTHORITY);
        ComponentSecurityVisibility.applySecurity(ignoreButton
            , SecurityConstants.EXCLUSION_WRITE
            , SecurityConstants.EXCLUSION_ADMIN
            , SecurityConstants.EXCLUSION_ALL_MODULES_WRITE
            , SecurityConstants.EXCLUSION_ALL_MODULES_ADMIN
            , SecurityConstants.ALL_AUTHORITY);

        super.open(hospitalEvent.getEvent());
    }

    /**
     * Helper method to get an initialised exclusion event action.
     *
     * @param comment
     * @param action
     * @param document
     * @param user
     * @return
     */
    protected ExclusionEventAction getExclusionEventAction(String comment, String action, IkasanESBDocument document, String user)
    {
        ExclusionEventAction exclusionEventAction = new ExclusionEventActionImpl();
        exclusionEventAction.setComment(comment);
        exclusionEventAction.setActionedBy(user);
        exclusionEventAction.setAction(String.format(translatedEventActionMessage, comment, action, user
            , this.dateFormatter.getFormattedDate(ZonedDateTime.now())
            , errorOccurrence != null ? errorOccurrence.getEvent() : document.getEvent()));
        // the error uri is in fact the id of excluded events
        exclusionEventAction.setErrorUri(document.getId());
        exclusionEventAction.setModuleName(document.getModuleName());
        exclusionEventAction.setFlowName(document.getFlowName());
        exclusionEventAction.setTimestamp(document.getTimestamp());
        exclusionEventAction.setEvent(document.getEvent());

        return exclusionEventAction;
    }

    private String getErrorUri(String id){
        if(id.contains(":")) {
            id = id.substring(id.lastIndexOf(":") + 1);
        }
        return id;
    }

    /** The error details of the error the exclusion was made for, or null when that error is not found. */
    private String errorDetail()
    {
        return this.errorOccurrence == null ? null : this.errorOccurrence.getErrorDetail();
    }

    /**
     * Ask the exclusion's module to resubmit or ignore it, on a background thread, and record the action. Whatever
     * happens the progress dialog is closed: the module may be unknown to the dashboard (no module metadata), refuse
     * the request, or something unexpected may fail.
     *
     * @param restAction     the action sent to the module: resubmit or ignore
     * @param recordedAction the action recorded: ExclusionEventAction.RESUBMIT or IGNORED
     * @param errorKey       message shown when the module does not action the exclusion
     * @param successKey     message shown when it does
     * @param userName       the user actioning the exclusion
     */
    private void actionExclusion(String restAction, String recordedAction, String comment, String errorKey, String successKey
        , ProgressIndicatorDialog progressIndicatorDialog, UI current, String userName)
    {
        Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("HospitalDialog"));
        executor.execute(() ->
        {
            try
            {
                ModuleMetaData moduleMetaData = this.moduleMetadataService.findById(hospitalEvent.getModuleName());
                if(moduleMetaData == null || moduleMetaData.getUrl() == null)
                {
                    logger.warn("Cannot {} exclusion [{}]: there is no module metadata (url) for module [{}]", restAction
                        , hospitalEvent.getId(), hospitalEvent.getModuleName());
                    current.access(() ->
                    {
                        progressIndicatorDialog.close();
                        NotificationHelper.showErrorNotification(String.format(getTranslation("error.exclusion-module-not-found"
                            , current.getLocale()), hospitalEvent.getModuleName()));
                    });
                    return;
                }

                boolean result = this.resubmissionRestService.resubmit(moduleMetaData.getUrl(), hospitalEvent.getModuleName(),
                    hospitalEvent.getFlowName(), restAction, this.getErrorUri(hospitalEvent.getId()), userName);

                if(!result)
                {
                    current.access(() ->
                    {
                        progressIndicatorDialog.close();
                        NotificationHelper.showErrorNotification(getTranslation(errorKey, current.getLocale()));
                    });
                    return;
                }

                ExclusionEventAction eventAction = this.getExclusionEventAction(comment, recordedAction,
                    this.hospitalEvent, userName);
                this.hospitalAuditService.save(eventAction);

                current.access(() ->
                {
                    resubmitButton.setVisible(false);
                    ignoreButton.setVisible(false);
                    progressIndicatorDialog.close();
                    this.searchResultsGrid.getDataProvider().refreshAll();
                    NotificationHelper.showUserNotification(getTranslation(successKey, current.getLocale()));
                    this.close();
                });
            }
            catch (RuntimeException e)
            {
                logger.error("An error occurred attempting to {} exclusion [{}]", restAction, hospitalEvent.getId(), e);
                current.access(() ->
                {
                    progressIndicatorDialog.close();
                    NotificationHelper.showErrorNotification(getTranslation(errorKey, current.getLocale()));
                });
            }
        });
    }
}
