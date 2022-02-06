package org.ikasan.dashboard.notification.business.stream.service;

import org.ikasan.business.stream.metadata.model.BusinessStream;
import org.ikasan.business.stream.metadata.model.Flow;
import org.ikasan.dashboard.notification.business.stream.model.BusinessStreamExclusion;
import org.ikasan.dashboard.notification.business.stream.model.BusinessStreamExclusions;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.solr.SolrGeneralService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BusinessStreamNotificationService {
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;

    public BusinessStreamNotificationService(BusinessStreamMetaDataService businessStreamMetaDataService
        , SolrGeneralService solrGeneralService) {
        this.businessStreamMetaDataService = businessStreamMetaDataService;
        this.solrGeneralService = solrGeneralService;
    }

    public Optional<BusinessStreamExclusions> getBusinessStreamExclusions(String businessStreamName, Long startTimestamp, Integer resultSize) {
        BusinessStreamMetaData<BusinessStream> businessStreamMetaData = this.businessStreamMetaDataService
            .findById("businessStream-" + businessStreamName);

        if(businessStreamMetaData == null) {
            return Optional.empty();
        }

        var ref = new Object() {
            Set<String> moduleNames = new HashSet<>();
            Set<String> flowNames = new HashSet<>();
        };

        if(businessStreamMetaData.getJson().contains("draw2d")) {
            JSONArray jsonArray = new JSONArray(businessStreamMetaData.getJson());

            jsonArray.iterator().forEachRemaining(item -> {
                if (((JSONObject) item).getString("type").equals("draw2d.shape.basic.Image")) {
                    if (((JSONObject) item).getString("id").startsWith("FLOW:")) {
                        String id = ((JSONObject) item).getString("id");
                        String moduleName = id.substring(id.indexOf("FLOW:") + "FLOW:".length(), id.indexOf("."));
                        String flowName = id.substring(id.indexOf(".") + 1, id.indexOf(":", id.indexOf(".")));

                        ref.moduleNames.add(moduleName);
                        ref.flowNames.add(flowName);
                    }
                }
            });
        }
        else {
            ref.moduleNames = businessStreamMetaData.getBusinessStream().getFlows()
                .stream()
                .map(Flow::getModuleName)
                .collect(Collectors.toSet());

            ref.flowNames = businessStreamMetaData.getBusinessStream().getFlows()
                .stream()
                .map(Flow::getFlowName)
                .collect(Collectors.toSet());
        }

        IkasanSolrDocumentSearchResults results = this.solrGeneralService.search(ref.moduleNames, ref.flowNames
            , null, startTimestamp, System.currentTimeMillis(), resultSize, List.of("exclusion")
            ,false, null, null);

        if(results.getTotalNumberOfResults() == 0) {
            return Optional.empty();
        }

        return Optional.of(new BusinessStreamExclusions(businessStreamMetaData, this.getBusinessStreamExclusions(results)));
    }

    private List<BusinessStreamExclusion> getBusinessStreamExclusions(IkasanSolrDocumentSearchResults results) {
        if(results.getResultList().size() > 0) {
            Map<String, IkasanSolrDocument> errorOccurrencesMap = results.getResultList()
                .stream()
                .map(ikasanDoc -> this.solrGeneralService.findByErrorUri("error", this.getErrorUri(ikasanDoc.getId())))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(IkasanSolrDocument::getErrorUri, Function.identity()));

            List<BusinessStreamExclusion> businessStreamExclusionsList = new ArrayList<>();

            results.getResultList().forEach(ikasanSolrDocument -> {
                BusinessStreamExclusion businessStreamExclusion = new BusinessStreamExclusion(ikasanSolrDocument,
                    errorOccurrencesMap.get(this.getErrorUri(ikasanSolrDocument.getId())));

                businessStreamExclusionsList.add(businessStreamExclusion);
            });

            return businessStreamExclusionsList;
        }

        return new ArrayList<>();
    }

    private String getErrorUri(String id){
        if(id.contains(":")) {
            id = id.substring(id.lastIndexOf(":") + 1);
        }
        return id;
    }
}
