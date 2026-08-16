package org.ikasan.dashboard.ui.general.component;


import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.rest.client.ReplayRestServiceImpl;
import org.ikasan.rest.client.ResubmissionRestServiceImpl;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@PreserveOnRefresh
@Route(value = "eventLifeId")
@UIScope
@Component
public class EventLifeIdDeepLinkView extends VerticalLayout implements HasUrlParameter<String>
{
    Logger logger = LoggerFactory.getLogger(EventLifeIdDeepLinkView.class);

    @Value("${max.download.bytes:50000000}")
    private int maxDownloadBytes;

    private SearchResults searchResults;

    public EventLifeIdDeepLinkView(@Qualifier("moduleMetadataService") ModuleMetaDataService moduleMetadataService,
                                   ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService,
                                   @Qualifier("hospitalEntityService") HospitalAuditService hospitalAuditService, ResubmissionRestServiceImpl resubmissionRestService,
                                   ReplayRestServiceImpl replayRestService, @Qualifier("replayAuditService") BatchInsert replayAuditService, DateFormatter dateFormatter)
    {
        this.searchResults = new SearchResults(esbSearchService, hospitalAuditService, resubmissionRestService
            , replayRestService, moduleMetadataService, replayAuditService, dateFormatter, this.maxDownloadBytes);
        this.searchResults.setSizeFull();

        this.add(searchResults);
        this.setSizeFull();
    }


    @Override
    public void setParameter(BeforeEvent beforeEvent, String parameter) {
        logger.info(String.format("Deep link event life identifier [%s]", parameter));

        this.searchResults.search(0, System.currentTimeMillis(), parameter, List.of("wiretap", "error", "exclusion"),
            false, new ArrayList<>(), new ArrayList<>());
    }

}
