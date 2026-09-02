package org.ikasan.dashboard.ui.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang.StringEscapeUtils;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventRecord;
import org.ikasan.systemevent.model.SystemEventImpl;
import tools.jackson.databind.json.JsonMapper;

import java.time.ZoneId;

public class IkasanSystemEventDocumentToCsvConverter {
    public static final String HEADER = "Action Performed By,System Event Context,System Event,Timestamp\n";

    private StringBuilder csvContents;

    private DateFormatter dateFormatter;

    private JsonMapper objectMapper = ObjectMapperFactory.newInstance();

    public IkasanSystemEventDocumentToCsvConverter(ZoneId zoneId) {
        this.csvContents = new StringBuilder(HEADER);
        this.dateFormatter = new DateFormatter(zoneId);
    }

    public void addDocument(SystemEvent ikasanSolrDocument) throws JsonProcessingException {
        csvContents.append("\"").append((objectMapper.readValue(((SystemEventRecord)ikasanSolrDocument).getPayload()
                , SystemEventImpl.class).getActor())).append("\"").append(",")
            .append("\"").append(ikasanSolrDocument.getSubject() == null ? "" : ikasanSolrDocument.getSubject()).append("\"").append(",")
            .append(StringEscapeUtils.escapeCsv(ikasanSolrDocument.getAction())).append(",")
            .append("\"").append(this.dateFormatter.getFormattedDate(ikasanSolrDocument.getTimestamp().getTime())).append("\"").append("\n");
    }

    public String getCvsContents() {
        return this.csvContents.toString();
    }
}
