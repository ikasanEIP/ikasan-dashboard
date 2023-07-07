/*
 * $Id$
 * $URL$
 *
 * ====================================================================
 * Ikasan Enterprise Integration Platform
 *
 * Distributed under the Modified BSD License.
 * Copyright notice: The copyright for this software and a full listing
 * of individual contributors are as shown in the packaged copyright.txt
 * file.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  - Neither the name of the ORGANIZATION nor the names of its contributors may
 *    be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 */
/*
 * $Id$
 * $URL$
 *
 * ====================================================================
 * Ikasan Enterprise Integration Platform
 *
 * Distributed under the Modified BSD License.
 * Copyright notice: The copyright for this software and a full listing
 * of individual contributors are as shown in the packaged copyright.txt
 * file.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  - Neither the name of the ORGANIZATION nor the names of its contributors may
 *    be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 */
package org.ikasan.job.orchestration.integration.inbound.component;

import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.builder.BuilderFactory;
import org.ikasan.component.endpoint.bigqueue.serialiser.SimpleStringSerialiser;
import org.ikasan.job.orchestration.integration.inbound.component.endpoint.ScheduleProcessInboundProducer;
import org.ikasan.job.orchestration.integration.inbound.component.endpoint.configuration.ScheduleProcessInboundProducerConfiguration;
import org.ikasan.spec.component.endpoint.Consumer;
import org.ikasan.spec.component.endpoint.Producer;
import org.ikasan.spec.error.reporting.ErrorReportingService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.annotation.Resource;

/**
 * Scheduler Agent component factory.
 *
 * @author Ikasan Development Team
 */
@Configuration
@ConditionalOnProperty(value="is.ikasan.enterprise.scheduler.instance", havingValue = "true")
public class ScheduledProcessEventInboundFlowComponentFactory
{
    @Value( "${module.name}" )
    private String moduleName;

    @Resource
    private IBigQueue inboundQueue;

    @Value("${scheduler.inbound.producer.ignore.errors:false}")
    private boolean schedulerInboundProducerIgnoreErrors;

    @Value("${scheduler.inbound.producer.log.details:false}")
    private boolean schedulerInboundProducerLogDetails;

    @Resource
    BuilderFactory builderFactory;

    @Resource
    JtaTransactionManager transactionManager;

    @Resource
    ErrorReportingService errorReportingService;

    @Resource
    ScheduledContextInstanceService scheduledContextInstanceService;

    @Resource
    ContextInstancePublicationService contextInstancePublicationService;

    @Resource
    ModuleMetaDataService moduleMetadataService;

    @DependsOn("inboundQueue")
    public Consumer getInboundBigQueueConsumer() {
        return builderFactory.getComponentBuilder().bigQueueConsumer()
            .setInboundQueue(inboundQueue)
            .setPutErrorsToBackOfQueue(true)
            .setSerialiser(new SimpleStringSerialiser())
            .build();
    }

    /**
     * Get the producer that publishes ScheduledProcessEvents.
     *
     * @return
     */
    public Producer getScheduledStatusProducer()
    {
        ScheduleProcessInboundProducerConfiguration configuration
            = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(this.schedulerInboundProducerIgnoreErrors);
        configuration.setLogDetails(this.schedulerInboundProducerLogDetails);
        ScheduleProcessInboundProducer producer
            = new ScheduleProcessInboundProducer(this.transactionManager.getTransactionManager(),
            scheduledContextInstanceService, contextInstancePublicationService, moduleMetadataService);
        producer.setConfiguration(configuration);
        producer.setConfiguredResourceId("scheduleProcessInboundProducer");
        producer.setErrorReportingService(errorReportingService);
        return producer;
    }

}

