package org.ikasan.relational.persistence.module.metadata.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.module.metadata.model.*;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.metadata.model.*;
import org.ikasan.spec.module.ModuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hibernate/PostgreSQL implementation of ModuleMetadataDao.
 *
 * This class provides Hibernate-based persistence for module metadata using PostgreSQL,
 * implementing the ModuleMetadataDao interface.
 */
public class HibernateModuleMetadataDaoImpl implements ModuleMetadataDao {

    private static final Logger logger = LoggerFactory.getLogger(HibernateModuleMetadataDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    /**
     * Constructor initializing the ObjectMapper with type mappings
     */
    public HibernateModuleMetadataDaoImpl() {
        this.objectMapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addAbstractTypeMapping(FlowMetaData.class, HibernateFlowMetaDataImpl.class);
        module.addAbstractTypeMapping(FlowElementMetaData.class, HibernateFlowElementMetaDataImpl.class);
        module.addAbstractTypeMapping(Transition.class, HibernateTransitionImpl.class);
        module.addAbstractTypeMapping(DecoratorMetaData.class, HibernateDecoratorMetaDataImpl.class);

        objectMapper.registerModule(module);
    }

    @Override
    @Transactional
    public void save(List<ModuleMetaData> moduleMetaDataList) {
        if (moduleMetaDataList == null || moduleMetaDataList.isEmpty()) {
            logger.debug("No module metadata to save");
            return;
        }

        logger.debug("Saving {} module metadata records", moduleMetaDataList.size());

        for (ModuleMetaData moduleMetaData : moduleMetaDataList) {
            HibernateModuleMetaDataImpl entity = convertToEntity(moduleMetaData);

            // Check if entity already exists
            HibernateModuleMetaDataImpl existing = entityManager.find(HibernateModuleMetaDataImpl.class, entity.getName());

            if (existing != null) {
                // Update existing entity
                existing.setType(entity.getType());
                existing.setUrl(entity.getUrl());
                existing.setHost(entity.getHost());
                existing.setPort(entity.getPort());
                existing.setContext(entity.getContext());
                existing.setProtocol(entity.getProtocol());
                existing.setDescription(entity.getDescription());
                existing.setVersion(entity.getVersion());
                existing.setIkasanVersion(entity.getIkasanVersion());
                existing.setConfiguredResourceId(entity.getConfiguredResourceId());
                existing.setFlows(entity.getFlows());
                entityManager.merge(existing);
            } else {
                // Persist new entity
                entityManager.persist(entity);
            }
        }

        entityManager.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleMetaData findById(String id) {
        logger.debug("Finding module metadata by id: {}", id);

        HibernateModuleMetaDataImpl entity = entityManager.find(HibernateModuleMetaDataImpl.class, id);

        return entity; // Entity implements ModuleMetaData interface
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        logger.debug("Deleting module metadata by id: {}", id);

        HibernateModuleMetaDataImpl entity = entityManager.find(HibernateModuleMetaDataImpl.class, id);

        if (entity != null) {
            entityManager.remove(entity);
            entityManager.flush();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleMetaData> findAll(Integer startOffset, Integer resultSize) {
        logger.debug("Finding all module metadata with offset={}, size={}", startOffset, resultSize);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<HibernateModuleMetaDataImpl> query = cb.createQuery(HibernateModuleMetaDataImpl.class);
        Root<HibernateModuleMetaDataImpl> root = query.from(HibernateModuleMetaDataImpl.class);

        query.select(root);
        query.orderBy(cb.asc(root.get("name")));

        TypedQuery<HibernateModuleMetaDataImpl> typedQuery = entityManager.createQuery(query);

        if (startOffset != null && startOffset > 0) {
            typedQuery.setFirstResult(startOffset);
        }
        if (resultSize != null && resultSize > 0) {
            typedQuery.setMaxResults(resultSize);
        }

        List<HibernateModuleMetaDataImpl> results = typedQuery.getResultList();

        logger.debug("Found {} module metadata records", results.size());

        return new ArrayList<>(results);
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleMetadataSearchResults find(List<String> modulesNames, Integer startOffset, Integer resultSize) {
        logger.debug("Finding module metadata with filter: names={}, offset={}, size={}",
            modulesNames, startOffset, resultSize);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateModuleMetaDataImpl> countRoot = countQuery.from(HibernateModuleMetaDataImpl.class);

        if (modulesNames != null && !modulesNames.isEmpty()) {
            countQuery.where(countRoot.get("name").in(modulesNames));
        }

        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateModuleMetaDataImpl> query = cb.createQuery(HibernateModuleMetaDataImpl.class);
        Root<HibernateModuleMetaDataImpl> root = query.from(HibernateModuleMetaDataImpl.class);

        if (modulesNames != null && !modulesNames.isEmpty()) {
            query.where(root.get("name").in(modulesNames));
        }

        query.select(root);
        query.orderBy(cb.asc(root.get("name")));

        TypedQuery<HibernateModuleMetaDataImpl> typedQuery = entityManager.createQuery(query);

        if (startOffset != null && startOffset > 0) {
            typedQuery.setFirstResult(startOffset);
        }
        if (resultSize != null && resultSize > 0) {
            typedQuery.setMaxResults(resultSize);
        }

        long startTime = System.currentTimeMillis();
        List<HibernateModuleMetaDataImpl> results = typedQuery.getResultList();
        long queryTime = System.currentTimeMillis() - startTime;

        logger.debug("Found {} module metadata records out of {} total", results.size(), totalCount);

        return new ModuleMetadataSearchResults(
            new ArrayList<>(results),
            totalCount,
            queryTime
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleMetadataSearchResults find(List<String> modulesNames, ModuleType moduleType,
                                            Integer startOffset, Integer resultSize) {
        logger.debug("Finding module metadata with filter: names={}, type={}, offset={}, size={}",
            modulesNames, moduleType, startOffset, resultSize);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Build predicates
        List<Predicate> predicates = new ArrayList<>();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateModuleMetaDataImpl> countRoot = countQuery.from(HibernateModuleMetaDataImpl.class);

        if (modulesNames != null && !modulesNames.isEmpty()) {
            predicates.add(countRoot.get("name").in(modulesNames));
        }
        if (moduleType != null) {
            predicates.add(cb.equal(countRoot.get("moduleType"), moduleType));
        }

        if (!predicates.isEmpty()) {
            countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        predicates.clear();
        CriteriaQuery<HibernateModuleMetaDataImpl> query = cb.createQuery(HibernateModuleMetaDataImpl.class);
        Root<HibernateModuleMetaDataImpl> root = query.from(HibernateModuleMetaDataImpl.class);

        if (modulesNames != null && !modulesNames.isEmpty()) {
            predicates.add(root.get("name").in(modulesNames));
        }
        if (moduleType != null) {
            predicates.add(cb.equal(root.get("moduleType"), moduleType));
        }

        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        query.select(root);
        query.orderBy(cb.asc(root.get("name")));

        TypedQuery<HibernateModuleMetaDataImpl> typedQuery = entityManager.createQuery(query);

        if (startOffset != null && startOffset > -1) {
            if (startOffset > 0) {
                typedQuery.setFirstResult(startOffset);
            }
        }
        if (resultSize != null && resultSize > -1) {
            if (resultSize > 0) {
                typedQuery.setMaxResults(resultSize);
            }
        }

        long startTime = System.currentTimeMillis();
        List<HibernateModuleMetaDataImpl> results = typedQuery.getResultList();
        long queryTime = System.currentTimeMillis() - startTime;

        logger.debug("Found {} module metadata records out of {} total", results.size(), totalCount);

        return new ModuleMetadataSearchResults(
            new ArrayList<>(results),
            totalCount,
            queryTime
        );
    }

    /**
     * Convert ModuleMetaData interface to HibernateModuleMetaDataImpl entity
     */
    private HibernateModuleMetaDataImpl convertToEntity(ModuleMetaData moduleMetaData) {
        if (moduleMetaData instanceof HibernateModuleMetaDataImpl) {
            return (HibernateModuleMetaDataImpl) moduleMetaData;
        }

        HibernateModuleMetaDataImpl entity = new HibernateModuleMetaDataImpl();
        entity.setName(moduleMetaData.getName());
        entity.setType(moduleMetaData.getType());
        entity.setUrl(moduleMetaData.getUrl());
        entity.setHost(moduleMetaData.getHost());
        entity.setPort(moduleMetaData.getPort());
        entity.setContext(moduleMetaData.getContext());
        entity.setProtocol(moduleMetaData.getProtocol());
        entity.setDescription(moduleMetaData.getDescription());
        entity.setVersion(moduleMetaData.getVersion());
        entity.setIkasanVersion(moduleMetaData.getIkasanVersion());
        entity.setConfiguredResourceId(moduleMetaData.getConfiguredResourceId());

        // Convert flows
        if (moduleMetaData.getFlows() != null) {
            List<FlowMetaData> flows = moduleMetaData.getFlows().stream()
                .map(this::convertFlowToHibernate)
                .collect(Collectors.toList());
            entity.setFlows(flows);
        }

        return entity;
    }

    /**
     * Convert FlowMetaData to HibernateFlowMetaDataImpl
     */
    private FlowMetaData convertFlowToHibernate(FlowMetaData flow) {
        if (flow instanceof HibernateFlowMetaDataImpl) {
            return flow;
        }

        HibernateFlowMetaDataImpl hibernateFlow = new HibernateFlowMetaDataImpl();
        hibernateFlow.setName(flow.getName());
        hibernateFlow.setConfigurationId(flow.getConfigurationId());
        hibernateFlow.setFlowStartupType(flow.getFlowStartupType());
        hibernateFlow.setFlowStartupComment(flow.getFlowStartupComment());

        if (flow.getConsumer() != null) {
            hibernateFlow.setConsumer(convertFlowElementToHibernate(flow.getConsumer()));
        }

        if (flow.getTransitions() != null) {
            List<Transition> transitions = flow.getTransitions().stream()
                .map(this::convertTransitionToHibernate)
                .collect(Collectors.toList());
            hibernateFlow.setTransitions(transitions);
        }

        if (flow.getFlowElements() != null) {
            List<FlowElementMetaData> elements = flow.getFlowElements().stream()
                .map(this::convertFlowElementToHibernate)
                .collect(Collectors.toList());
            hibernateFlow.setFlowElements(elements);
        }

        return hibernateFlow;
    }

    /**
     * Convert FlowElementMetaData to HibernateFlowElementMetaDataImpl
     */
    private FlowElementMetaData convertFlowElementToHibernate(FlowElementMetaData element) {
        if (element instanceof HibernateFlowElementMetaDataImpl) {
            return element;
        }

        HibernateFlowElementMetaDataImpl hibernateElement = new HibernateFlowElementMetaDataImpl();
        hibernateElement.setComponentName(element.getComponentName());
        hibernateElement.setDescription(element.getDescription());
        hibernateElement.setComponentType(element.getComponentType());
        hibernateElement.setImplementingClass(element.getImplementingClass());
        hibernateElement.setConfigurable(element.isConfigurable());
        hibernateElement.setConfigurationId(element.getConfigurationId());
        hibernateElement.setInvokerConfigurationId(element.getInvokerConfigurationId());

        if (element.getDecorators() != null) {
            List<DecoratorMetaData> decorators = element.getDecorators().stream()
                .map(this::convertDecoratorToHibernate)
                .collect(Collectors.toList());
            hibernateElement.setDecorators(decorators);
        }

        return hibernateElement;
    }

    /**
     * Convert Transition to HibernateTransitionImpl
     */
    private Transition convertTransitionToHibernate(Transition transition) {
        if (transition instanceof HibernateTransitionImpl) {
            return transition;
        }

        HibernateTransitionImpl hibernateTransition = new HibernateTransitionImpl();
        hibernateTransition.setFrom(transition.getFrom());
        hibernateTransition.setTo(transition.getTo());
        hibernateTransition.setName(transition.getName());

        return hibernateTransition;
    }

    /**
     * Convert DecoratorMetaData to HibernateDecoratorMetaDataImpl
     */
    private DecoratorMetaData convertDecoratorToHibernate(DecoratorMetaData decorator) {
        if (decorator instanceof HibernateDecoratorMetaDataImpl) {
            return decorator;
        }

        HibernateDecoratorMetaDataImpl hibernateDecorator = new HibernateDecoratorMetaDataImpl();
        hibernateDecorator.setName(decorator.getName());
        hibernateDecorator.setType(decorator.getType());
        hibernateDecorator.setConfigurable(decorator.isConfigurable());
        hibernateDecorator.setConfigurationId(decorator.getConfigurationId());

        return hibernateDecorator;
    }
}
