package org.ikasan.dashboard.notification.business.stream.model;

import org.ikasan.solr.model.IkasanSolrDocument;

public class BusinessStreamExclusion {
    private IkasanSolrDocument exclusionEvent;
    private IkasanSolrDocument errorOccurrence;

    public BusinessStreamExclusion(IkasanSolrDocument exclusionEvent, IkasanSolrDocument errorOccurrence) {
        this.exclusionEvent = exclusionEvent;
        this.errorOccurrence = errorOccurrence;
    }

    public IkasanSolrDocument getExclusionEvent() {
        return exclusionEvent;
    }

    public IkasanSolrDocument getErrorOccurrence() {
        return errorOccurrence;
    }
}
