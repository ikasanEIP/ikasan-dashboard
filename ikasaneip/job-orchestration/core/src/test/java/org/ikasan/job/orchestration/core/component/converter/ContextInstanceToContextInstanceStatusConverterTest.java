package org.ikasan.job.orchestration.core.component.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.core.AbstractTest;
import org.ikasan.job.orchestration.model.status.ContextInstanceStatus;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.junit.Test;

import java.io.IOException;

public class ContextInstanceToContextInstanceStatusConverterTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextInstanceToContextInstanceStatusConverter converter = new ContextInstanceToContextInstanceStatusConverter();
        ContextInstanceStatus contextInstanceStatus = converter.convert(context);

        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        // todo some assertions
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextInstanceStatus));
    }
}
