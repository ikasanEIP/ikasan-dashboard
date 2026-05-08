package org.ikasan.orchestration.service.scheduled.profile;

import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;

import java.util.List;

public class ContextProfileServiceImpl implements ContextProfileService {

    private final ContextProfileDao contextProfileDao;

    /**
     * Constructs a new instance of {@code ContextProfileServiceImpl} with the specified {@code ContextProfileDao}.
     *
     * This constructor initializes the service implementation and ensures the provided {@code ContextProfileDao}
     * is not null. If the provided {@code ContextProfileDao} is null, an {@code IllegalArgumentException} will
     * be thrown.
     *
     * @param contextProfileDao the data access object to interact with context profile records; must not be null
     * @throws IllegalArgumentException if {@code contextProfileDao} is null
     */
    public ContextProfileServiceImpl(ContextProfileDao contextProfileDao) {
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
    public void save(List<ContextProfileRecord> records) {
        this.contextProfileDao.save(records);
    }

    @Override
    public void deleteByContextName(String contextName) { this.contextProfileDao.deleteByContextName(contextName); }

    @Override
    public ContextProfileRecord findById(String id) {
        return this.contextProfileDao.findById(id);
    }

    @Override
    public SearchResults<ContextProfileRecord> findByFilter(ContextProfileSearchFilter contextProfileSearchFilter, int limit, int offset, String sortColumn, String sortOrder) {
        return this.contextProfileDao.findByFilter(contextProfileSearchFilter, limit, offset, sortColumn, sortOrder);
    }

}
