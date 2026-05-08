package org.ikasan.esb.service.hospital;

import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.entity.EsbEntityService;
import org.ikasan.spec.hospital.model.ExclusionEventAction;
import org.ikasan.spec.hospital.service.HospitalAuditService;

import java.util.List;

public class HospitalServiceImpl implements HospitalAuditService, EsbEntityService<ExclusionEventAction>
{
    private EsbEntityDao<ExclusionEventAction> hospitalDao;

    /**
     * Constructs a new instance of HospitalServiceImpl with the given SolrHospitalDao.
     *
     * @param hospitalDao the data access object for managing ExclusionEventAction entities.
     *                         It cannot be null; otherwise, an IllegalArgumentException is thrown.
     */
    public HospitalServiceImpl(EsbEntityDao<ExclusionEventAction> hospitalDao) {
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
