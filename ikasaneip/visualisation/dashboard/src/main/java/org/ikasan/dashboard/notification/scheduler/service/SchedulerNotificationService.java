package org.ikasan.dashboard.notification.scheduler.service;

import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.solr.SolrGeneralService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SchedulerNotificationService {
    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;

    public SchedulerNotificationService(SolrGeneralService solrGeneralService) {
        this.solrGeneralService = solrGeneralService;
    }

    public Optional<List<IkasanSolrDocument>> getFailedScheduledJobs(String agentName, Long startTimestamp, Integer resultSize) {

        IkasanSolrDocumentSearchResults results = this.solrGeneralService.search(Set.of(agentName), Set.of()
            , "returnCode:0", startTimestamp, System.currentTimeMillis(), resultSize, List.of("scheduledProcessEvent")
            ,true, null, null);

        if(results.getTotalNumberOfResults() == 0) {
            return Optional.empty();
        }

        return Optional.of(results.getResultList());
    }

}
