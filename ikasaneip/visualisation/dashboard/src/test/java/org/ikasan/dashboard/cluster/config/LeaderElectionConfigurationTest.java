package org.ikasan.dashboard.cluster.config;

import org.ikasan.dashboard.cluster.service.LeaderElectionService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Unit test for LeaderElectionConfiguration.
 */
public class LeaderElectionConfigurationTest {

    private LeaderElectionService leaderElectionService;
    private LeaderElectionConfiguration configuration;

    @Before
    public void setUp() {
        leaderElectionService = mock(LeaderElectionService.class);
        configuration = new LeaderElectionConfiguration(leaderElectionService);
    }

    @Test
    public void testConfigurationCreation() {
        Assert.assertNotNull(configuration);
    }

    @Test
    public void testOnApplicationReadyStartsService() throws Exception {
        configuration.onApplicationReady();

        verify(leaderElectionService, times(1)).start();
    }

    @Test(expected = RuntimeException.class)
    public void testOnApplicationReadyThrowsExceptionOnFailure() throws Exception {
        doThrow(new RuntimeException("Connection failed")).when(leaderElectionService).start();

        configuration.onApplicationReady();

        verify(leaderElectionService, times(1)).start();
    }

    @Test
    public void testServiceInjection() {
        LeaderElectionService mockService = mock(LeaderElectionService.class);
        LeaderElectionConfiguration config = new LeaderElectionConfiguration(mockService);

        Assert.assertNotNull(config);
    }
}
