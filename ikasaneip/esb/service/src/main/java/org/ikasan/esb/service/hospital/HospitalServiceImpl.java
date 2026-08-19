package org.ikasan.esb.service.hospital;

import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.entity.EntityService;
import org.ikasan.spec.hospital.model.ExclusionEventAction;
import org.ikasan.spec.hospital.service.HospitalAuditService;

import java.util.List;

public class HospitalServiceImpl implements HospitalAuditService, EntityService<ExclusionEventAction>
{
    private EntityDao<ExclusionEventAction> hospitalDao;

    /**
     * Constructs a new instance of HospitalServiceImpl with the given SolrHospitalDao.
     *
     * @param hospitalDao the data access object for managing ExclusionEventAction entities.
     *                         It cannot be null; otherwise, an IllegalArgumentException is thrown.
     */
    public HospitalServiceImpl(EntityDao<ExclusionEventAction> hospitalDao) {
        this.hospitalDao = hospitalDao;
        if(this.hospitalDao == null) {
            throw new IllegalArgumentException("SolrHospitalDao cannot be null!");
        }
    }

    @Override
    public void save(ExclusionEventAction exclusionEventAction) {
        hospitalDao.save(exclusionEventAction);
    }

    @Override
    public void save(List<ExclusionEventAction> exclusionEventActions) {
        hospitalDao.save(exclusionEventActions);
    }
}
