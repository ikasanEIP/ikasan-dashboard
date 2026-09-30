package org.ikasan.rest.dashboard.component.converter;

import org.ikasan.rest.dashboard.model.replay.ReplayEventImpl;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.ikasan.spec.replay.ReplayEvent;
import org.ikasan.spec.search.model.IkasanESBDocument;

public class IkasanESBDocumentToReplayEventConverter implements Converter<IkasanESBDocument, ReplayEvent> {

    @Override
    public ReplayEvent convert(IkasanESBDocument ikasanESBDocument) throws TransformationException {
        ReplayEventImpl replayEvent = new ReplayEventImpl();
        replayEvent.setId(ikasanESBDocument.getId());
        replayEvent.setModuleName(ikasanESBDocument.getModuleName());
        replayEvent.setFlowName(ikasanESBDocument.getFlowName());
        replayEvent.setEvent(ikasanESBDocument.getEvent().getBytes());
        replayEvent.setTimestamp(ikasanESBDocument.getTimestamp());
        replayEvent.setEventId(ikasanESBDocument.getEventId());
        replayEvent.setExpiry(ikasanESBDocument.getExpiry());

        return replayEvent;
    }
}
