package org.ikasan.dashboard.ui.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringEscapeUtils;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.systemevent.model.SolrSystemEvent;
import org.ikasan.systemevent.model.SystemEventImpl;

import java.time.ZoneId;

public class IkasanSystemEventDocumentToCsvConverter {
    public static final String HEADER = "Action Performed By,System Event Context,System Event,Timestamp\n";

    private StringBuilder csvContents;

    private DateFormatter dateFormatter;

    ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    public IkasanSystemEventDocumentToCsvConverter(ZoneId zoneId) {
        this.csvContents = new StringBuilder(HEADER);
        this.dateFormatter = new DateFormatter(zoneId);
    }

    public void addDocument(SystemEvent ikasanSolrDocument) throws JsonProcessingException {
        csvContents.append("\"").append((objectMapper.readValue(((SolrSystemEvent)ikasanSolrDocument).getPayload()
                , SystemEventImpl.class).getActor())).append("\"").append(",")
            .append("\"").append(ikasanSolrDocument.getSubject() == null ? "" : ikasanSolrDocument.getSubject()).append("\"").append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getAction())).append(",")
            .append("\"").append(this.dateFormatter.getFormattedDate(((SolrSystemEvent) ikasanSolrDocument).getTimestampLong())).append("\"").append("\n");
    }

    public String getCvsContents() {
        return this.csvContents.toString();
    }
}
