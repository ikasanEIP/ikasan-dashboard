package org.ikasan.rest.dashboard.component.converter;

import org.ikasan.rest.dashboard.model.wiretap.WiretapEventImpl;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.wiretap.WiretapEvent;

public class IkasanESBDocumentToWiretapEventConverter implements Converter<IkasanESBDocument, WiretapEvent> {

    @Override
    public WiretapEvent convert(IkasanESBDocument ikasanESBDocument) throws TransformationException {
        WiretapEventImpl wiretapEvent = new WiretapEventImpl();
        wiretapEvent.setId(ikasanESBDocument.getId());
        wiretapEvent.setEventId(ikasanESBDocument.getEventId());
        wiretapEvent.setModuleName(ikasanESBDocument.getModuleName());
        wiretapEvent.setFlowName(ikasanESBDocument.getFlowName());
        wiretapEvent.setComponentName(ikasanESBDocument.getComponentName());
        wiretapEvent.setEvent(ikasanESBDocument.getEvent());
        wiretapEvent.setTimestamp(ikasanESBDocument.getTimestamp());

        return wiretapEvent;
    }
}
