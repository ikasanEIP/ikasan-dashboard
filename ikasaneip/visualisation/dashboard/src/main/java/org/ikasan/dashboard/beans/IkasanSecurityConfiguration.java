package org.ikasan.dashboard.beans;

import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.security.SecurityAutoConfiguration;
import org.ikasan.security.dao.SecurityDao;
import org.ikasan.security.dao.UserDao;
import org.ikasan.security.service.*;
import org.ikasan.security.service.authentication.AuthenticationProviderFactory;
import org.ikasan.security.service.authentication.AuthenticationProviderFactoryImpl;
import org.ikasan.security.service.authentication.CustomAuthenticationProvider;
import org.ikasan.spec.systemevent.SystemEventService;
import org.ikasan.systemevent.SystemEventAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.access.method.P;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ImportResource( {
    "classpath:datasource-conf.xml",
    "classpath:transaction-conf.xml"
} )
@Import({SecurityAutoConfiguration.class, SystemEventAutoConfiguration.class})
public class IkasanSecurityConfiguration
{
    @Bean
    @Primary
    public UserService userService(UserDao userDao, SecurityService securityService, PasswordEncoder passwordEncoder)
    {
        return new UserServiceImpl(userDao, securityService, passwordEncoder, false);
    }

    @Bean
    public SystemEventLogger systemEventLogger(SystemEventService systemEventService)
    {
        return new SystemEventLogger(systemEventService);
    }


    @Bean(name = "entityManagerFactory") // todo work out why we need a been named entity manager factory in the context
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("ikasan.ds")DataSource dataSource
        , JpaVendorAdapter jpaVendorAdapter, @Qualifier("platformJpaProperties") Properties platformJpaProperties) {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean
            = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setDataSource(dataSource);
        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        localContainerEntityManagerFactoryBean.setJpaProperties(platformJpaProperties);
        localContainerEntityManagerFactoryBean.setPersistenceUnitName("security");
        localContainerEntityManagerFactoryBean.setPersistenceXmlLocation("classpath:security-persistence.xml");

        return localContainerEntityManagerFactoryBean;
    }

    @Bean
    public AuthenticationService authenticationService(SecurityService securityService, UserService userService){

        AuthenticationProviderFactory authenticationProviderFactory = new AuthenticationProviderFactoryImpl(userService,securityService);
        return new AuthenticationServiceImpl(authenticationProviderFactory,securityService);
    }

    @Bean
    public AuthenticationProvider ikasanAuthenticationProvider(AuthenticationService authenticationService){

        return new CustomAuthenticationProvider(authenticationService);

    }

    @Bean
    public LdapService ldapService(SecurityDao securityDao, UserDao userDao, PasswordEncoder passwordEncoder)
    {
        return new LdapServiceImpl(securityDao, userDao, passwordEncoder);
    }

    @Bean
    public AuthenticationProviderFactory authenticationProviderFactory(UserService userService, SecurityService securityService)
    {
        return new AuthenticationProviderFactoryImpl(userService, securityService);
    }

    @Bean
    Properties platformJpaProperties() {
        Properties platformJpaProperties = new Properties();
        platformJpaProperties.put("hibernate.show_sql", false);
        platformJpaProperties.put("hibernate.hbm2ddl.auto", "none");
        platformJpaProperties.put("hibernate.transaction.jta.platform",
            "org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform");
        platformJpaProperties.put("hibernate.event.merge.entity_copy_observer", "allow");

        return platformJpaProperties;
    }
}
