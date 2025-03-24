package org.ikasan.dashboard.security.schedule;

import org.ikasan.security.model.AuthenticationMethod;
import org.ikasan.security.service.LdapService;
import org.ikasan.security.service.LdapServiceException;
import org.ikasan.security.service.SecurityService;
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
    private String timezone = ZoneId.systemDefault().getId();

    public LdapDirectorySynchronisationJob(AuthenticationMethod authenticationMethod, LdapService ldapService,
                                           SecurityService securityService) {
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
    }

    public LdapDirectorySynchronisationJob(AuthenticationMethod authenticationMethod, LdapService ldapService,
                                           SecurityService securityService, String timezone) {
        this(authenticationMethod, ldapService, securityService);
        this.timezone = timezone;
        if(this.timezone == null) {
            throw new IllegalArgumentException("timezone cannot be null!");
        }
    }


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            logger.info("Running ldap synchronisation " + authenticationMethod.getName());
            this.ldapService.synchronize(authenticationMethod);
            // refresh the authentication method to make sure we are not saving
            // any stale values.
            this.authenticationMethod = this.securityService
                .getAuthenticationMethod(this.authenticationMethod.getId());
            this.authenticationMethod.setLastSynchronised(new Date());
            this.securityService.saveOrUpdateAuthenticationMethod(authenticationMethod);
            logger.info("Finished running ldap synchronisation " + authenticationMethod.getName());
        }
        catch (LdapServiceException e) {
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
