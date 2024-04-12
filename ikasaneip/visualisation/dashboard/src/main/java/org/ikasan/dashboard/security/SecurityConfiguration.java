package org.ikasan.dashboard.security;

import org.ikasan.rest.dashboard.JwtAuthenticationEntryPoint;
import org.ikasan.rest.dashboard.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.annotation.Resource;

/**
 * Extended to handle multi-http security described by
 * https://docs.spring.io/spring-security/site/docs/3.2.x/reference/htmlsingle/html5/#multiple-httpsecurity
 *
 * First order will allow httpBasic to be run on specific url patterns via ikasan authentication.
 *
 * Second order configures spring security, doing the following:
 * <li>Bypass security checks for static resources,</li>
 * <li>Restrict access to the application, allowing only logged in users,</li>
 * <li>Set up the login form</li>
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity (prePostEnabled = true)
@EnableMethodSecurity
public class SecurityConfiguration
{
    private static final String LOGIN_PROCESSING_URL = "/login";

    private static final String LOGIN_FAILURE_URL = "/login";

    private static final String LOGIN_URL = "/login";

    private static final String LOGOUT_SUCCESS_URL = "/login";

    @Resource
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Resource
    private JwtRequestFilter jwtRequestFilter;

    @Bean(name = "authenticationManager")
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean(name = "securityContextRepository")
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }


    @Configuration
    @DependsOn({"authenticationManager", "securityContextRepository"})
    public class IkasanSecurityConfigurationAdapter
    {

        @Resource
        AuthenticationManager authenticationManager;

        /**
         * Require login to access internal pages and configure login form.
         */
        @DependsOn({"authenticationManager", "securityContextRepository"})
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.setSharedObject(AuthenticationManager.class, this.authenticationManager);

            // Not using Spring CSRF here to be able to use plain HTML for the login page
            http.csrf(httpSecurityCsrfConfigurer -> httpSecurityCsrfConfigurer.disable())
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry
                    -> authorizationManagerRequestMatcherRegistry
                    .requestMatchers("/rest/export/context/**", // ContextExportControl
                        "/rest/module/bigQueue/size/all/**", // BigQueueModuleController
                        "/rest/context/status/**", // ContextStatusServiceController
                        "/actuator/**"// expose spring actuator via basic authentication
                    )
                )
                .httpBasic(httpSecurityHttpBasicConfigurer -> httpSecurityHttpBasicConfigurer.configure(http))

                // Register our CustomRequestCache, that saves unauthorized access attempts, so
                // the user is redirected after login.
                .requestCache(c -> c.requestCache(new CustomRequestCache()))

                // Restrict access to our application.
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry
                    -> authorizationManagerRequestMatcherRegistry
                    .requestMatchers(SecurityUtils::isFrameworkInternalRequest)
                    .permitAll()
                    .requestMatchers("/", "/VAADIN/**",
                        // the standard favicon URI
                        "/favicon.ico",
                        // the robots exclusion standard
                        "/robots.txt",
                        // web application manifest
                        "/manifest.webmanifest",
                        "/sw.js",
                        "/offline-page.html",
                        // icons and images
                        "/icons/**",
                        "/images/**",
                        // (development mode) static resources
                        "/frontend/**",
                        // (development mode) webjars
                        "/webjars/**",
                        // (development mode) H2 debugging console
                        "/h2-console/**",
                        "/swagger-ui/**",
                        // (production mode) static resources
                        "/frontend-es5/**",
                        "/frontend-es6/**",
                        "/actuator/**")
                    .permitAll()
                    .requestMatchers("/authenticate").permitAll()
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/actuator/**").permitAll()
                    // Allow all requests by logged in users.
                    .anyRequest().authenticated()
                )
                // Allow all flow internal requests.
                // Configure the login page.
                .formLogin(httpSecurityFormLoginConfigurer -> {
                    httpSecurityFormLoginConfigurer.loginPage(LOGIN_URL)
                        .permitAll()
                        .loginProcessingUrl(LOGIN_PROCESSING_URL)
                        .failureUrl(LOGIN_FAILURE_URL);
                })

                // Configure logout
                .logout(httpSecurityLogoutConfigurer -> {
                    httpSecurityLogoutConfigurer.logoutSuccessUrl(LOGOUT_SUCCESS_URL);
                })
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(jwtAuthenticationEntryPoint, new AntPathRequestMatcher("/rest/**")))
                /**
                 * Session Management should be set to stateless for JWT token, but due to VAADIN utilising
                 * cookies we cannot do that
                 */
                // Add a filter to validate the tokens with every request
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(httpSecurityExceptionHandlingConfigurer
                    -> httpSecurityExceptionHandlingConfigurer.defaultAuthenticationEntryPointFor(new IkasanAuthenticationEntryPoint()
                        , new AntPathRequestMatcher("/**")))

                .securityContext((securityContext) -> {
                        securityContext.requireExplicitSave(true);
                        securityContext.securityContextRepository(securityContextRepository());
                    }
                );

            return http.build();
        }
    }

}
