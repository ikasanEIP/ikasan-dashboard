package org.ikasan.relational.persistence;

import org.ikasan.relational.persistence.module.metadata.dao.HibernateModuleMetadataDaoImpl;
import org.ikasan.relational.persistence.scheduled.context.dao.HibernateScheduledContextDaoImpl;
import org.ikasan.relational.persistence.scheduled.instance.dao.HibernateScheduledContextInstanceAuditAggregateDaoImpl;
import org.ikasan.relational.persistence.scheduled.instance.dao.HibernateScheduledContextInstanceAuditDaoImpl;
import org.ikasan.relational.persistence.scheduled.instance.dao.HibernateScheduledContextInstanceDaoImpl;
import org.ikasan.relational.persistence.scheduled.instance.dao.HibernateSchedulerJobInstanceDaoImpl;
import org.ikasan.relational.persistence.scheduled.job.dao.*;
import org.ikasan.relational.persistence.scheduled.joblock.dao.HibernateJobLockCacheAuditDaoImpl;
import org.ikasan.relational.persistence.scheduled.joblock.dao.HibernateJobLockCacheDaoImpl;
import org.ikasan.relational.persistence.scheduled.notification.dao.HibernateEmailNotificationContextDaoImpl;
import org.ikasan.relational.persistence.scheduled.notification.dao.HibernateEmailNotificationDetailsDaoImpl;
import org.ikasan.relational.persistence.scheduled.notification.dao.HibernateNotificationSendAuditDaoImpl;
import org.ikasan.relational.persistence.scheduled.profile.dao.HibernateContextProfileDaoImpl;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.job.dao.*;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationContextDao;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationDetailsDao;
import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@DependsOn({"jpaVendorAdapter", "platformJpaProperties"})
public class HibernatePersistenceAutoConfiguration {

    @Value("${system.event.expiry.minutes:10080}")
    private long systemEventExpiryMinutes;

    @Value("${system.event.housekeeping.batch.size:100}")
    private int systemEventHouseKeepingBatchSize;

    @Value("${system.event.transaction.batch.size:1000}")
    private int systemEventTransactionBatchSize;

    @Value("${systemEventServiceHousekeepingJob-deleteOnceHarvested:false}")
    private boolean deleteOnceHarvested;


    @Bean(name = "scheduledContextDao")
    @DependsOn("hibernateEntityManager")
    public ScheduledContextDao scheduledContextDao() {
        return new HibernateScheduledContextDaoImpl();
    }

    @Bean(name = "scheduledContextInstanceDao")
    @DependsOn("hibernateEntityManager")
    public ScheduledContextInstanceDao scheduledContextInstanceDao() {
        return new HibernateScheduledContextInstanceDaoImpl();
    }

    @Bean(name = "schedulerJobInstanceDao")
    @DependsOn("hibernateEntityManager")
    public SchedulerJobInstanceDao schedulerJobInstanceDao() {
        return new HibernateSchedulerJobInstanceDaoImpl();
    }

    @Bean(name = "scheduledContextInstanceAuditDao")
    @DependsOn("hibernateEntityManager")
    public ScheduledContextInstanceAuditDao scheduledContextInstanceAuditDao() {
        return new HibernateScheduledContextInstanceAuditDaoImpl();
    }

    @Bean(name = "scheduledContextInstanceAuditAggregateDao")
    @DependsOn("hibernateEntityManager")
    public ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao() {
        return new HibernateScheduledContextInstanceAuditAggregateDaoImpl();
    }

    @Bean(name = "bridgingJobDao")
    @DependsOn("hibernateEntityManager")
    public BridgingJobDao bridgingJobDao() {
        return new HibernateBridgingJobDaoImpl();
    }

    @Bean(name = "contextStartJobDao")
    @DependsOn("hibernateEntityManager")
    public ContextStartJobDao contextStartJobDao() {
        return new HibernateContextStartJobDaoImpl();
    }

    @Bean(name = "contextTerminalJobDao")
    @DependsOn("hibernateEntityManager")
    public ContextTerminalJobDao contextTerminalJobDao() {
        return new HibernateContextTerminalJobDaoImpl();
    }

    @Bean(name = "fileEventDrivenJobDao")
    @DependsOn("hibernateEntityManager")
    public FileEventDrivenJobDao fileEventDrivenJobDao() {
        return new HibernateFileEventDrivenJobDaoImpl();
    }

    @Bean(name = "quartzScheduleDrivenJobDao")
    @DependsOn("hibernateEntityManager")
    public QuartzScheduleDrivenJobDao quartzScheduleDrivenJobDao() {
        return new HibernateQuartzScheduleDrivenJobDaoImpl();
    }

    @Bean(name = "globalEventJobDao")
    @DependsOn("hibernateEntityManager")
    public GlobalEventJobDao globalEventJobDao() {
        return new HibernateGlobalEventJobDaoImpl();
    }

    @Bean(name = "internalEventDrivenJobDao")
    @DependsOn("hibernateEntityManager")
    public InternalEventDrivenJobDao internalEventDrivenJobDao() {
        return new HibernateInternalEventDrivenJobDaoImpl();
    }

    @Bean(name = "schedulerJobDao")
    @DependsOn("hibernateEntityManager")
    public SchedulerJobDao schedulerJobDao() {
        return new HibernateSchedulerJobDaoImpl();
    }

    @Bean(name = "jobLockCacheDao")
    @DependsOn("hibernateEntityManager")
    public JobLockCacheDao jobLockCacheDao() {
        return new HibernateJobLockCacheDaoImpl();
    }

    @Bean(name = "jobLockCacheAuditDao")
    @DependsOn("hibernateEntityManager")
    public JobLockCacheAuditDao jobLockCacheAuditDao() {
        return new HibernateJobLockCacheAuditDaoImpl();
    }

    @Bean(name = "emailNotificationContextDao")
    @DependsOn("hibernateEntityManager")
    public EmailNotificationContextDao emailNotificationContextDao() {
        return new HibernateEmailNotificationContextDaoImpl();
    }

    @Bean(name = "emailNotificationDetailsDao")
    @DependsOn("hibernateEntityManager")
    public EmailNotificationDetailsDao emailNotificationDetailsDao() {
        return new HibernateEmailNotificationDetailsDaoImpl();
    }

    @Bean(name = "notificationSendAuditDao")
    @DependsOn("hibernateEntityManager")
    public NotificationSendAuditDao notificationSendAuditDao() {
        return new HibernateNotificationSendAuditDaoImpl();
    }

    @Bean(name = "contextProfileDao")
    @DependsOn("hibernateEntityManager")
    public ContextProfileDao contextProfileDao() {
        return new HibernateContextProfileDaoImpl();
    }

    @Bean(name = "moduleMetadataDao")
    @DependsOn("hibernateEntityManager")
    public ModuleMetadataDao moduleMetadataDao() {
        return new HibernateModuleMetadataDaoImpl();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean hibernateEntityManager(@Qualifier("ikasan.xads")DataSource dataSource
        , JpaVendorAdapter jpaVendorAdapter, @Qualifier("platformJpaProperties")Properties platformJpaProperties) {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean
            = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setDataSource(dataSource);
        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        localContainerEntityManagerFactoryBean.setJpaProperties(platformJpaProperties);
        localContainerEntityManagerFactoryBean.setPersistenceUnitName("hibernate-persistence");
        localContainerEntityManagerFactoryBean.setPersistenceXmlLocation("classpath:hibernate-persistence.xml");

        return localContainerEntityManagerFactoryBean;
    }
}
