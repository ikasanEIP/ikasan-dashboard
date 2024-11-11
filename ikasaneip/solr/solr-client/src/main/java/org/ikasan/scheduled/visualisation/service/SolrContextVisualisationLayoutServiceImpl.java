package org.ikasan.scheduled.visualisation.service;

import org.ikasan.spec.scheduled.visualisation.dao.ContextVisualisationLayoutDao;
import org.ikasan.spec.scheduled.visualisation.model.ContextVisualisationLayoutRecord;
import org.ikasan.spec.scheduled.visualisation.service.ContextVisualisationLayoutService;

import java.util.List;

public class SolrContextVisualisationLayoutServiceImpl implements ContextVisualisationLayoutService {
    private ContextVisualisationLayoutDao contextVisualisationLayoutDao;

    /**
     * Constructs a new SolrContextVisualisationLayoutServiceImpl with the specified ContextVisualisationLayoutDao.
     *
     * @param contextVisualisationLayoutDao the data access object for context visualisation layout
     */
    public SolrContextVisualisationLayoutServiceImpl(ContextVisualisationLayoutDao contextVisualisationLayoutDao) {
        this.contextVisualisationLayoutDao = contextVisualisationLayoutDao;
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
