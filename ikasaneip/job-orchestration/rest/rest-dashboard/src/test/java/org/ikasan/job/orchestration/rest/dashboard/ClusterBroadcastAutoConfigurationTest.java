package org.ikasan.job.orchestration.rest.dashboard;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ClusterBroadcastAutoConfigurationTest {

    // Numeric IPs are used for peer URLs throughout these tests so that InetAddress.getAllByName()
    // returns immediately without a DNS lookup. Non-existent hostnames would each block for up to
    // the DNS timeout (default ~2 s) making the suite unnecessarily slow.
    private static final String PEER1 = "http://192.168.1.101:9090";
    private static final String PEER2 = "http://192.168.1.102:9090";

    private ClusterBroadcastAutoConfiguration uut;

    @Before
    public void setUp() {
        uut = new ClusterBroadcastAutoConfiguration();
        ReflectionTestUtils.setField(uut, "serverPort", 9090);
    }

    @Test
    public void test_empty_property_returns_empty_list() {
        ReflectionTestUtils.setField(uut, "clusterPeerUrls", "");
        assertTrue(uut.clusterPeerUrls().isEmpty());
    }

    @Test
    public void test_self_reference_via_localhost_is_excluded() {
        ReflectionTestUtils.setField(uut, "clusterPeerUrls", "http://localhost:9090");
        assertTrue(uut.clusterPeerUrls().isEmpty());
    }

    @Test
    public void test_self_reference_via_loopback_ip_is_excluded() {
        ReflectionTestUtils.setField(uut, "clusterPeerUrls", "http://127.0.0.1:9090");
        assertTrue(uut.clusterPeerUrls().isEmpty());
    }

    @Test
    public void test_self_reference_via_local_hostname_is_excluded() throws UnknownHostException {
        String localHostname = InetAddress.getLocalHost().getHostName().toLowerCase();
        ReflectionTestUtils.setField(uut, "clusterPeerUrls", "http://" + localHostname + ":9090");
        assertTrue(uut.clusterPeerUrls().isEmpty(), "Expected URL referencing local hostname to be excluded");
    }

    @Test
    public void test_localhost_on_different_port_is_kept() {
        ReflectionTestUtils.setField(uut, "clusterPeerUrls", "http://localhost:9091");
        List<String> urls = uut.clusterPeerUrls();
        assertEquals(1, urls.size());
        assertEquals("http://localhost:9091", urls.get(0));
    }

    @Test
    public void test_peer_url_on_different_host_is_kept() {
        ReflectionTestUtils.setField(uut, "clusterPeerUrls", PEER1);
        List<String> urls = uut.clusterPeerUrls();
        assertEquals(1, urls.size());
        assertEquals(PEER1, urls.get(0));
    }

    @Test
    public void test_mix_excludes_self_and_keeps_peers() {
        ReflectionTestUtils.setField(uut, "clusterPeerUrls",
            "http://localhost:9090," + PEER1 + "," + PEER2);
        List<String> urls = uut.clusterPeerUrls();
        assertEquals(2, urls.size());
        assertFalse(urls.contains("http://localhost:9090"));
        assertTrue(urls.contains(PEER1));
        assertTrue(urls.contains(PEER2));
    }

    @Test
    public void test_multiple_self_references_all_excluded() {
        ReflectionTestUtils.setField(uut, "clusterPeerUrls",
            "http://localhost:9090,http://127.0.0.1:9090");
        assertTrue(uut.clusterPeerUrls().isEmpty());
    }

    @Test
    public void test_whitespace_around_urls_is_trimmed() {
        ReflectionTestUtils.setField(uut, "clusterPeerUrls",
            " " + PEER1 + " , " + PEER2 + " ");
        List<String> urls = uut.clusterPeerUrls();
        assertEquals(2, urls.size());
        assertTrue(urls.contains(PEER1));
        assertTrue(urls.contains(PEER2));
    }

    @Test
    public void test_exact_duplicate_peer_url_is_deduplicated() {
        ReflectionTestUtils.setField(uut, "clusterPeerUrls", PEER1 + "," + PEER1);
        List<String> urls = uut.clusterPeerUrls();
        assertEquals(1, urls.size());
        assertEquals(PEER1, urls.get(0));
    }

    @Test
    public void test_multiple_duplicate_peer_urls_are_deduplicated() {
        ReflectionTestUtils.setField(uut, "clusterPeerUrls",
            PEER1 + "," + PEER2 + "," + PEER1 + "," + PEER2);
        List<String> urls = uut.clusterPeerUrls();
        assertEquals(2, urls.size());
        assertTrue(urls.contains(PEER1));
        assertTrue(urls.contains(PEER2));
    }

    @Test
    public void test_mixed_case_duplicate_peer_url_is_deduplicated() {
        ReflectionTestUtils.setField(uut, "clusterPeerUrls",
            "http://192.168.1.101:9090,http://192.168.1.101:9090");
        List<String> urls = uut.clusterPeerUrls();
        assertEquals(1, urls.size());
    }

    @Test
    public void test_unresolvable_hostname_is_included_in_list() {
        // An unresolvable hostname should remain in the peer list (DNS may be temporarily
        // unavailable at startup) but a warning is logged. It is not silently swallowed.
        ReflectionTestUtils.setField(uut, "clusterPeerUrls",
            "http://this-host-does-not-exist.invalid:9090");
        List<String> urls = uut.clusterPeerUrls();
        assertEquals(1, urls.size());
        assertEquals("http://this-host-does-not-exist.invalid:9090", urls.get(0));
    }
}
