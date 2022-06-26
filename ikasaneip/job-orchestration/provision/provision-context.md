![IKASAN](../../developer/docs/quickstart-images/Ikasan-title-transparent.png)
# Provisioning Same Context for Local Development Environment
1. From Ikasan Core deploy a scheduler-agent to your local machine - https://github.com/ikasanEIP/ikasan/tree/3.3.x/ikasaneip/ootb/module/scheduler-agent
2. Configure the scheduler agent to point at your local dashbaord. For example: 
    ```
    module.name=scheduler-agent
    module.jar.name=scheduler-agent
    
    # standard dirs
    persistence.dir=./persistence
    lib.dir=./lib
    
    # Logging levels across packages (optional)
    logging.level.root=WARN
    logging.level.org.ikasan=INFO
    
    # Blue console servlet settings (optional)
    server.error.whitelabel.enabled=false
    
    module.java.command=java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8765 -server -Xms512m -Xmx512m -XX:MaxMetaspaceSize=196m -Dspring.jta.logDir=${persistence.dir}/${module.name}-ObjectStore -Dorg.apache.activemq.SERIALIZABLE_PACKAGES=* -Dmodule.name=
    ${module.name} -jar ${lib.dir}/${module.name}-*.jar
    
    server.tomcat.max-swallow-size=10MB
    
    # Web Bindings
    h2.db.port=19082
    server.port=19080
    server.address=localhost
    server.servlet.context-path=/scheduler-agent
    server.tomcat.additional-tld-skip-patterns=xercesImpl.jar,xml-apis.jar,serializer.jar
    spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration,,me.snowdrop.boot.narayana.autoconfigure.NarayanaConfiguration,org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration
    
    spring.liquibase.change-log=classpath:db-changelog-scheduler-agent.xml
    spring.liquibase.enabled=true
    
    # health probs and remote management (optional)
    management.endpoints.enabled-by-default=false
    management.endpoint.info.enabled=true
    management.endpoint.health.enabled=true
    management.endpoint.logfile.enabled=true
    management.endpoints.web.exposure.include=info,health,logfile
    management.endpoint.shutdown.enabled=true
    #management.endpoints.web.base-path=/rest
    
    #management.endpoint.health.probes.enabled=true
    management.endpoint.health.show-details=always
    management.endpoint.health.show-components=always
    management.health.jms.enabled=false
    
    # Ikasan persistence store
    datasource.username=sa
    datasource.password=sa
    datasource.driver-class-name=org.h2.Driver
    datasource.xadriver-class-name=org.h2.jdbcx.JdbcDataSource
    datasource.url=jdbc:h2:tcp://localhost:${h2.db.port}/${persistence.dir}/${module.name}-db/esb;IFEXISTS=FALSE
    #datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    datasource.dialect=org.hibernate.dialect.H2Dialect
    datasource.show-sql=false
    datasource.hbm2ddl.auto=none
    datasource.validationQuery=select 1
    
    # Dashboard data extraction settings
    ikasan.dashboard.extract.enabled=true
    ikasan.dashboard.extract.base.url=http://localhost:9090
    ikasan.dashboard.extract.username=admin
    ikasan.dashboard.extract.password=admin
    
    ikasan.exceptions.retry-configs.[0].className=org.ikasan.spec.component.endpoint.EndpointException
    ikasan.exceptions.retry-configs.[0].delayInMillis=5000
    ikasan.exceptions.retry-configs.[0].maxRetries=-1
    
    ikasan.exceptions.excludedClasses[0]=org.ikasan.spec.component.transformation.TransformationException
    
    big.queue.consumer.inboundQueueName=module-inbound-context-queue
    big.queue.consumer.outboundQueueName=module-outbound-context-queue
    big.queue.consumer.queueDir=/sandbox/mick/bigquque
    
    module.rest.connection.readTimeout=300000
    module.rest.connection.connectTimeout=300000
    module.rest.connection.connectionRequestTimeout=300000
    
    scheduler.agent.log.folder=./target/logs
    scheduler.agent.log.folder.parenthesis=/
    
    # Housekeep Log Files Flow
    housekeep.scheduled.consumer.cron=20 20 03 * * ?
    housekeep.log.files.process.log-folder=${scheduler.agent.log.folder}
    housekeep.log.files.process.ttl.days=25
    housekeep.log.files.process.should-archive=false
    housekeep.log.files.process.should-move=false
    housekeep.log.files.process.move-folder=
    
    job.monitoring.broker.timeout.minutes=240
    
    context.instance.recovery.active=false
    ```
3. Enable the unit test - [ContextProvisionHelperTest.java](./src/test/java/org/ikasan/job/orchestration/provision/context/ContextProvisionHelperTest.java)
4. Run the test method.
    ```
    public void provision_jobs()
   ```
5. All sample context data is found [here](./src/test/resources/data/full-context).