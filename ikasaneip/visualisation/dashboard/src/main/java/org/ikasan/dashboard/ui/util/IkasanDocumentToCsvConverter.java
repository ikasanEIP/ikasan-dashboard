package org.ikasan.dashboard.ui.util;

import org.apache.commons.lang.StringEscapeUtils;
import org.ikasan.solr.model.IkasanSolrDocument;

public class IkasanDocumentToCsvConverter {
    public static final String HEADER = "ModuleName,FlowName,ComponentName,Type,ErrorMessage,Payload\n";

    private StringBuilder csvContents;

    public IkasanDocumentToCsvConverter() {
        this.csvContents = new StringBuilder(HEADER);
    }

    public void addDocument(IkasanSolrDocument ikasanSolrDocument) {
        csvContents.append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getModuleName())).append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getFlowName())).append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getComponentName())).append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getType())).append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getErrorMessage())).append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getEvent())).append("\n");
    }

    public String getCvsContents() {
        return this.csvContents.toString();
    }
}
