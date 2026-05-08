package org.ikasan.dashboard.beans;

import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.security.dao.SolrSecurityDaoImpl;
import org.ikasan.security.dao.SolrUserDaoImpl;
import org.ikasan.security.service.*;
import org.ikasan.security.service.authentication.AuthenticationProviderFactory;
import org.ikasan.security.service.authentication.AuthenticationProviderFactoryImpl;
import org.ikasan.security.service.authentication.CustomAuthenticationProvider;
import org.ikasan.spec.security.dao.SecurityDao;
import org.ikasan.spec.security.dao.UserDao;
import org.ikasan.spec.security.service.AuthenticationService;
import org.ikasan.spec.security.service.SecurityService;
import org.ikasan.spec.security.service.UserService;
import org.ikasan.spec.systemevent.SystemEventService;
import org.ikasan.systemevent.SystemEventAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ImportResource( {
    "classpath:datasource-conf.xml",
    "classpath:transaction-conf.xml"
} )
@Import({SystemEventAutoConfiguration.class})
public class IkasanSecurityConfiguration
{
    @Value("${hibernate.show_sql:false}")
    private boolean hibernateShowSql;

    @Value("${hibernate.transaction.jta.platform:org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform}")
    private String hibernateTransactionJtaPlatform;

    @Value("${hibernate.event.merge.entity_copy_observer:allow}")
    private String hibernateEventMergeEntityCopyObserver;

    @Value("${com.sun.jndi.ldap.connect.timeout.milliseconds:60000}")
    private int ldapConnectTimeoutMilliseconds;

    @Value("${com.sun.jndi.ldap.read.timeout.milliseconds:60000}")
    private int ldapReadTimeoutMilliseconds;

    @Bean
    public SecurityService securityService(SecurityDao securityDao) {
        return new SecurityServiceImpl(securityDao);
    }

    @Bean
    public UserService userService(UserDao userDao, SecurityService securityService)
    {
        return new UserServiceImpl(userDao, securityService, passwordEncoder(), false);
    }

    @Bean
    public AuthenticationService authenticationService(SecurityService securityService, UserService userService) {

        AuthenticationProviderFactory authenticationProviderFactory = new AuthenticationProviderFactoryImpl(userService,securityService);
        return new AuthenticationServiceImpl(authenticationProviderFactory,securityService);
    }

    @Bean
    public AuthenticationProvider ikasanAuthenticationProvider(AuthenticationService authenticationService) {
        return new CustomAuthenticationProvider(authenticationService);
    }

    @Bean
    public LdapService ldapService(SecurityDao securityDao, UserDao userDao, PasswordEncoder passwordEncoder) {
        return new LdapServiceImpl(securityDao, userDao, passwordEncoder
            , this.ldapReadTimeoutMilliseconds, this.ldapConnectTimeoutMilliseconds);
    }

    @Bean
    public AuthenticationProviderFactory authenticationProviderFactory(UserService userService, SecurityService securityService) {
        return new AuthenticationProviderFactoryImpl(userService, securityService);
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SystemEventLogger systemEventLogger(SystemEventService systemEventService)
    {
        return new SystemEventLogger(systemEventService);
    }


    @Bean(name = "entityManagerFactory") // todo work out why we need a been named entity manager factory in the context
    public LocalContainerEntityManagerFactoryBean configurationServiceEntityManager(@Qualifier("ikasan.ds")DataSource dataSource
        , JpaVendorAdapter jpaVendorAdapter, @Qualifier("platformJpaProperties")Properties platformJpaProperties) {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean
            = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setDataSource(dataSource);
        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        localContainerEntityManagerFactoryBean.setJpaProperties(platformJpaProperties);
        localContainerEntityManagerFactoryBean.setPersistenceUnitName("configuration-service");
        localContainerEntityManagerFactoryBean.setPersistenceXmlLocation("classpath:/configuration-service-persistence.xml");

        return localContainerEntityManagerFactoryBean;
    }

    @Bean
    Properties platformJpaProperties() {
        Properties platformJpaProperties = new Properties();
        platformJpaProperties.put("hibernate.show_sql", hibernateShowSql);
        platformJpaProperties.put("hibernate.hbm2ddl.auto", "none");
        platformJpaProperties.put("hibernate.transaction.jta.platform",
            hibernateTransactionJtaPlatform);
        platformJpaProperties.put("hibernate.event.merge.entity_copy_observer",
            hibernateEventMergeEntityCopyObserver);

        return platformJpaProperties;
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter
            = new HibernateJpaVendorAdapter();

        return hibernateJpaVendorAdapter;
    }
}
