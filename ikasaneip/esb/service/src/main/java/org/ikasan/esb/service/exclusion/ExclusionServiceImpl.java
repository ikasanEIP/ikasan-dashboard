package org.ikasan.esb.service.exclusion;

import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.entity.EntityService;
import org.ikasan.spec.exclusion.ExclusionEvent;
import org.ikasan.spec.persistence.BatchInsert;

import java.util.List;

/**
 * Created by Ikasan Development Team on 23/09/2017.
 */
public class ExclusionServiceImpl
    implements BatchInsert<ExclusionEvent>, EntityService<ExclusionEvent> {

    private EntityDao<ExclusionEvent> exclusionEventDao;

    /**
     * Constructor for ExclusionServiceImpl.
     * Initializes the ExclusionServiceImpl with the specified EntityDao.
     *
     * @param exclusionEventDao the data access object for handling ExclusionEvent entities.
     *                          Must not be null, otherwise an IllegalArgumentException is thrown.
     */
    public ExclusionServiceImpl(EntityDao<ExclusionEvent> exclusionEventDao) {
        this.exclusionEventDao = exclusionEventDao;
        if(this.exclusionEventDao == null) {
            throw new IllegalArgumentException("exclusionEventDao cannot be null!");
        }
    }
    @Override
    public void save(ExclusionEvent save) {
        this.exclusionEventDao.save(save);
    }

    @Override
    public void save(List<ExclusionEvent> save) {
        this.exclusionEventDao.save(save);
    }

    @Override
    public void insert(List<ExclusionEvent> entities)
    {
        this.save(entities);
    }
}
