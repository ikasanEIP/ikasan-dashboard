package org.ikasan.dashboard.ui.search.listener;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import org.ikasan.dashboard.ui.general.component.HospitalCommentsDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.general.component.SearchResults;
import org.ikasan.dashboard.ui.search.component.SolrSearchFilteringGrid;
import org.ikasan.dashboard.ui.search.model.hospital.ExclusionEventActionImpl;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.VaadinThreadFactory;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.hospital.model.ExclusionEventAction;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ResubmissionService;
import org.ikasan.spec.solr.SolrGeneralService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ResubmitHospitalEventSubmissionListener extends HospitalEventActionListener implements ComponentEventListener<ClickEvent<Button>> {
    private Logger logger = LoggerFactory.getLogger(ResubmitHospitalEventSubmissionListener.class);

    private HospitalAuditService hospitalAuditService;

    private boolean success = false;

    private SearchResults searchResults;

    public ResubmitHospitalEventSubmissionListener(HospitalAuditService hospitalAuditService, ResubmissionService resubmissionRestService
        , ModuleMetaDataService moduleMetadataService, SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService
        , String actionMessage, SolrSearchFilteringGrid searchResultsGrid, HashMap<String, Checkbox> selectionBoxes
        , HashMap<String, IkasanSolrDocument> selectionItems, IkasanAuthentication ikasanAuthentication, DateFormatter dateFormatter, SearchResults searchResults) {
        super(actionMessage, solrGeneralService, moduleMetadataService, resubmissionRestService
            , searchResultsGrid, selectionBoxes, selectionItems, ikasanAuthentication, dateFormatter);
        this.hospitalAuditService = hospitalAuditService;
        if (this.hospitalAuditService == null) {
            throw new IllegalArgumentException("hospitalAuditService cannot be null!");
        }
        this.searchResults = searchResults;
        if (this.searchResults == null) {
            throw new IllegalArgumentException("searchResults cannot be null!");
        }
    }

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (!confirmSelectedEvents()) {
            NotificationHelper.showErrorNotification(getTranslation("message.at-least-one-record-needs-to-be-selected", UI.getCurrent().getLocale()));
            return;
        }

        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        ExclusionEventActionImpl exclusionEventAction = new ExclusionEventActionImpl();

        HospitalCommentsDialog commentsDialog = new HospitalCommentsDialog(exclusionEventAction, ExclusionEventAction.RESUBMIT);
        commentsDialog.open();

        commentsDialog.addOpenedChangeListener(dialogOpenedChangeEvent ->
        {
            if (!dialogOpenedChangeEvent.isOpened() && commentsDialog.isActioned()) {
                ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(true);

                if (selected) {
                    progressIndicatorDialog.open(String.format(String.format(getTranslation("message.resubmitting-exclusions", UI.getCurrent().getLocale())
                        , searchResultsGrid.getResultSize())), null);
                } else {
                    progressIndicatorDialog.open(String.format(String.format(getTranslation("message.resubmitting-exclusions", UI.getCurrent().getLocale())
                        , super.getNumberOfSeletedItems())), null);
                }

                final UI current = UI.getCurrent();
                Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ResubmitHospitalEvent"));
                executor.execute(() -> {
                    try {
                        this.success = false;
                        List<ExclusionEventAction> exclusionEventActions = null;

                        AtomicInteger resubmitCount = new AtomicInteger(0);

                        if (!selected) {
                            List<IkasanSolrDocument> resubmissionEvents = this.selectionItems.values()
                                .stream()
                                .filter(document -> this.shouldActionEvent(document))
                                .collect(Collectors.toList());

                            resubmissionEvents.sort(Comparator.comparingLong(IkasanSolrDocument::getTimestamp));

                            resubmitCount.set(resubmissionEvents.size());

                            exclusionEventActions = super.actionHospitalEvents(resubmissionEvents, exclusionEventAction, progressIndicatorDialog,
                                "resubmit", authentication.getName(), current, authentication);

                            if (exclusionEventActions.size() > 0) {
                                hospitalAuditService.save(exclusionEventActions);
                            }
                        } else {
                            long resultSize = searchResultsGrid.getResultSize();

                            for (int i = 0; i < resultSize; i += 100) {
                                if (progressIndicatorDialog.isCancelled()) {
                                    break;
                                }

                                List<IkasanSolrDocument> docs = searchResultsGrid.getDataProvider()
                                    .fetch(new Query<>(0, 100, List.of(new QuerySortOrder("timestamp", SortDirection.ASCENDING)), null, null))
                                    .collect(Collectors.toList());

                                List<IkasanSolrDocument> resubmissionEvents = docs
                                    .stream()
                                    .filter(document -> this.shouldActionEvent(document))
                                    .collect(Collectors.toList());

                                resubmitCount.set(resubmitCount.get()+ resubmissionEvents.size());

                                exclusionEventActions = super.actionHospitalEvents(resubmissionEvents, exclusionEventAction, progressIndicatorDialog,
                                    "resubmit", authentication.getName(), current, authentication);

                                if (exclusionEventActions.size() > 0) {
                                    hospitalAuditService.save(exclusionEventActions);
                                }
                            }
                        }

                        if (progressIndicatorDialog.isOpened()) {
                            current.access(() ->
                            {
                                progressIndicatorDialog.close();
                                NotificationHelper.showUserNotification(String.format(getTranslation("message.successfully-resubmitted-exclusions", UI.getCurrent().getLocale()), resubmitCount.get()));
                                selectionBoxes.keySet().forEach(key -> selectionBoxes.get(key).setValue(false));
                                selectionItems.clear();
                            });
                        }

                        current.access(() -> this.searchResultsGrid.getDataProvider().refreshAll());
                        this.success = true;
                        if(this.selected) {
                            searchResults.toggleSelected();
                        }
                    }
                    catch (Exception e) {
                        logger.error("An error has occurred managing hospital events!", e);
                        current.access(() ->
                        {
                            this.searchResultsGrid.getDataProvider().refreshAll();
                            progressIndicatorDialog.close();
                            NotificationHelper.showErrorNotification(getTranslation("message.error-bulk-resubmit-exclusions", UI.getCurrent().getLocale()));
                        });
                    }
                });
            }
        });
    }

    public boolean isSuccess() {
        return success;
    }
}
