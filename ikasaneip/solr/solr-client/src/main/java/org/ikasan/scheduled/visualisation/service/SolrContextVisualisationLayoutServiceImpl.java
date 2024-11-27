package org.ikasan.scheduled.visualisation.service;

import org.ikasan.scheduled.visualisation.dao.SolrContextVisualisationLayoutDaoImpl;
import org.ikasan.spec.scheduled.visualisation.dao.ContextVisualisationLayoutDao;
import org.ikasan.spec.scheduled.visualisation.model.ContextVisualisationLayoutRecord;
import org.ikasan.spec.scheduled.visualisation.service.ContextVisualisationLayoutService;

import java.util.List;

public class SolrContextVisualisationLayoutServiceImpl implements ContextVisualisationLayoutService {
    private SolrContextVisualisationLayoutDaoImpl contextVisualisationLayoutDao;

    /**
     * Constructs a new SolrContextVisualisationLayoutServiceImpl with the specified ContextVisualisationLayoutDao.
     *
     * @param contextVisualisationLayoutDao the data access object for context visualisation layout
     */
    public SolrContextVisualisationLayoutServiceImpl(SolrContextVisualisationLayoutDaoImpl contextVisualisationLayoutDao) {
        this.contextVisualisationLayoutDao = contextVisualisationLayoutDao;
    }

    @Override
    public void save(ContextVisualisationLayoutRecord contextVisualisationLayoutRecord) {
        this.contextVisualisationLayoutDao.save(contextVisualisationLayoutRecord);
    }

    @Override
    public ContextVisualisationLayoutRecord findById(String id) {
        return this.contextVisualisationLayoutDao.findById(id);
    }

    @Override
    public List<ContextVisualisationLayoutRecord> findByParentContext(String parentContext) {
        return this.contextVisualisationLayoutDao.findByParentContext(parentContext);
    }

    @Override
    public ContextVisualisationLayoutRecord findByParentContextAndContext(String parentContext, String context) {
        return this.contextVisualisationLayoutDao.findByParentContextAndContext(parentContext, context);
    }
}
