package org.ikasan.dashboard.notification.business.stream.model;

import org.ikasan.spec.search.model.IkasanESBDocument;

public class BusinessStreamExclusion {
    private IkasanESBDocument exclusionEvent;
    private IkasanESBDocument errorOccurrence;

    public BusinessStreamExclusion(IkasanESBDocument exclusionEvent, IkasanESBDocument errorOccurrence) {
        this.exclusionEvent = exclusionEvent;
        this.errorOccurrence = errorOccurrence;
    }

    public IkasanESBDocument getExclusionEvent() {
        return exclusionEvent;
    }

    public IkasanESBDocument getErrorOccurrence() {
        return errorOccurrence;
    }
}
