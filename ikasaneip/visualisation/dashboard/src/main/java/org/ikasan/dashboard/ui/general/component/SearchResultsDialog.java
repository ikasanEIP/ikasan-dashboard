package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ReplayService;
import org.ikasan.spec.module.client.ResubmissionService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;

import java.util.List;

public class SearchResultsDialog extends AbstractCloseableResizableDialog {

    private SearchResults searchResults;

    public SearchResultsDialog(ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService
        , HospitalAuditService hospitalAuditService, ResubmissionService resubmissionRestService
        , ReplayService replayRestService, ModuleMetaDataService moduleMetadataService, BatchInsert replayAuditService, DateFormatter dateFormatter
        , int maxDownloadBytes){
        searchResults = new SearchResults(esbSearchService, hospitalAuditService,
            resubmissionRestService, replayRestService, moduleMetadataService, replayAuditService, dateFormatter, maxDownloadBytes);

        searchResults.setSizeFull();

        HorizontalLayout wrapper = new HorizontalLayout();
        wrapper.setWidthFull();
        wrapper.setHeight("95%");
        wrapper.add(searchResults);
        this.content.add(wrapper);


        this.setSizeFull();
    }

    public void search(long startTime, long endTime, String searchTerm, String type, boolean negateQuery, String moduleName, String flowName) {
        this.searchResults.search(startTime, endTime, searchTerm, List.of(type), negateQuery, List.of(moduleName), List.of(flowName));
    }
}
