package org.ikasan.esb.service.replay;

import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.entity.EsbEntityService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.replay.ReplayEvent;

import java.util.List;

/**
 * Created by Ikasan Development Team on 23/09/2017.
 */
public class ReplayServiceImpl implements EsbEntityService<ReplayEvent>, BatchInsert<ReplayEvent>
{
    private EsbEntityDao<ReplayEvent> replayDao;

    /**
     * Constructor for ReplayServiceImpl.
     * This initializes the service with the provided data access object for managing ReplayEvent entities.
     * Throws an IllegalArgumentException if the provided DAO is null.
     *
     * @param replayDao the data access object responsible for handling ReplayEvent entities.
     *                  Must not be null.
     */
    public ReplayServiceImpl(EsbEntityDao<ReplayEvent> replayDao) {
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
