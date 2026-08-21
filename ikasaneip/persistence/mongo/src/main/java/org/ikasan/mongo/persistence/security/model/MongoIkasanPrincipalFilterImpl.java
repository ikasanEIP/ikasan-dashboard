package org.ikasan.mongo.persistence.security.model;

import org.ikasan.spec.security.model.IkasanPrincipalFilter;

/**
 * Solr implementation of IkasanPrincipalFilter.
 *
 * This class provides filtering capabilities for Ikasan principals including
 * filtering by name, type, description, and sorting options.
 *
 * @author Ikasan Development Team
 */
public class MongoIkasanPrincipalFilterImpl implements IkasanPrincipalFilter {

    private String nameFilter;
    private String typeFilter;
    private String descriptionFilter;
    private String sortOrder;
    private String sortColumn;

    /**
     * Default constructor.
     */
    public MongoIkasanPrincipalFilterImpl() {
    }

    @Override
    public String getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public String getSortColumn() {
        return sortColumn;
    }

    @Override
    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }

    @Override
    public String getNameFilter() {
        return nameFilter;
    }

    @Override
    public void setNameFilter(String nameFilter) {
        this.nameFilter = nameFilter;
    }

    @Override
    public String getTypeFilter() {
        return typeFilter;
    }

    @Override
    public void setTypeFilter(String typeFilter) {
        this.typeFilter = typeFilter;
    }

    @Override
    public String getDescriptionFilter() {
        return descriptionFilter;
    }

    @Override
    public void setDescriptionFilter(String descriptionFilter) {
        this.descriptionFilter = descriptionFilter;
    }
}
