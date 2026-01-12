package org.ikasan.dashboard.security.schedule;

import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.security.model.AuthenticationMethod;
import org.ikasan.security.service.LdapService;
import org.ikasan.security.service.LdapServiceException;
import org.ikasan.security.service.SecurityService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

public class LdapDirectorySynchronisationJobTest {

    private Mockery mockery = new Mockery()
    {
        {
            setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
            setThreadingPolicy(new Synchroniser());
        }
    };

    private AuthenticationMethod authenticationMethod = mockery.mock(AuthenticationMethod.class);
    private LdapService ldapService = mockery.mock(LdapService.class);
    private SecurityService securityService = mockery.mock(SecurityService.class);
    private JobExecutionContext jobExecutionContext = mockery.mock(JobExecutionContext.class);
    private SystemEventLogger systemEventLogger = mockery.mock(SystemEventLogger.class);

    @Test
    public void test_synchronise_success() throws JobExecutionException, LdapServiceException {
        LdapDirectorySynchronisationJob job = new LdapDirectorySynchronisationJob(authenticationMethod,
            ldapService, securityService, systemEventLogger);

        mockery.checking(new Expectations(){{
            oneOf(authenticationMethod).getName();
            will(returnValue("ldap repo"));
            oneOf(ldapService).synchronize(authenticationMethod);
            oneOf(authenticationMethod).setLastSynchronised(with(any(Date.class)));
            oneOf(authenticationMethod).getId();
            will(returnValue(1L));
            oneOf(securityService).getAuthenticationMethod(1L);
            will(returnValue(authenticationMethod));
            oneOf(securityService).saveOrUpdateAuthenticationMethod(authenticationMethod);
            exactly(3).of(authenticationMethod).getName();
            will(returnValue("ldap repo"));
            exactly(2).of(systemEventLogger).logEvent(with(any(String.class)), with(any(String.class))
                , with(any(String.class)));
        }});

        job.execute(jobExecutionContext);

        mockery.assertIsSatisfied();
    }
}
