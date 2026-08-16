package org.ikasan.dashboard.notification.scheduler.service;

import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SchedulerNotificationService {
    private ESBSearchService<IkasanSolrDocument, IkasanDocumentSearchResults> esbSearchService;

    public SchedulerNotificationService(ESBSearchService esbSearchService) {
        this.esbSearchService = esbSearchService;
    }

    public Optional<List<IkasanESBDocument>> getFailedScheduledJobs(String agentName, Long startTimestamp, Integer resultSize) {

        IkasanDocumentSearchResults results = this.esbSearchService.search(Set.of(agentName), Set.of()
            , "returnCode:0", startTimestamp, System.currentTimeMillis(), resultSize, List.of("scheduledProcessEvent")
            ,true, null, null);

        if(results.getTotalNumberOfResults() == 0) {
            return Optional.empty();
        }

        return Optional.of(results.getResultList());
    }

}
