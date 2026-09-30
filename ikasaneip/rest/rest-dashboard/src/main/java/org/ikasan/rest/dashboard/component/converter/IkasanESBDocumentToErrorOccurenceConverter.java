package org.ikasan.rest.dashboard.component.converter;

import org.ikasan.rest.dashboard.model.error.ErrorOccurrenceImpl;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.ikasan.spec.error.reporting.ErrorOccurrence;
import org.ikasan.spec.search.model.IkasanESBDocument;

public class IkasanESBDocumentToErrorOccurenceConverter implements Converter<IkasanESBDocument, ErrorOccurrence<byte[]>> {

    @Override
    public ErrorOccurrence<byte[]> convert(IkasanESBDocument ikasanESBDocument) throws TransformationException {
        ErrorOccurrence<byte[]> errorOccurrence = new ErrorOccurrenceImpl();
        errorOccurrence.setModuleName(ikasanESBDocument.getModuleName());
        errorOccurrence.setFlowName(ikasanESBDocument.getFlowName());
        errorOccurrence.setFlowElementName(ikasanESBDocument.getComponentName());
        if(ikasanESBDocument.getEvent()!=null)errorOccurrence.setEvent(ikasanESBDocument.getEvent().getBytes());
        errorOccurrence.setErrorDetail(ikasanESBDocument.getErrorDetail());
        errorOccurrence.setErrorMessage(ikasanESBDocument.getErrorMessage());
        errorOccurrence.setExceptionClass(ikasanESBDocument.getExceptionClass());
        errorOccurrence.setAction(ikasanESBDocument.getErrorAction());
        errorOccurrence.setUri(ikasanESBDocument.getErrorUri());
        errorOccurrence.setTimestamp(ikasanESBDocument.getTimestamp());
        errorOccurrence.setExpiry(ikasanESBDocument.getExpiry());

        return errorOccurrence;
    }
}
