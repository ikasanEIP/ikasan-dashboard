package org.ikasan.dashboard.security.schedule;

import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.spec.security.model.AuthenticationMethod;
import org.ikasan.security.service.LdapService;
import org.ikasan.security.service.LdapServiceException;
import org.ikasan.spec.security.service.SecurityService;
import org.ikasan.spec.scheduler.DashboardJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.Date;

public class LdapDirectorySynchronisationJob implements DashboardJob {

    private static Logger logger = LoggerFactory.getLogger(LdapDirectorySynchronisationJob.class);

    private AuthenticationMethod authenticationMethod;

    private LdapService ldapService;
    private SecurityService securityService;
    private SystemEventLogger systemEventLogger;
    private String timezone = ZoneId.systemDefault().getId();


    /**
     * Constructor for LdapDirectorySynchronisationJob class.
     *
     * @param authenticationMethod The authentication method for LDAP synchronization.
     * @param ldapService The LDAP service to use for synchronization.
     * @param securityService The security service for handling authentication methods.
     * @param systemEventLogger The system event logger for logging events.
     * @throws IllegalArgumentException if any of the parameters are null.
     */
    public LdapDirectorySynchronisationJob(AuthenticationMethod authenticationMethod, LdapService ldapService,
                                           SecurityService securityService, SystemEventLogger systemEventLogger) {
        this.authenticationMethod = authenticationMethod;
        if(this.authenticationMethod == null) {
            throw new IllegalArgumentException("authenticationMethod cannot be null!");
        }
        this.ldapService = ldapService;
        if(this.ldapService == null) {
            throw new IllegalArgumentException("ldapService cannot be null!");
        }
        this.securityService = securityService;
        if(this.securityService == null) {
            throw new IllegalArgumentException("securityService cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
    }


    /**
     * Constructor for LdapDirectorySynchronisationJob class.
     *
     * @param authenticationMethod The authentication method for LDAP synchronization.
     * @param ldapService The LDAP service to use for synchronization.
     * @param securityService The security service for handling authentication methods.
     * @param timezone The timezone to be set for the job.
     * @param systemEventLogger The system event logger for logging events.
     * @throws IllegalArgumentException if timezone is null.
     */
    public LdapDirectorySynchronisationJob(AuthenticationMethod authenticationMethod, LdapService ldapService,
                                           SecurityService securityService, String timezone, SystemEventLogger systemEventLogger) {
        this(authenticationMethod, ldapService, securityService, systemEventLogger);
        this.timezone = timezone;
        if(this.timezone == null) {
            throw new IllegalArgumentException("timezone cannot be null!");
        }
    }


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            logger.info("Running ldap synchronisation " + authenticationMethod.getName());
            this.systemEventLogger.logEvent(SystemEventConstants.LDAP_REPOSITORY_SYNCHRONISATION_JOB_START,
                String.format("Running ldap synchronisation [%s]", authenticationMethod.getName()), "system");
            this.ldapService.synchronize(authenticationMethod);
            // refresh the authentication method to make sure we are not saving
            // any stale values.
            this.authenticationMethod = this.securityService
                .getAuthenticationMethod(this.authenticationMethod.getId());
            this.authenticationMethod.setLastSynchronised(new Date());
            this.securityService.saveOrUpdateAuthenticationMethod(authenticationMethod);
            logger.info("Finished running ldap synchronisation " + authenticationMethod.getName());
            this.systemEventLogger.logEvent(SystemEventConstants.LDAP_REPOSITORY_SYNCHRONISATION_JOB_COMPLETE,
                String.format("Finished running ldap synchronisation [%s]", authenticationMethod.getName()), "system");
        }
        catch (LdapServiceException e) {
            this.systemEventLogger.logEvent(SystemEventConstants.LDAP_REPOSITORY_SYNCHRONISATION_JOB_ERROR,
                String.format("Error running ldap synchronisation [%s]. Error message[%s]!"
                    , authenticationMethod.getName(), e.getMessage()), "system");
            logger.error("Error running ldap synchronisation " + authenticationMethod.getName(), e);
            throw new JobExecutionException(e);
        }
    }

    @Override
    public String getJobName() {
        return this.authenticationMethod.getName();
    }

    @Override
    public String getCronExpression() {
        return this.authenticationMethod.getSynchronisationCronExpression();
    }

    @Override
    public String getTimezone() {
        return this.timezone;
    }
}
