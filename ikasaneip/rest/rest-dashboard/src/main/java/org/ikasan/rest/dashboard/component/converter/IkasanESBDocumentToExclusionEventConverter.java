package org.ikasan.rest.dashboard.component.converter;

import org.ikasan.rest.dashboard.model.exclusion.ExclusionEventImpl;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.ikasan.spec.exclusion.ExclusionEvent;
import org.ikasan.spec.search.model.IkasanESBDocument;

public class IkasanESBDocumentToExclusionEventConverter implements Converter<IkasanESBDocument, ExclusionEvent<String>> {

    @Override
    public ExclusionEvent<String> convert(IkasanESBDocument ikasanESBDocument) throws TransformationException {
        ExclusionEvent<String> errorOccurrence = new ExclusionEventImpl();
        errorOccurrence.setModuleName(ikasanESBDocument.getModuleName());
        errorOccurrence.setFlowName(ikasanESBDocument.getFlowName());
        errorOccurrence.setEvent(ikasanESBDocument.getEvent().getBytes());
        errorOccurrence.setErrorUri(ikasanESBDocument.getErrorUri());
        errorOccurrence.setTimestamp(ikasanESBDocument.getTimestamp());
        errorOccurrence.setIdentifier(ikasanESBDocument.getIdentifier());

        return errorOccurrence;
    }
}
