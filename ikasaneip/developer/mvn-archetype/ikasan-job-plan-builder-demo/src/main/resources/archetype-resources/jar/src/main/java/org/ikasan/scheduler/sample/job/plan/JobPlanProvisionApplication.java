package org.ikasan.scheduler.sample.job.plan;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.job.orchestration.util.ContextImportZipUtils;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootApplication
public class JobPlanProvisionApplication implements CommandLineRunner {

    private Logger logger = LoggerFactory.getLogger(JobPlanProvisionApplication.class);

    private static final String BUILD_JOB_PLAN_BUNDLE = "BUILD_JOB_PLAN_BUNDLE";
    private static final String BUILD_AND_DEPLOY_JOB_PLAN_BUNDLE = "BUILD_AND_DEPLOY_JOB_PLAN_BUNDLE";
    private static final String DEPLOY_JOB_PLAN_BUNDLE = "DEPLOY_JOB_PLAN_BUNDLE";

    @Autowired
    private ApplicationContext applicationContext;

    @Resource
    private ContextProvisionService contextProvisionService;

    @Value("${zip.output.dir}")
    private String zipOutputDir;

    public static void main(String[] args) {
        SpringApplication.run(JobPlanProvisionApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if(args == null || args.length == 0) {
            this.provisionPlan(this.buildPlan());
        }
        else if (args.length > 0) {
            if(args.length > 2) {
                logger.error("Too many arguments!");
            }
            
            String action = args[0];

            if(action.equals(BUILD_JOB_PLAN_BUNDLE)) {
                this.buildPlan();
            }
            else if(action.equals(BUILD_AND_DEPLOY_JOB_PLAN_BUNDLE)) {
                this.provisionPlan(this.buildPlan());
            }
            else if(action.equals(DEPLOY_JOB_PLAN_BUNDLE)) {
                this.provisionPlan(this.loadContextBundle(args[1]));
            }
        }

        int exitCode = SpringApplication.exit(applicationContext, () -> 0);

        logger.info("Process Complete!");

        System.exit(exitCode);
    }

    /**
     * Delegate to the job plan builder service to build a context bundle.
     * @return
     * @throws JsonProcessingException
     */
    private ContextBundle buildPlan() throws JsonProcessingException {
        JobPlanBuilderService jobPlanBuilder = new JobPlanBuilderService(zipOutputDir);
        return jobPlanBuilder.buildContext();
    }

    /**
     * Provision the job plan.
     *
     * @param contextBundle
     */
    private void provisionPlan(ContextBundle contextBundle) {
        logger.info("Provisioning Context Bundle");
        contextProvisionService.provisionContext(contextBundle);
        logger.info("Finished Provisioning Context Bundle");
    }

    private ContextBundle loadContextBundle(String path) throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(path);
        return  ContextImportZipUtils.extractZipFile(fileInputStream);
    }
}