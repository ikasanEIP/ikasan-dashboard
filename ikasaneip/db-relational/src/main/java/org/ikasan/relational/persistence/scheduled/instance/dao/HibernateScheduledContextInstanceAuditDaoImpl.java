package org.ikasan.relational.persistence.scheduled.instance.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.ikasan.relational.persistence.scheduled.instance.model.HibernateScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate/PostgreSQL implementation of ScheduledContextInstanceAuditDao.
 *
 * This DAO manages audit records for scheduled context instances.
 * Note: This implementation uses the same entity as the main DAO, but could be
 * configured to use a separate audit table if needed.
 */
public class HibernateScheduledContextInstanceAuditDaoImpl implements ScheduledContextInstanceAuditDao {

    private static final Logger logger = LoggerFactory.getLogger(HibernateScheduledContextInstanceAuditDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     */
    public HibernateScheduledContextInstanceAuditDaoImpl() {
    }

    /**
     * Constructor with EntityManager for testing
     */
    public HibernateScheduledContextInstanceAuditDaoImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void save(ScheduledContextInstanceRecord scheduledContextInstanceRecord) {
        logger.debug("Saving audit ScheduledContextInstanceRecord: {}",
            scheduledContextInstanceRecord.getId());

        if (!(scheduledContextInstanceRecord instanceof HibernateScheduledContextInstanceRecord)) {
            throw new IllegalArgumentException(
                "ScheduledContextInstanceRecord must be an instance of HibernateScheduledContextInstanceRecord");
        }

        HibernateScheduledContextInstanceRecord hibernateRecord =
            (HibernateScheduledContextInstanceRecord) scheduledContextInstanceRecord;

        // Set timestamps if new
        if (hibernateRecord.getTimestamp() == 0) {
            hibernateRecord.setTimestamp(System.currentTimeMillis());
        }
        hibernateRecord.setModifiedTimestamp(System.currentTimeMillis());

        // Persist as audit record
        entityManager.persist(hibernateRecord);

        logger.debug("Successfully saved audit ScheduledContextInstanceRecord: {}",
            hibernateRecord.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledContextInstanceRecord findById(String id) {
        logger.debug("Finding audit ScheduledContextInstanceRecord by id: {}", id);
        return entityManager.find(HibernateScheduledContextInstanceRecord.class, id);
    }
}
