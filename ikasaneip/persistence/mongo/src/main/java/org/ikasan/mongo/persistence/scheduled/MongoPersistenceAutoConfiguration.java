package org.ikasan.mongo.persistence.scheduled;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.ikasan.mongo.persistence.scheduled.context.dao.MongoScheduledContextDaoImpl;
import org.ikasan.mongo.persistence.scheduled.context.repository.MongoScheduledContextRecordRepository;
import org.ikasan.mongo.persistence.scheduled.instance.dao.MongoSchedulerJobInstanceDaoImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoSchedulerJobInstanceRecordRepository;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {
    "org.ikasan.mongo.persistence.scheduled.context.repository",
    "org.ikasan.mongo.persistence.scheduled.instance.repository"
})
public class MongoPersistenceAutoConfiguration {

    @Bean
    public MongoClient mongoClient(@Value("${spring.data.mongodb.uri}") String connectionString) {
        return MongoClients.create(connectionString);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient,
                                        @Value("${spring.data.mongodb.database:test}") String databaseName) {
        return new MongoTemplate(mongoClient, databaseName);
    }

    @Bean
    public ScheduledContextDao scheduledContextDao(MongoScheduledContextRecordRepository repository,
                                                    MongoTemplate mongoTemplate) {
        return new MongoScheduledContextDaoImpl(repository, mongoTemplate);
    }

    @Bean
    public SchedulerJobInstanceDao schedulerJobInstanceDao(MongoSchedulerJobInstanceRecordRepository repository,
                                                           MongoTemplate mongoTemplate) {
        return new MongoSchedulerJobInstanceDaoImpl(repository, mongoTemplate);
    }
}
