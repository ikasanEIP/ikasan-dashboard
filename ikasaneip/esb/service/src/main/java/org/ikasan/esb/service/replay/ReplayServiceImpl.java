package org.ikasan.esb.service.replay;

import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.entity.EntityService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.replay.ReplayEvent;

import java.util.List;

/**
 * Created by Ikasan Development Team on 23/09/2017.
 */
public class ReplayServiceImpl implements EntityService<ReplayEvent>, BatchInsert<ReplayEvent>
{
    private EntityDao<ReplayEvent> replayDao;

    /**
     * Constructor for ReplayServiceImpl.
     * This initializes the service with the provided data access object for managing ReplayEvent entities.
     * Throws an IllegalArgumentException if the provided DAO is null.
     *
     * @param replayDao the data access object responsible for handling ReplayEvent entities.
     *                  Must not be null.
     */
    public ReplayServiceImpl(EntityDao<ReplayEvent> replayDao) {
        this.replayDao = replayDao;
        if (this.replayDao == null) {
            throw new IllegalArgumentException("replayDao cannot be null!");
        }
    }

    @Override
    public void save(ReplayEvent save) {
        this.replayDao.save(save);
    }

    @Override
    public void save(List<ReplayEvent> save) {
        this.replayDao.save(save);
    }

    @Override
    public void insert(List<ReplayEvent> entities)
    {
        this.save(entities);
    }
}
