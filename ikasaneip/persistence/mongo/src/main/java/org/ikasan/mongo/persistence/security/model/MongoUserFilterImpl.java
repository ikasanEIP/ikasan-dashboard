package org.ikasan.mongo.persistence.security.model;

import org.ikasan.spec.security.model.UserFilter;

/**
 * Solr implementation of UserFilter.
 *
 * This class provides filtering capabilities for users including
 * filtering by username, email, first name, last name, department,
 * and sorting options.
 *
 * @author Ikasan Development Team
 */
public class MongoUserFilterImpl implements UserFilter {

    private String usernameFilter;
    private String emailFilter;
    private String nameFilter;
    private String lastNameFilter;
    private String departmentFilter;
    private String sortOrder;
    private String sortColumn;

    /**
     * Default constructor.
     */
    public MongoUserFilterImpl() {
    }

    @Override
    public String getNameFilter() {
        return nameFilter;
    }

    @Override
    public String getLastNameFilter() {
        return lastNameFilter;
    }

    @Override
    public void setNameFilter(String nameFilter) {
        this.nameFilter = nameFilter;
    }

    @Override
    public void setLastNameFilter(String lastNameFilter) {
        this.lastNameFilter = lastNameFilter;
    }

    @Override
    public String getUsernameFilter() {
        return usernameFilter;
    }

    @Override
    public void setUsernameFilter(String usernameFilter) {
        this.usernameFilter = usernameFilter;
    }

    @Override
    public String getEmailFilter() {
        return emailFilter;
    }

    @Override
    public void setEmailFilter(String emailFilter) {
        this.emailFilter = emailFilter;
    }

    @Override
    public String getDepartmentFilter() {
        return departmentFilter;
    }

    @Override
    public void setDepartmentFilter(String departmentFilter) {
        this.departmentFilter = departmentFilter;
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
}
