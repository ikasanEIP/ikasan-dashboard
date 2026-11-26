package org.ikasan.dashboard.ui.search.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.search.component.SearchFilteringGrid;
import org.ikasan.dashboard.ui.search.model.hospital.ExclusionEventActionImpl;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.hospital.model.ExclusionEventAction;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ResubmissionService;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public abstract class HospitalEventActionListener extends IkasanEventActionListener {
    private Logger logger = LoggerFactory.getLogger(HospitalEventActionListener.class);

    private String translatedEventActionMessage;
    private ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService;
    private ResubmissionService resubmissionRestService;
    private IkasanAuthentication ikasanAuthentication;
    private DateFormatter dateFormatter;

    public HospitalEventActionListener(String translatedEventActionMessage, ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService,
                                       ModuleMetaDataService moduleMetadataService, ResubmissionService resubmissionRestService,
                                       SearchFilteringGrid searchResultsGrid, HashMap<String, Checkbox> selectionBoxes,
                                       HashMap<String, IkasanESBDocument> selectionItems, IkasanAuthentication ikasanAuthentication,
                                       DateFormatter dateFormatter) {
        super(moduleMetadataService, searchResultsGrid, selectionBoxes, selectionItems);
        this.translatedEventActionMessage = translatedEventActionMessage;
        if (this.translatedEventActionMessage == null) {
            throw new IllegalArgumentException("translatedEventActionMessage cannot be null!");
        }
        this.esbSearchService = esbSearchService;
        if (this.esbSearchService == null) {
            throw new IllegalArgumentException("solrGeneralService cannot be null!");
        }
        this.resubmissionRestService = resubmissionRestService;
        if (this.resubmissionRestService == null) {
            throw new IllegalArgumentException("resubmissionRestService cannot be null!");
        }
        this.ikasanAuthentication = ikasanAuthentication;
        if (this.ikasanAuthentication == null) {
            throw new IllegalArgumentException("ikasanAuthentication cannot be null!");
        }
        this.dateFormatter = dateFormatter;
        if (this.dateFormatter == null) {
            throw new IllegalArgumentException("dateFormatter cannot be null!");
        }
    }

    protected List<ExclusionEventAction> actionHospitalEvents(List<IkasanESBDocument> exclusionEvents, ExclusionEventAction exclusionEventAction, ProgressIndicatorDialog progressIndicatorDialog
        , String action, String username, UI current, Authentication authentication) throws JsonProcessingException {
        ExclusionEventAction eventAction;
        List<ExclusionEventAction> exclusionEventActions = new ArrayList<>();

        for (IkasanESBDocument document : exclusionEvents) {

            if (progressIndicatorDialog.isCancelled()) {
                break;
            }

            if (this.shouldActionEvent(document)) {
                ModuleMetaData moduleMetaData = super.getModuleMetaData(document.getModuleName());
                if (moduleMetaData == null || moduleMetaData.getUrl() == null) {
                    // the module is unknown to the dashboard, so it cannot be asked to action its exclusions
                    logger.warn("Cannot {} exclusion [{}]: there is no module metadata (url) for module [{}]", action
                        , document.getId(), document.getModuleName());
                    current.access(() ->
                    {
                        progressIndicatorDialog.cancel();
                        progressIndicatorDialog.close();
                        NotificationHelper.showErrorNotification(String.format(getTranslation("error.exclusion-module-not-found"
                            , current.getLocale()), document.getModuleName()));
                    });

                    return exclusionEventActions;
                }
                boolean result = this.resubmissionRestService.resubmit(moduleMetaData.getUrl(), document.getModuleName(),
                    document.getFlowName(), action, this.getErrorUri(document.getId()), authentication.getName());

                if (!result) {
                    current.access(() ->
                    {
                        progressIndicatorDialog.cancel();
                        progressIndicatorDialog.close();
                        if (action.equals("resubmit")) {
                            NotificationHelper.showErrorNotification(getTranslation("message.error-bulk-resubmit-exclusions", UI.getCurrent().getLocale()));
                        } else {
                            NotificationHelper.showErrorNotification(getTranslation("message.error-bulk-ignore-exclusions", UI.getCurrent().getLocale()));
                        }
                    });

                    return exclusionEventActions;
                }

                // record what was actually done to the event: the module was asked to resubmit or to ignore it
                eventAction = getExclusionEventAction(exclusionEventAction.getComment()
                    , action.equals("ignore") ? ExclusionEventActionImpl.IGNORED : ExclusionEventActionImpl.RESUBMIT
                    , document, username);


                exclusionEventActions.add(eventAction);

                logger.info("User[{{}]. Excluded event[{}]. Excluded event action[{}]. Comment[{}].", this.ikasanAuthentication.getName()
                    , document.getId(), action, exclusionEventAction.getComment());
            }
        }

        return exclusionEventActions;
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
    protected ExclusionEventAction getExclusionEventAction(String comment, String action, IkasanESBDocument document, String user) {
        IkasanESBDocument errorOccurrence = this.esbSearchService.findByErrorUri("error", this.getErrorUri(document.getId()));
        ExclusionEventAction exclusionEventAction = new ExclusionEventActionImpl();
        exclusionEventAction.setComment(comment);
        exclusionEventAction.setActionedBy(user);
        exclusionEventAction.setAction(String.format(translatedEventActionMessage, comment, action
            , user , this.dateFormatter.getFormattedDate(ZonedDateTime.now())
            // the error may have expired or been removed: record the excluded event itself then
            , errorOccurrence != null ? errorOccurrence.getEvent() : document.getEvent()));
        // the error uri is in fact the id of excluded events
        exclusionEventAction.setErrorUri(document.getId());
        exclusionEventAction.setModuleName(document.getModuleName());
        exclusionEventAction.setFlowName(document.getFlowName());
        exclusionEventAction.setTimestamp(document.getTimestamp());
        exclusionEventAction.setEvent(document.getEvent());

        return exclusionEventAction;
    }

    protected int getNumberOfSeletedItems() {
        int count = 0;
        for (Checkbox checkbox : super.selectionBoxes.values()) {
            if (checkbox.getValue()) {
                count++;
            }
        }

        return count;
    }

    public String getTranslation(String key, Locale locale) {
        I18NProvider provider = VaadinService.getCurrent().getInstantiator().getI18NProvider();

        if (provider != null) {
            return provider.getTranslation(key, locale);
        }

        return "";
    }

    private String getErrorUri(String id){
        if(id.contains(":")) {
            id = id.substring(id.lastIndexOf(":") + 1);
        }
        return id;
    }
}
