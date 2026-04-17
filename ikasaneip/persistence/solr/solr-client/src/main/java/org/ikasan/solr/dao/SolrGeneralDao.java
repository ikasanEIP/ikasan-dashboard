package org.ikasan.solr.dao;

import org.ikasan.solr.model.IkasanSolrDocument;

import java.util.List;
import java.util.Set;

/**
 * Created by Ikasan Development Team on 04/08/2017.
 */
public interface SolrGeneralDao<RESULTS, DOCUMENT>
{
    /**
     * Perform general search against ikasan solr index.
     *
     * @param moduleName
     * @param flowNames
     * @param searchString
     * @param startTime
     * @param endTime
     * @param resultSize
     * @param negateQuery
     * @param sortField
     * @param sortOrder
     * @return RESULTS
     */
    RESULTS search(Set<String> moduleName, Set<String> flowNames, String searchString, long startTime
        , long endTime, int resultSize, boolean negateQuery, String sortField, String sortOrder);


    /**
     * Perform general search against ikasan solr index.
     *
     * @param moduleName
     * @param flowNames
     * @param searchString
     * @param startTime
     * @param endTime
     * @param resultSize
     * @param entityTypes
     * @param negateQuery
     * @param sortField
     * @param sortOrder
     * @return RESULTS
     */
    RESULTS search(Set<String> moduleName, Set<String> flowNames, String searchString, long startTime
        , long endTime, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder);

    /**
     * Perform general search against ikasan solr index.
     *
     * @param moduleName
     * @param flowNames
     * @param componentNames
     * @param eventId
     * @param searchString
     * @param startTime
     * @param endTime
     * @param offset
     * @param resultSize
     * @param entityTypes
     * @param negateQuery
     * @param sortField
     * @param sortOrder
     * @return
     */
    RESULTS search(Set<String> moduleName, Set<String> flowNames, Set<String> componentNames, String eventId
        , String searchString, long startTime, long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery
        , String sortField, String sortOrder);



    /**
     * Perform general search against ikasan solr index.
     *
     * @param searchString
     * @param startTime
     * @param endTime
     * @param resultSize
     * @param entityTypes
     * @param negateQuery
     * @param sortField
     * @param sortOrder
     * @return RESULTS
     */
    RESULTS search(String searchString, long startTime, long endTime, int resultSize, List<String> entityTypes, boolean negateQuery
        , String sortField, String sortOrder);

    /**
     * Perform general search against ikasan solr index.
     *
     * @param searchString
     * @param startTime
     * @param endTime
     * @param offset
     * @param resultSize
     * @param entityTypes
     * @param negateQuery
     * @param sortField
     * @param sortOrder
     * @return RESULTS
     */
    RESULTS search(String searchString, long startTime, long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery
        , String sortField, String sortOrder);

    /**
     * Set the solr username
     *
     * @param solrUsername
     */
    void setSolrUsername(String solrUsername);


    /**
     * Set the solr password
     *
     * @param solrPassword
     */
    void setSolrPassword(String solrPassword);

    /**
     * Method to remove expired records from the solr index.
     */
    void removeExpired();

    /**
     * Method to find a document in the solr index by type and id.
     *
     * @param type
     * @param id
     */
    DOCUMENT findById(String type, String id);

    /**
     * Method to find a document in the solr index by type and id.
     *
     * @param type
     * @param uri
     */
    DOCUMENT findByErrorUri(String type, String uri);

    /**
     * Method to remove records from the solr index by type and id.
     *
     * @param type
     * @param id
     */
    void removeById(String type, String id);

    /**
     * Save or update an IkasanSolrDocument
     *
     * @param ikasanSolrDocument
     */
    void saveOrUpdate(IkasanSolrDocument ikasanSolrDocument);

    /**
     * Save or update a list of IkasanSolrDocument
     *
     * @param ikasanSolrDocuments
     */
    void saveOrUpdate(List<IkasanSolrDocument> ikasanSolrDocuments);

    /**
     * Backs up the Solr index to a specified location with a specified number of backups to keep.
     *
     * @param backupLocationPath The path where the backup of the index should be stored
     * @param numberOfBackupsToKeep The number of backup copies of the index to keep
     */
    void backupIndex(String backupLocationPath, int numberOfBackupsToKeep);
}
