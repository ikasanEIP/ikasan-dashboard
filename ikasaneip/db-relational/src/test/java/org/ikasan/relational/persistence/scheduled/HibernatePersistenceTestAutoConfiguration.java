package org.ikasan.relational.persistence.scheduled;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.jta.UserTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@ImportResource("/transactions.xml")
public class HibernatePersistenceTestAutoConfiguration
{
    @Value("${postgres.url}")
    private String postgresUrl;

    @Bean(name = {"ikasan.xads", "ikasan.ds"})
    public DataSource ikasanDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(this.postgresUrl);
        dataSource.setUsername("testuser");
        dataSource.setPassword("testpass");
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        TransactionManagerImple transactionManagerImple = new TransactionManagerImple();
        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager
            (UserTransaction.userTransaction(), transactionManagerImple);

        return jtaTransactionManager;
    }

    @Bean
    Properties platformJpaProperties() {
        Properties platformJpaProperties = new Properties();
        platformJpaProperties.put("hibernate.show_sql", false);
        platformJpaProperties.put("hibernate.hbm2ddl.auto", "create-drop");
        platformJpaProperties.put("hibernate.transaction.jta.platform",
            "org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform");

        return platformJpaProperties;
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter
            = new HibernateJpaVendorAdapter();

        return hibernateJpaVendorAdapter;
    }
}
