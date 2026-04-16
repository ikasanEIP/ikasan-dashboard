package org.ikasan.relational.persistence.scheduled.profile.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.profile.model.HibernateContextProfileRecord;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

public class HibernateContextProfileDaoImpl implements ContextProfileDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void save(ContextProfileRecord contextProfileRecord) {
        this.entityManager.merge(contextProfileRecord);
    }

    @Override
    @Transactional
    public void deleteByContextName(String contextName) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<HibernateContextProfileRecord> delete = cb.createCriteriaDelete(HibernateContextProfileRecord.class);
        Root<HibernateContextProfileRecord> root = delete.from(HibernateContextProfileRecord.class);

        delete.where(cb.equal(root.get("contextName"), contextName));

        entityManager.createQuery(delete).executeUpdate();
    }

    @Override
    public ContextProfileRecord findById(String id) {
        return this.entityManager.find(HibernateContextProfileRecord.class, id);
    }

    @Override
    public SearchResults<ContextProfileRecord> findByFilter(ContextProfileSearchFilter filter, int limit, int offset, String sortColumn, String sortOrder) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateContextProfileRecord> countRoot = countQuery.from(HibernateContextProfileRecord.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, filter));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Results query
        CriteriaQuery<ContextProfileRecord> query = cb.createQuery(ContextProfileRecord.class);
        Root<HibernateContextProfileRecord> root = query.from(HibernateContextProfileRecord.class);

        query.select(root);
        query.where(buildPredicates(cb, root, filter));

        // Apply sorting
        if (sortColumn != null && !sortColumn.isEmpty()) {
            if ("ASCENDING".equals(sortOrder)) {
                query.orderBy(cb.asc(root.get(sortColumn)));
            } else {
                query.orderBy(cb.desc(root.get(sortColumn)));
            }
        } else {
            // Default sort by createdDateTime descending
            query.orderBy(cb.desc(root.get("createdDateTime")));
        }

        TypedQuery<ContextProfileRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<ContextProfileRecord> results = typedQuery.getResultList();

        return new SearchResultsImpl<>(results, totalCount, (long) offset);
    }

    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<HibernateContextProfileRecord> root, ContextProfileSearchFilter filter) {
        List<Predicate> predicates = new ArrayList<>();

        // Profile name filter (required in Solr impl, uses wildcard if empty)
        if (filter.getProfileName() != null && !filter.getProfileName().isEmpty()) {
            predicates.add(cb.equal(root.get("profileName"), filter.getProfileName()));
        }

        // Context name filter (required in Solr impl, uses wildcard if empty)
        if (filter.getContextName() != null && !filter.getContextName().isEmpty()) {
            predicates.add(cb.equal(root.get("contextName"), filter.getContextName()));
        }

        // Owner filter (optional)
        if (filter.getOwner() != null && !filter.getOwner().isEmpty()) {
            predicates.add(cb.equal(root.get("owner"), filter.getOwner()));
        }

        // User filter (optional) - check if user is in accessUsers JSON array
        if (filter.getUser() != null && !filter.getUser().isEmpty()) {
            Expression<String> jsonAsText = root.get("accessUsersJson").as(String.class);
            predicates.add(cb.like(jsonAsText, "%" + filter.getUser() + "%"));
        }

        // Access roles filter (optional) - check if any role is in accessGroups JSON array
        if (filter.getAccessRoles() != null && !filter.getAccessRoles().isEmpty()) {
            List<Predicate> rolePredicates = new ArrayList<>();
            for (String role : filter.getAccessRoles()) {
                Expression<String> jsonAsText = root.get("accessGroupsJson").as(String.class);
                rolePredicates.add(cb.like(jsonAsText, "%" + role + "%"));
            }
            predicates.add(cb.or(rolePredicates.toArray(new Predicate[0])));
        }

        return predicates.toArray(new Predicate[0]);
    }
}
