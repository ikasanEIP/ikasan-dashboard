package org.ikasan.esb.service.error.reporting;

import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.entity.EsbEntityService;
import org.ikasan.spec.error.reporting.ErrorOccurrence;
import org.ikasan.spec.persistence.BatchInsert;

import java.util.List;

/**
 * Created by Ikasan Development Team on 23/09/2017.
 */
public class ErrorReportingServiceImpl implements EsbEntityService<ErrorOccurrence>, BatchInsert<ErrorOccurrence>
{

    private EsbEntityDao<ErrorOccurrence> errorReportingServiceDao;


    /**
     * Constructor for ErrorReportingServiceImpl.
     *
     * @param errorReportingServiceDao the DAO responsible for managing ErrorOccurrence entities.
     *                                 Must not be null.
     * @throws IllegalArgumentException if the provided errorReportingServiceDao is null.
     */
    public ErrorReportingServiceImpl(EsbEntityDao<ErrorOccurrence> errorReportingServiceDao) {
        this.errorReportingServiceDao = errorReportingServiceDao;
        if (this.errorReportingServiceDao == null) {
            throw new IllegalArgumentException("errorManagementDao cannot be null!");
        }
    }

    @Override
    public void save(ErrorOccurrence entity) {
        this.errorReportingServiceDao.save(entity);
    }

    @Override
    public void save(List<ErrorOccurrence> entities) {
        this.errorReportingServiceDao.save(entities);
    }

    @Override
    public void insert(List<ErrorOccurrence> entities)
    {
        this.save(entities);
    }
}
