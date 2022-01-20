package org.ikasan.scheduler.core.component.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.ikasan.scheduler.core.model.status.ContextInstanceStatus;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.scheduler.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
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
