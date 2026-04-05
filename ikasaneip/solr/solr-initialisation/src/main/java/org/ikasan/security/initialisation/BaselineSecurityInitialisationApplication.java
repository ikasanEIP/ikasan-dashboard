package org.ikasan.security.initialisation;

import org.ikasan.SolrClientAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(SolrClientAutoConfiguration.class)
public class BaselineSecurityInitialisationApplication implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(BaselineSecurityInitialisationApplication.class);

    private final BaselineSecurityDataLoader dataLoader;

    public BaselineSecurityInitialisationApplication(BaselineSecurityDataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public static void main(String[] args) {
        logger.info("Starting Baseline Security Initialisation Application...");
        SpringApplication.run(BaselineSecurityInitialisationApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Executing baseline security data load...");
        dataLoader.loadBaselineData();
        logger.info("Baseline security data load completed successfully!");
    }
}
