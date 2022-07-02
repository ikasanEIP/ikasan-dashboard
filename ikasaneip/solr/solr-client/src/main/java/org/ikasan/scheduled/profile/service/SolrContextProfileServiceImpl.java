package org.ikasan.scheduled.profile.service;

import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;

public class SolrContextProfileServiceImpl implements ContextProfileService {

    private ContextProfileDao contextProfileDao;

    public SolrContextProfileServiceImpl(ContextProfileDao contextProfileDao) {
        this.contextProfileDao = contextProfileDao;

        if(this.contextProfileDao == null) {
            throw new IllegalArgumentException("contextProfileDao cannot be null!");
        }
    }

    @Override
    public void save(ContextProfileRecord contextProfileRecord) {
        this.contextProfileDao.save(contextProfileRecord);
    }

    @Override
    public ContextProfileRecord findById(String id) {
        return this.contextProfileDao.findById(id);
    }

    @Override
    public SearchResults<ContextProfileRecord> findByFilter(ContextProfileSearchFilter contextProfileSearchFilter, int limit, int offset, String sortColumn, String sortOrder) {
        return this.contextProfileDao.findByFilter(contextProfileSearchFilter, limit, offset, sortColumn, sortOrder);
    }

}
