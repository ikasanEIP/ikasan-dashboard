package org.ikasan.dashboard.ui.administration.view;

import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.UITest;
import org.ikasan.dashboard.ui.administration.component.SystemEventDialog;
import org.ikasan.dashboard.ui.administration.component.SystemEventFilteringGrid;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.security.model.IkasanPrincipal;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.model.RoleModule;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventRecord;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.ikasan.systemevent.model.SolrSystemEventRecordImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.github.mvysny.kaributesting.v10.ButtonKt._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.any;

public class SystemEventSearchViewTest extends UITest {

    @MockitoBean
    private Set<IkasanPrincipal> principals;

    @MockitoBean
    private IkasanPrincipal principal;

    @MockitoBean
    private Set<Role> roles;

    @MockitoBean
    private Role role;

    @MockitoBean
    private Set<RoleModule> roleModules;

    @MockitoBean
    private RoleModule roleModule;

    @MockitoBean
    protected SystemEventSearchService systemEventSearchService;

    @Override
    public void setup_expectations() throws IOException {

    }

    @Test
    public void test_search_admin_user()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        Mockito.when(this.systemEventSearchService.findByFilter(Mockito.any(SystemEventSearchFilter.class), Mockito.anyInt()
                , Mockito.anyInt(), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(this.getSolrSystemEventsResults(25));

        UI.getCurrent().navigate("adminSearchView");

        SystemEventSearchView systemEventSearchView = _get(SystemEventSearchView.class);
        Assertions.assertNotNull(systemEventSearchView);

        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));
        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));

        SystemEventFilteringGrid searchResultsGrid = (SystemEventFilteringGrid) ReflectionTestUtils
            .getField(systemEventSearchView, "searchResultsGrid");

        Assert.assertEquals(25, searchResultsGrid.getResultSize());
    }

    @Test
    public void test_search_non_admin_user_with_one_associated_module()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(false);
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.SYSTEM_EVENT_READ))
            .thenReturn(true);
        Mockito.when(super.ikasanAuthentication.getPrincipal())
            .thenReturn(super.user);
        Mockito.when(super.user.getPrincipals())
            .thenReturn(this.principals);
        Mockito.doCallRealMethod().when(this.principals).forEach((any(Consumer.class)));
        Mockito.when(this.principals.iterator()).thenReturn(Set.of(principal).iterator(), Set.of(principal).iterator());
        Mockito.when(principal.getRoles()).thenReturn(this.roles);
        Mockito.doCallRealMethod().when(this.roles).forEach((any(Consumer.class)));
        Mockito.when(this.roles.iterator()).thenReturn(Set.of(role).iterator(), Set.of(role).iterator());
        Mockito.when(role.getRoleModules()).thenReturn(this.roleModules);
        Mockito.doCallRealMethod().when(this.roleModules).forEach((any(Consumer.class)));
        Mockito.when(this.roleModules.iterator()).thenReturn(Set.of(roleModule).iterator(), Set.of(roleModule).iterator());
        Mockito.when(this.roleModule.getModuleName()).thenReturn("testModuleName");
        Mockito.when(this.systemEventSearchService.findByFilter(Mockito.any(), Mockito.anyInt(), Mockito.anyInt()
                , Mockito.isNull(), Mockito.isNull()))
            .thenReturn(this.getSolrSystemEventsResults(10));

        UI.getCurrent().navigate("adminSearchView");

        SystemEventSearchView systemEventSearchView = _get(SystemEventSearchView.class);
        Assertions.assertNotNull(systemEventSearchView);

        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));
        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));

        SystemEventFilteringGrid searchResultsGrid = (SystemEventFilteringGrid) ReflectionTestUtils
            .getField(systemEventSearchView, "searchResultsGrid");

        Assert.assertEquals(10, searchResultsGrid.getResultSize());
    }

    @Test
    public void test_search_non_admin_user_with_no_associated_module()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(false);
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.SYSTEM_EVENT_READ))
            .thenReturn(true);
        Mockito.when(super.ikasanAuthentication.getPrincipal())
            .thenReturn(super.user);
        Mockito.when(super.user.getPrincipals())
            .thenReturn(this.principals);
        Mockito.doCallRealMethod().when(this.principals).forEach((any(Consumer.class)));
        Mockito.when(this.principals.iterator()).thenReturn(Set.of(principal).iterator(), Set.of(principal).iterator());
        Mockito.when(principal.getRoles()).thenReturn(this.roles);
        Mockito.doCallRealMethod().when(this.roles).forEach((any(Consumer.class)));
        Mockito.when(this.roles.iterator()).thenReturn(Set.of(role).iterator(), Set.of(role).iterator());
        Mockito.when(role.getRoleModules()).thenReturn(this.roleModules);
        Mockito.doCallRealMethod().when(this.roleModules).forEach((any(Consumer.class)));
        Mockito.when(this.roleModules.iterator()).thenReturn(new HashSet<RoleModule>().iterator(), new HashSet<RoleModule>().iterator());
        Mockito.when(this.systemEventSearchService.findByFilter(Mockito.any(), Mockito.anyInt(), Mockito.anyInt()
                , Mockito.isNull(), Mockito.isNull()))
            .thenReturn(this.getSolrSystemEventsResults(0));

        UI.getCurrent().navigate("adminSearchView");

        SystemEventSearchView systemEventSearchView = _get(SystemEventSearchView.class);
        Assertions.assertNotNull(systemEventSearchView);

        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));
        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));

        SystemEventFilteringGrid searchResultsGrid = (SystemEventFilteringGrid) ReflectionTestUtils
            .getField(systemEventSearchView, "searchResultsGrid");

        Assert.assertEquals(0, searchResultsGrid.getResultSize());
    }

    @Test
    public void test_search_and_open_result_dialog()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        Mockito.when(this.systemEventSearchService.findByFilter(Mockito.any(), Mockito.anyInt(), Mockito.anyInt()
                , Mockito.isNull(), Mockito.isNull()))
            .thenReturn(this.getSolrSystemEventsResults(1));

        UI.getCurrent().navigate("adminSearchView");

        SystemEventSearchView systemEventSearchView = _get(SystemEventSearchView.class);
        Assertions.assertNotNull(systemEventSearchView);

        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));
        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));

        SystemEventFilteringGrid searchResultsGrid = (SystemEventFilteringGrid) ReflectionTestUtils
            .getField(systemEventSearchView, "searchResultsGrid");

        GridKt._doubleClickItem(searchResultsGrid, 0);

        SystemEventDialog systemEventDialog = _get(SystemEventDialog.class);

        Assert.assertEquals("admin", ((TextField)ReflectionTestUtils
            .getField(systemEventDialog, "actionedByTf")).getValue());
        Assert.assertEquals("murex-trade-tradeConsumer", ((TextField)ReflectionTestUtils
            .getField(systemEventDialog, "contextTf")).getValue());
    }

    protected SearchResults<SystemEvent> getSolrSystemEventsResults(int size) {

        ArrayList<SystemEvent> ikasanSolrDocuments = new ArrayList<>();

        IntStream.range(0, size).forEach(i -> {
            SolrSystemEventRecordImpl document = new SolrSystemEventRecordImpl();
            document.setId("id" +i);
            document.setTimestampLong(1606203560055L);
            document.setPayload("{\"moduleName\":\"murex-trade\",\"action\":\"Configuration Updated OldConfig [{\\\"configurationId\\\":" +
                "\\\"murex-trade-tradeConsumer\\\",\\\"description\\\":null,\\\"parameters\\\":[{\\\"id\\\":54,\\\"name\\\":\\\"autoContentConversion\\\"" +
                ",\\\"value\\\":true,\\\"description\\\":null},{\\\"id\\\":55,\\\"name\\\":\\\"autoSplitBatch\\\",\\\"value\\\":true,\\\"description\\\":null}" +
                ",{\\\"id\\\":56,\\\"name\\\":\\\"batchMode\\\",\\\"value\\\":true,\\\"description\\\":null},{\\\"id\\\":57,\\\"name\\\":\\\"batchSize\\\"" +
                ",\\\"value\\\":0,\\\"description\\\":null},{\\\"id\\\":58,\\\"name\\\":\\\"cacheLevel\\\",\\\"value\\\":1,\\\"description\\\":null}" +
                ",{\\\"id\\\":59,\\\"name\\\":\\\"concurrentConsumers\\\",\\\"value\\\":1,\\\"description\\\":null},{\\\"id\\\":60,\\\"name\\\":" +
                "\\\"connectionFactoryJndiProperties\\\",\\\"value\\\":{\\\"java.naming.security.principal\\\":\\\"\\\",\\\"java.naming.factory.initial\\\":" +
                "\\\"org.apache.activemq.jndi.ActiveMQInitialContextFactory\\\",\\\"java.naming.provider.url\\\":" +
                "\\\"tcp://localhost:61616?jms.prefetchPolicy.all=1&jms.redeliveryPolicy.maximumRedeliveries=-1&jms.clientIDPrefix=murex-trade-tradeConsumer\\\"" +
                ",\\\"java.naming.security.credentials\\\":\\\"\\\"},\\\"description\\\":null},{\\\"id\\\":61,\\\"name\\\":\\\"connectionFactoryName\\\",\\\"value\\\":" +
                "\\\"XAConnectionFactory\\\",\\\"description\\\":null},{\\\"id\\\":62,\\\"name\\\":\\\"connectionFactoryPassword\\\",\\\"value\\\":null," +
                "\\\"description\\\":null},{\\\"id\\\":63,\\\"name\\\":\\\"connectionFactoryUsername\\\",\\\"value\\\":null,\\\"description\\\":null}," +
                "{\\\"id\\\":64,\\\"name\\\":\\\"destinationJndiName\\\",\\\"value\\\":\\\"dynamicQueues/com.caixa.bank.murex.out\\\",\\\"description\\\":" +
                "null},{\\\"id\\\":65,\\\"name\\\":\\\"destinationJndiProperties\\\",\\\"value\\\":{\\\"java.naming.security.principal\\\":\\\"\\\"," +
                "\\\"java.naming.factory.initial\\\":\\\"org.apache.activemq.jndi.ActiveMQInitialContextFactory\\\",\\\"java.naming.provider.url\\\":" +
                "\\\"tcp://localhost:61616?jms.prefetchPolicy.all=1&jms.redeliveryPolicy.maximumRedeliveries=-1&jms.clientIDPrefix=murex-trade-tradeConsumer\\\"," +
                "\\\"java.naming.security.credentials\\\":\\\"\\\"},\\\"description\\\":null},{\\\"id\\\":66,\\\"name\\\":\\\"durable\\\",\\\"value\\\":" +
                "true,\\\"description\\\":null},{\\\"id\\\":67,\\\"name\\\":\\\"durableSubscriptionName\\\",\\\"value\\\":\\\"murex-trade-tradeConsumer\\\"," +
                "\\\"description\\\":null},{\\\"id\\\":68,\\\"name\\\":\\\"maxConcurrentConsumers\\\",\\\"value\\\":1,\\\"description\\\":null},{\\\"id\\\":69," +
                "\\\"name\\\":\\\"pubSubDomain\\\",\\\"value\\\":false,\\\"description\\\":null},{\\\"id\\\":70,\\\"name\\\":\\\"sessionAcknowledgeMode\\\"," +
                "\\\"value\\\":null,\\\"description\\\":null},{\\\"id\\\":71,\\\"name\\\":\\\"sessionTransacted\\\",\\\"value\\\":false,\\\"description\\\":" +
                "null}]}] NewConfig [{\\\"configurationId\\\":\\\"murex-trade-tradeConsumer\\\",\\\"description\\\":null,\\\"parameters\\\":[{\\\"id\\\":54," +
                "\\\"name\\\":\\\"autoContentConversion\\\",\\\"value\\\":true,\\\"description\\\":null},{\\\"id\\\":55,\\\"name\\\":\\\"autoSplitBatch\\\"," +
                "\\\"value\\\":true,\\\"description\\\":null},{\\\"id\\\":56,\\\"name\\\":\\\"batchMode\\\",\\\"value\\\":true,\\\"description\\\":null}," +
                "{\\\"id\\\":57,\\\"name\\\":\\\"batchSize\\\",\\\"value\\\":0,\\\"description\\\":null},{\\\"id\\\":58,\\\"name\\\":\\\"cacheLevel\\\"," +
                "\\\"value\\\":1,\\\"description\\\":null},{\\\"id\\\":59,\\\"name\\\":\\\"concurrentConsumers\\\",\\\"value\\\":1,\\\"description\\\":null}," +
                "{\\\"id\\\":60,\\\"name\\\":\\\"connectionFactoryJndiProperties\\\",\\\"value\\\":{\\\"java.naming.security.principal\\\":\\\"\\\"," +
                "\\\"java.naming.factory.initial\\\":\\\"org.apache.activemq.jndi.ActiveMQInitialContextFactory\\\",\\\"java.naming.provider.url\\\":" +
                "\\\"tcp://localhost:61616?jms.prefetchPolicy.all=1&jms.redeliveryPolicy.maximumRedeliveries=-1&jms.clientIDPrefix=murex-trade-tradeConsumer\\\"," +
                "\\\"java.naming.security.credentials\\\":\\\"\\\"},\\\"description\\\":null},{\\\"id\\\":61,\\\"name\\\":\\\"connectionFactoryName\\\"," +
                "\\\"value\\\":\\\"XAConnectionFactory\\\",\\\"description\\\":null},{\\\"id\\\":62,\\\"name\\\":\\\"connectionFactoryPassword\\\",\\\"value\\\":" +
                "null,\\\"description\\\":null},{\\\"id\\\":63,\\\"name\\\":\\\"connectionFactoryUsername\\\",\\\"value\\\":null,\\\"description\\\":null}," +
                "{\\\"id\\\":64,\\\"name\\\":\\\"destinationJndiName\\\",\\\"value\\\":\\\"dynamicQueues/com.caixa.bank.murex.out\\\",\\\"description\\\":" +
                "null},{\\\"id\\\":65,\\\"name\\\":\\\"destinationJndiProperties\\\",\\\"value\\\":{\\\"java.naming.security.principal\\\":\\\"\\\"," +
                "\\\"java.naming.factory.initial\\\":\\\"org.apache.activemq.jndi.ActiveMQInitialContextFactory\\\",\\\"java.naming.provider.url\\\":" +
                "\\\"tcp://localhost:61616?jms.prefetchPolicy.all=1&jms.redeliveryPolicy.maximumRedeliveries=-1&jms.clientIDPrefix=murex-trade-tradeConsumer\\\"," +
                "\\\"java.naming.security.credentials\\\":\\\"\\\"},\\\"description\\\":null},{\\\"id\\\":66,\\\"name\\\":\\\"durable\\\",\\\"value\\\":true," +
                "\\\"description\\\":null},{\\\"id\\\":67,\\\"name\\\":\\\"durableSubscriptionName\\\",\\\"value\\\":\\\"murex-trade-tradeConsumer\\\"," +
                "\\\"description\\\":null},{\\\"id\\\":68,\\\"name\\\":\\\"maxConcurrentConsumers\\\",\\\"value\\\":1,\\\"description\\\":null}," +
                "{\\\"id\\\":69,\\\"name\\\":\\\"pubSubDomain\\\",\\\"value\\\":false,\\\"description\\\":null},{\\\"id\\\":70,\\\"name\\\":" +
                "\\\"sessionAcknowledgeMode\\\",\\\"value\\\":null,\\\"description\\\":null},{\\\"id\\\":71,\\\"name\\\":\\\"sessionTransacted\\\"," +
                "\\\"value\\\":false,\\\"description\\\":null}]}]\",\"actor\":\"admin\",\"id\":57,\"subject\":\"murex-trade-tradeConsumer\",\"timestamp\":" +
                "1606807854254,\"expiry\":1607412654254}");

            ikasanSolrDocuments.add(document);
        });

        return new SearchResultsImpl<>(ikasanSolrDocuments
            , ikasanSolrDocuments.size(), 1);
    }

    @Test
    public void test_view_exists() {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        UI.getCurrent().navigate("adminSearchView");

        SystemEventSearchView systemEventSearchView = _get(SystemEventSearchView.class);
        Assertions.assertNotNull(systemEventSearchView);
    }

    @Test
    public void test_view_is_visible() {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        UI.getCurrent().navigate("adminSearchView");

        SystemEventSearchView systemEventSearchView = _get(SystemEventSearchView.class);
        Assertions.assertTrue(systemEventSearchView.isVisible());
    }

    @Test
    public void test_search_button_exists() {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        UI.getCurrent().navigate("adminSearchView");

        Button searchButton = _get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton"));
        Assertions.assertNotNull(searchButton);
    }

    @Test
    public void test_search_results_grid_initialized() {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        Mockito.when(this.systemEventSearchService.findByFilter(Mockito.any(SystemEventSearchFilter.class), Mockito.anyInt()
                , Mockito.anyInt(), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(this.getSolrSystemEventsResults(0));

        UI.getCurrent().navigate("adminSearchView");

        SystemEventSearchView systemEventSearchView = _get(SystemEventSearchView.class);

        SystemEventFilteringGrid searchResultsGrid = (SystemEventFilteringGrid) ReflectionTestUtils
            .getField(systemEventSearchView, "searchResultsGrid");

        Assertions.assertNotNull(searchResultsGrid);
    }

    @Test
    public void test_view_has_children() {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        UI.getCurrent().navigate("adminSearchView");

        SystemEventSearchView systemEventSearchView = _get(SystemEventSearchView.class);
        Assertions.assertTrue(systemEventSearchView.getChildren().count() > 0);
    }

    @Test
    public void test_search_with_zero_results() {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        Mockito.when(this.systemEventSearchService.findByFilter(Mockito.any(SystemEventSearchFilter.class), Mockito.anyInt()
                , Mockito.anyInt(), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(this.getSolrSystemEventsResults(0));

        UI.getCurrent().navigate("adminSearchView");

        SystemEventSearchView systemEventSearchView = _get(SystemEventSearchView.class);

        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));
        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));

        SystemEventFilteringGrid searchResultsGrid = (SystemEventFilteringGrid) ReflectionTestUtils
            .getField(systemEventSearchView, "searchResultsGrid");

        Assert.assertEquals(0, searchResultsGrid.getResultSize());
    }

    @Test
    public void test_search_with_multiple_results() {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        Mockito.when(this.systemEventSearchService.findByFilter(Mockito.any(SystemEventSearchFilter.class), Mockito.anyInt()
                , Mockito.anyInt(), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(this.getSolrSystemEventsResults(50));

        UI.getCurrent().navigate("adminSearchView");

        SystemEventSearchView systemEventSearchView = _get(SystemEventSearchView.class);

        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));
        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));

        SystemEventFilteringGrid searchResultsGrid = (SystemEventFilteringGrid) ReflectionTestUtils
            .getField(systemEventSearchView, "searchResultsGrid");

        Assert.assertEquals(50, searchResultsGrid.getResultSize());
    }

    @Test
    public void test_search_button_clickable() {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        Mockito.when(this.systemEventSearchService.findByFilter(Mockito.any(SystemEventSearchFilter.class), Mockito.anyInt()
                , Mockito.anyInt(), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(this.getSolrSystemEventsResults(5));

        UI.getCurrent().navigate("adminSearchView");

        Button searchButton = _get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton"));

        // Verify button is clickable
        _click(searchButton);
        Assertions.assertNotNull(searchButton);
    }

    @Test
    public void test_grid_is_populated_after_search() {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        Mockito.when(this.systemEventSearchService.findByFilter(Mockito.any(SystemEventSearchFilter.class), Mockito.anyInt()
                , Mockito.anyInt(), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(this.getSolrSystemEventsResults(15));

        UI.getCurrent().navigate("adminSearchView");

        SystemEventSearchView systemEventSearchView = _get(SystemEventSearchView.class);

        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));
        _click(_get(Button.class, spec -> spec.withId("systemEventSearchFormSearchButton")));

        SystemEventFilteringGrid searchResultsGrid = (SystemEventFilteringGrid) ReflectionTestUtils
            .getField(systemEventSearchView, "searchResultsGrid");

        // Verify grid has results
        Assertions.assertTrue(searchResultsGrid.getResultSize() > 0);
        Assert.assertEquals(15, searchResultsGrid.getResultSize());
    }

    @Test
    public void test_navigation_successful() {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        UI.getCurrent().navigate("adminSearchView");

        SystemEventSearchView systemEventSearchView = _get(SystemEventSearchView.class);
        Assertions.assertTrue(systemEventSearchView.isAttached());
    }

    @Test
    public void test_helper_method_creates_correct_results() {
        SearchResults<SystemEvent> results = getSolrSystemEventsResults(10);

        Assertions.assertNotNull(results);
        Assert.assertEquals(10, results.getResultList().size());
        Assert.assertEquals(10, results.getTotalNumberOfResults());
    }

    @Test
    public void test_helper_method_creates_empty_results() {
        SearchResults<SystemEvent> results = getSolrSystemEventsResults(0);

        Assertions.assertNotNull(results);
        Assert.assertEquals(0, results.getResultList().size());
        Assert.assertEquals(0, results.getTotalNumberOfResults());
    }
}
