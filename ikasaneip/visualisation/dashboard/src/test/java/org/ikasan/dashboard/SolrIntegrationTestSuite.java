package org.ikasan.dashboard;

import org.ikasan.dashboard.backup.schedule.SolrIndexBackupJobTest;
import org.ikasan.dashboard.cluster.config.LeaderElectionConfigurationTest;
import org.ikasan.dashboard.cluster.config.ZooKeeperLeaderElectionPropertiesTest;
import org.ikasan.dashboard.cluster.health.LeaderElectionHealthIndicatorTest;
import org.ikasan.dashboard.cluster.service.ZooKeeperLeaderElectionServiceTest;
import org.ikasan.dashboard.notification.business.stream.BusinessStreamNotificationJobTest;
import org.ikasan.dashboard.notification.business.stream.service.BusinessStreamNotificationServiceTest;
import org.ikasan.dashboard.notification.scheduler.SchedulerNotificationJobTest;
import org.ikasan.dashboard.notification.scheduler.service.SchedulerNotificationServiceTest;
import org.ikasan.dashboard.security.schedule.LdapDirectorySynchronisationJobTest;
import org.ikasan.dashboard.ui.administration.filter.*;
import org.ikasan.dashboard.ui.administration.util.ConfigurationChangedSystemEventFormatterTest;
import org.ikasan.dashboard.ui.administration.view.*;
import org.ikasan.dashboard.ui.broadcast.FlowStateBroadcasterTest;
import org.ikasan.dashboard.ui.general.component.EventLifeIdDeepLinkViewTest;
import org.ikasan.dashboard.ui.general.component.FilteringGridTest;
import org.ikasan.dashboard.ui.general.component.HospitalViewTest;
import org.ikasan.dashboard.ui.general.component.SearchResultTest;
import org.ikasan.dashboard.ui.layout.IkasanAppLayoutTest;
import org.ikasan.dashboard.ui.scheduler.DurationFormatUtilsTest;
import org.ikasan.dashboard.ui.scheduler.component.*;
import org.ikasan.dashboard.ui.scheduler.service.ContextTemplateDraw2dAdapterTest;
import org.ikasan.dashboard.ui.scheduler.service.ContextTemplateToDagConverterTest;
import org.ikasan.dashboard.ui.scheduler.view.SchedulerViewTest;
import org.ikasan.dashboard.ui.search.component.SearchFormTest;
import org.ikasan.dashboard.ui.search.component.SearchFilteringGridTest;
import org.ikasan.dashboard.ui.search.view.SearchViewTest;
import org.ikasan.dashboard.ui.visualisation.adapter.service.ModuleDraw2DAdapterTest;
import org.ikasan.dashboard.ui.visualisation.component.FlowFilteringGridTest;
import org.ikasan.dashboard.ui.visualisation.component.ModuleFilteringGridTest;
import org.ikasan.dashboard.ui.visualisation.dao.ModuleMetaDataDaoImplTest;
import org.ikasan.dashboard.ui.visualisation.layout.IkasanFlowLayoutManagerTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.AceEditorLogConsumerTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobContextMenuTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerJobLogFileViewerDialogTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.CanvasJsonToContextTemplateAdapterTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextDraw2DAdapterTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.LogStreamerTest;
import org.ikasan.dashboard.ui.visualisation.view.BusinessStreamViewTest;
import org.ikasan.dashboard.ui.visualisation.view.ModuleVisualisationViewTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@RunWith(Suite.class)

@Suite.SuiteClasses({
    ZooKeeperLeaderElectionPropertiesTest.class,
    ZooKeeperLeaderElectionServiceTest.class,
    LeaderElectionConfigurationTest.class,
    LeaderElectionHealthIndicatorTest.class,
    BusinessStreamNotificationServiceTest.class,
    BusinessStreamNotificationJobTest.class,
    SchedulerNotificationJobTest.class,
    SchedulerNotificationServiceTest.class,
    SchedulerStatusWidgetTest.class,
    ContextInstanceWidgetTest.class,
    ContextInstanceDashboardWidgetTest.class,
    FilteringGridTest.class,
    GroupFilterTest.class,
    ModuleFilterTest.class,
    PolicyFilterTest.class,
    RoleFilterTest.class,
    RoleModuleFilterTest.class,
    UserFilterTest.class,
    UserLightFilterTest.class,
    GroupManagementViewTest.class,
    PolicyManagementViewTest.class,
    RoleManagementViewTest.class,
    UserDirectoriesViewTest.class,
    UserManagementViewTest.class,
    FlowStateBroadcasterTest.class,
    SearchViewTest.class,
    SearchFilteringGridTest.class,
    SearchFormTest.class,
    ModuleDraw2DAdapterTest.class,
    ModuleMetaDataDaoImplTest.class,
    IkasanFlowLayoutManagerTest.class,
    BusinessStreamViewTest.class,
    HospitalViewTest.class,
    SearchResultTest.class,
    EventLifeIdDeepLinkViewTest.class,
    ModuleFilteringGridTest.class,
    FlowFilteringGridTest.class,
    SolrIndexBackupJobTest.class,
    LdapDirectorySynchronisationJobTest.class,
    ModuleVisualisationViewTest.class,
    SystemEventSearchViewTest.class,
    AdministrationSearchViewTest.class,
    IkasanAppLayoutTest.class,
    SchedulerViewTest.class,
    ConfigurationChangedSystemEventFormatterTest.class,
    JobContextMenuTest.class,
    SchedulerJobLogFileViewerDialogTest.class,
    AceEditorLogConsumerTest.class,
    CanvasJsonToContextTemplateAdapterTest.class,
    ContextDraw2DAdapterTest.class,
    ContextTemplateWidgetTest.class,
    AgentWidgetTest.class,
    ContextTemplateManagementWidgetTest.class,
    FileWatcherJobDialogTest.class,
    ContextTemplateDraw2dAdapterTest.class,
    ContextTemplateToDagConverterTest.class,
    DurationFormatUtilsTest.class,
    LogStreamerTest.class
})
public class SolrIntegrationTestSuite {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.autoconfigure.exclude", () -> "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration," +
            "org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration," +
            "org.springframework.boot.actuate.autoconfigure.ldap.LdapHealthContributorAutoConfiguration," +
            "org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration," +
            "org.springframework.boot.actuate.autoconfigure.observation.web.client.HttpClientObservationsAutoConfiguration," +
            "org.ikasan.backup.IkasanBackupAutoConfiguration," +
            "org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration," +
            "org.ikasan.solr.initialisation.SolrInitialisationAutoConfiguration");
    }
}