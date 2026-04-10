package org.ikasan.scheduled;

import org.ikasan.scheduled.context.dao.HibernateScheduledContextDaoImpl;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
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
