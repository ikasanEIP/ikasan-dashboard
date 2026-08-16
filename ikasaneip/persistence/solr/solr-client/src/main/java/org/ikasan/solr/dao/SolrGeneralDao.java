package org.ikasan.solr.dao;

import org.apache.solr.client.solrj.SolrClient;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.spec.search.dao.ESBSearchDao;

import java.util.List;
import java.util.Set;

/**
 * Created by Ikasan Development Team on 04/08/2017.
 */
public interface SolrGeneralDao<RESULTS, DOCUMENT> extends ESBSearchDao<RESULTS, DOCUMENT>
{
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

    /**
     * Sets the Solr client for interacting with the Solr server.
     *
     * @param solrClient the SolrClient instance to be used for Solr operations
     */
    void setSolrClient(SolrClient solrClient);
}
