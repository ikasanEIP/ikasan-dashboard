package org.ikasan.orchestration.service.context.status;

import org.ikasan.orchestration.service.context.reset.ContextResetServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContextServiceAutoConfiguration {

    @Bean
    public ContextStatusServiceImpl contextStatusService() {
        return new ContextStatusServiceImpl();
    }

    @Bean
    public ContextResetServiceImpl contextResetService() {
        return new ContextResetServiceImpl();
    }
}
