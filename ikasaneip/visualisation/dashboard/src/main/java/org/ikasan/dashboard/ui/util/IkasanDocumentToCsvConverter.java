package org.ikasan.dashboard.ui.util;

import org.apache.commons.lang.StringEscapeUtils;
import org.ikasan.spec.search.model.IkasanESBDocument;

public class IkasanDocumentToCsvConverter {
    public static final String HEADER = "ModuleName,FlowName,ComponentName,Type,ErrorMessage,Payload,Event Id,Timestamp\n";

    private StringBuilder csvContents;

    private DateFormatter dateFormatter;

    public IkasanDocumentToCsvConverter() {
        this.csvContents = new StringBuilder(HEADER);
        this.dateFormatter = new DateFormatter();
    }

    public void addDocument(IkasanESBDocument ikasanSolrDocument) {
        csvContents.append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getModuleName())).append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getFlowName())).append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getComponentName() == null ? "" : ikasanSolrDocument.getComponentName())).append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getType())).append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getErrorMessage() == null ? "" : ikasanSolrDocument.getErrorMessage())).append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getEvent())).append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getEventId())).append(",")
            .append(StringEscapeUtils.escapeCsv(this.dateFormatter.getFormattedDate(ikasanSolrDocument.getTimestamp()))).append("\n");
    }

    public String getCvsContents() {
        return this.csvContents.toString();
    }
}
