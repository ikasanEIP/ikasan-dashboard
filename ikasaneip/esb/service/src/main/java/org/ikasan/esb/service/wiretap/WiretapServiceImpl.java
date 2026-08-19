package org.ikasan.esb.service.wiretap;

import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.entity.EntityService;
import org.ikasan.spec.module.ModuleService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.wiretap.WiretapEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by amajewski on 23/09/2017.
 */
public class WiretapServiceImpl implements BatchInsert<WiretapEvent>, EntityService<WiretapEvent>
{

    /** Logger for this class */
    private static Logger logger = LoggerFactory.getLogger(WiretapServiceImpl.class);

    /**
     * Data access object for the persistence of <code>WiretapFlowEvent</code>
     */
    private EntityDao<WiretapEvent> wiretapDao;

    /**
     * Container for modules
     */
    private ModuleService moduleService;


    /**
     * Constructs an instance of SolrWiretapServiceImpl.
     *
     * @param wiretapDao the data access object for persisting WiretapEvent instances; must not be null
     * @param moduleService the service responsible for managing modules; must not be null
     * @throws IllegalArgumentException if wiretapDao or moduleService is null
     */
    public WiretapServiceImpl(EntityDao<WiretapEvent> wiretapDao, ModuleService moduleService) {
        this.wiretapDao = wiretapDao;
        if (wiretapDao == null) {
            throw new IllegalArgumentException("wiretapDao cannot be 'null'");
        }

        this.moduleService = moduleService;
        if(moduleService == null)
        {
            throw new IllegalArgumentException("moduleService cannot be 'null'");
        }
    }


    /**
     * Constructs an instance of SolrWiretapServiceImpl.
     *
     * @param wiretapDao the data access object for persisting WiretapEvent instances; must not be null
     * @throws IllegalArgumentException if wiretapDao is null
     */
    public WiretapServiceImpl(EntityDao<WiretapEvent> wiretapDao) {
        this.wiretapDao = wiretapDao;
        if (wiretapDao == null) {
            throw new IllegalArgumentException("wiretapDao cannot be 'null'");
        }

    }

    @Override
    public void save(WiretapEvent wiretapEvent) {
        wiretapDao.save(wiretapEvent);
    }

    @Override
    public void save(List<WiretapEvent> save) {
        wiretapDao.save(save);
    }

    @Override
    public void insert(List<WiretapEvent> entities)
    {
        this.save(entities);
    }
}
