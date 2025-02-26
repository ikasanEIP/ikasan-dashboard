package org.ikasan.dashboard.ui.search.listener;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.general.component.ReplayCommentsDialog;
import org.ikasan.dashboard.ui.search.component.ReplayResultsDialog;
import org.ikasan.dashboard.ui.search.component.SolrSearchFilteringGrid;
import org.ikasan.dashboard.ui.search.model.replay.ReplayAuditEventImpl;
import org.ikasan.dashboard.ui.search.model.replay.ReplayAuditImpl;
import org.ikasan.dashboard.ui.search.model.replay.ReplayDialogDto;
import org.ikasan.dashboard.ui.util.VaadinThreadFactory;
import org.ikasan.rest.client.ReplayFailException;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ReplayService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.replay.ReplayAuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ReplayEventSubmissionListener extends IkasanEventActionListener implements ComponentEventListener<ClickEvent<Button>>
{
    Logger logger = LoggerFactory.getLogger(ReplayEventSubmissionListener.class);

    private ReplayService replayRestService;
    private BatchInsert replayAuditService;

    public ReplayEventSubmissionListener(ReplayService replayRestService, BatchInsert replayAuditService, ModuleMetaDataService moduleMetadataService, SolrSearchFilteringGrid searchResultsGrid
        , HashMap<String, Checkbox> selectionBoxes, HashMap<String, IkasanSolrDocument> selectionItems)
    {
        super(moduleMetadataService, searchResultsGrid, selectionBoxes, selectionItems);

        this.replayRestService = replayRestService;
        if(this.replayRestService == null)
        {
            throw new IllegalArgumentException("replayRestService cannot be null!");
        }
        this.replayAuditService = replayAuditService;
        if(this.replayAuditService == null)
        {
            throw new IllegalArgumentException("replayAuditService cannot be null!");
        }
    }

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent)
    {
        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        final UI current = UI.getCurrent();
        final I18NProvider i18NProvider = VaadinService.getCurrent().getInstantiator().getI18NProvider();

        if(!confirmSelectedEvents())
        {
            NotificationHelper.showErrorNotification(current.getTranslation("message.at-least-one-record-needs-to-be-selected"
                , UI.getCurrent().getLocale()));
            return;
        }

        ReplayDialogDto replayDialogDto = new ReplayDialogDto();
        replayDialogDto.setUser(authentication.getName());

        ReplayCommentsDialog commentsDialog = new ReplayCommentsDialog(replayDialogDto);
        commentsDialog.open();

        commentsDialog.addOpenedChangeListener(dialogOpenedChangeEvent ->
        {
            if(!dialogOpenedChangeEvent.isOpened() && commentsDialog.isSaved())
            {
                ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(true);

                if (selected)
                {
                    progressIndicatorDialog.open(String.format(i18NProvider.getTranslation("message.replay-number-of-events"
                        , current.getLocale()), searchResultsGrid.getResultSize()), null);
                }
                else
                {
                    progressIndicatorDialog.open(String.format(i18NProvider.getTranslation("message.replay-number-of-events"
                        , current.getLocale()), this.selectionItems.size()), null);
                }

                IkasanAuthentication ikasanAuthentication = (IkasanAuthentication)SecurityContextHolder.getContext().getAuthentication();

                Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ReplayEventSubmission"));
                executor.execute(() -> {
                    try
                    {
                        StringBuffer replayReport = new StringBuffer();
                        List<ReplayAuditEvent> replayAuditEvents = new ArrayList<>();

                        AtomicInteger replayCount = new AtomicInteger(0);

                        boolean containErrors = false;
                        if (!selected)
                        {
                            List<IkasanSolrDocument> replayEvents = this.selectionItems.values()
                                .stream()
                                .filter(document -> this.shouldActionEvent(document))
                                .collect(Collectors.toList());

                            replayEvents.sort(Comparator.comparingLong(IkasanSolrDocument::getTimestamp));

                            for (IkasanSolrDocument document : this.selectionItems.values())
                            {
                                logger.info("replaying [{}]", document.getEventId());

                                boolean result = false;

                                try {
                                    result = this.replayRestService.replay(replayDialogDto.getTargetServer(), replayDialogDto.getAuthenticationUser(),
                                        replayDialogDto.getPassword(), document.getModuleName(), document.getFlowName(), document.getPayloadRaw(),
                                        ikasanAuthentication.getName());
                                    replayReport.append(String.format("Event id [%s] successfully replayed to [%s]", document.getEventId(), replayDialogDto.getTargetServer())).append("\r\n");
                                }
                                catch (ReplayFailException e) {
                                    containErrors = true;
                                    replayReport.append(String.format("Error replaying event[%s]: [%s]", document.getEventId(), e.getMessage())).append("\r\n");
                                }

                                replayAuditEvents.add(createReplayAuditEvent(result, replayDialogDto, document, current, i18NProvider));
                                replayCount.getAndIncrement();
                            }
                        }
                        else
                        {

                            for (int i = 0; i < searchResultsGrid.getResultSize(); i += 100)
                            {
                                if (progressIndicatorDialog.isCancelled())
                                {
                                    return;
                                }

                                List<IkasanSolrDocument> docs = searchResultsGrid.getDataProvider().fetch
                                    (new Query<>(i, 100, List.of(new QuerySortOrder("timestamp", SortDirection.ASCENDING)), null, null)).collect(Collectors.toList());

                                for (IkasanSolrDocument document : docs)
                                {
                                    if (this.shouldActionEvent(document))
                                    {
                                        logger.info("replaying [{}]", document.getEventId());

                                        boolean result = false;

                                        try {
                                            result = this.replayRestService.replay(replayDialogDto.getTargetServer(), replayDialogDto.getAuthenticationUser(),
                                                replayDialogDto.getPassword(), document.getModuleName(), document.getFlowName(), document.getPayloadRaw(),
                                                ikasanAuthentication.getName());
                                            replayReport.append(String.format("Event id [%s] successfully replayed to [%s]", document.getEventId(), replayDialogDto.getTargetServer())).append("\r\n");
                                        }
                                        catch (ReplayFailException e) {
                                            containErrors = true;
                                            replayReport.append(String.format("Error replaying event[%s]: [%s]", document.getEventId(), e.getMessage())).append("\r\n");
                                        }

                                        replayAuditEvents.add(createReplayAuditEvent(result, replayDialogDto, document, current, i18NProvider));

                                        replayCount.getAndIncrement();
                                    }
                                }
                            }
                        }

                        this.replayAuditService.insert(replayAuditEvents);
                        boolean finalContainErrors = containErrors;
                        current.access(() ->
                        {
                            progressIndicatorDialog.close();
                            NotificationHelper.showUserNotification(String.format(i18NProvider.getTranslation("message.replay-complete"
                                , current.getLocale()), replayCount.get()));

                            ReplayResultsDialog replayResultsDialog = new ReplayResultsDialog(replayReport.toString(), finalContainErrors);
                            replayResultsDialog.open();
                        });
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        current.access(() ->
                        {
                            progressIndicatorDialog.close();
                            NotificationHelper.showErrorNotification(i18NProvider.getTranslation("message.replay-error"
                                , current.getLocale()));
                        });
                    }
                });
            }
        });
    }

    /**
     * Helper method to create replay audit events.
     *
     * @param result
     * @param replayDialogDto
     * @param document
     * @param current
     * @param i18NProvider
     * @return
     */
    private ReplayAuditEventImpl createReplayAuditEvent(boolean result, ReplayDialogDto replayDialogDto, IkasanSolrDocument document, UI current, I18NProvider i18NProvider)
    {
        ReplayAuditEventImpl replayAuditEvent = new ReplayAuditEventImpl();
        replayAuditEvent.setId(document.getId());
        replayAuditEvent.setReplayAudit(new ReplayAuditImpl(replayDialogDto.getUser(),
            replayDialogDto.getReplayReason(), replayDialogDto.getTargetServer(), System.currentTimeMillis()));
        if(result)
        {
            replayAuditEvent.setResultMessage(String.format(i18NProvider.getTranslation("message.replay-audit-success"
                , current.getLocale()), document.getId()));
        }
        else
        {
            replayAuditEvent.setResultMessage(String.format(i18NProvider.getTranslation("message.replay-audit-failure"
                , current.getLocale()), document.getId()));
        }
        replayAuditEvent.setTimestamp(System.currentTimeMillis());

        return replayAuditEvent;
    }
}
