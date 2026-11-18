package org.ikasan.data.synchronisation;

import org.ikasan.spec.metadata.dao.BusinessStreamMetadataDao;
import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.security.dao.AuthenticationMethodDao;
import org.ikasan.spec.security.dao.IkasanPrincipalDao;
import org.ikasan.spec.security.dao.PolicyDao;
import org.ikasan.spec.security.dao.RoleDao;
import org.ikasan.spec.security.dao.UserDao;

/**
 * The Mongo DAOs used to read entities whose Mongo document does not implement the interface the Solr DAO takes
 * (security records and metadata). Reading them through the Mongo DAO reuses its conversion exactly as the
 * application reads them, rather than duplicating it here.
 */
public record MongoLookups(UserDao userDao,
                           IkasanPrincipalDao principalDao,
                           RoleDao roleDao,
                           PolicyDao policyDao,
                           AuthenticationMethodDao authenticationMethodDao,
                           ModuleMetadataDao moduleMetadataDao,
                           BusinessStreamMetadataDao businessStreamMetadataDao,
                           ComponentConfigurationMetadataDao componentConfigurationMetadataDao) {
}
