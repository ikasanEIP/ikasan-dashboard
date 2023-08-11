package org.ikasan.job.orchestration.integration.inbound.component.endpoint;

import com.arjuna.ats.arjuna.coordinator.ActionStatus;
import com.arjuna.ats.jta.resources.LastResourceCommitOptimisation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.component.endpoint.bigqueue.message.BigQueueMessageImpl;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.integration.inbound.component.endpoint.configuration.ScheduleProcessInboundProducerConfiguration;
import org.ikasan.job.orchestration.integration.inbound.exception.InvalidContextInstanceIdException;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.instance.model.SolrContextInstanceSearchFilterImpl;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.component.endpoint.EndpointException;
import org.ikasan.spec.component.endpoint.Producer;
import org.ikasan.spec.configuration.ConfigurationException;
import org.ikasan.spec.configuration.ConfiguredResource;
import org.ikasan.spec.error.reporting.ErrorReportingService;
import org.ikasan.spec.error.reporting.IsErrorReportingServiceAware;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.HashMap;
import java.util.List;

public class ScheduleProcessInboundProducer implements Producer<String>, ConfiguredResource<ScheduleProcessInboundProducerConfiguration>, LastResourceCommitOptimisation, IsErrorReportingServiceAware {

    private static final String FLOW_NAME = "Scheduled Process Event Inbound Flow";
    private Logger logger = LoggerFactory.getLogger(ScheduleProcessInboundProducer.class);
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private ScheduleProcessInboundProducerConfiguration configuration;
    private String configurationId;
    private ScheduledProcessProducerConnectionCallback scheduledProcessProducerConnectionCallback;
    private TransactionManager transactionManager;
    private ErrorReportingService errorReportingService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ContextInstancePublicationService contextInstancePublicationService;
    private ModuleMetaDataService moduleMetadataService;

    public ScheduleProcessInboundProducer(TransactionManager transactionManager, ScheduledContextInstanceService scheduledContextInstanceService,
                                          ContextInstancePublicationService contextInstancePublicationService, ModuleMetaDataService moduleMetadataService) {
        this.transactionManager = transactionManager;
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.contextInstancePublicationService = contextInstancePublicationService;
        this.moduleMetadataService = moduleMetadataService;
    }

    @Override
    public void invoke(String payload) throws EndpointException {
        try {
            this.enlist();

            BigQueueMessage bigQueueMessage = objectMapper.readValue(payload, BigQueueMessageImpl.class);
            String message = (String) bigQueueMessage.getMessage();

            ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent
                = objectMapper.readValue(message, ContextualisedScheduledProcessEventImpl.class);

            // Warn on the dashboard but do not create an exclude event as we cannot resubmit a null context id.
            if(contextualisedScheduledProcessEvent.getContextInstanceId() == null) {
                String errorMessage = String.format("Received scheduler event with null context instance id [%s]." +
                    " Cache Contents - %s", contextualisedScheduledProcessEvent, ContextMachineCache.instance().toString());
                logger.warn(errorMessage);
                errorReportingService.notify(FLOW_NAME, payload, new InvalidContextInstanceIdException(errorMessage));
                this.scheduledProcessProducerConnectionCallback = new ScheduledProcessProducerConnectionCallbackImpl(payload, null);
                return;
            }

            ContextMachine contextMachine = ContextMachineCache.instance()
                .getByContextInstanceId(contextualisedScheduledProcessEvent.getContextInstanceId());

            if(contextMachine == null) {

                // before raising a hospital event, check the context instance id is terminated. If it is then send a notification to the agent
                // to remove the context instance and just write an error to the dashboard and do not raise a hospital event as we cannot do anything to the payload
                if (contextualisedScheduledProcessEvent.getContextName() != null) {

                    // Create a search query to find out the instance id from solr.
                    ContextInstanceSearchFilter filter = new SolrContextInstanceSearchFilterImpl();
                    filter.setContextSearchFilter(contextualisedScheduledProcessEvent.getContextName());
                    filter.setContextInstanceId(contextualisedScheduledProcessEvent.getContextInstanceId());
                    SearchResults<ScheduledContextInstanceRecord> contextInstanceRecords =
                        scheduledContextInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

                    // Check if we see a context instance id in solr with the instance ofs ENDED or COMPLETED, if so ignore the payload and send notification to the agent.
                    for (ScheduledContextInstanceRecord scheduledContextInstanceRecord : contextInstanceRecords.getResultList()) {
                        if (scheduledContextInstanceRecord.getContextName().equals(contextualisedScheduledProcessEvent.getContextName()) &&
                            scheduledContextInstanceRecord.getContextInstanceId().equals(contextualisedScheduledProcessEvent.getContextInstanceId()) &&
                            (scheduledContextInstanceRecord.getContextInstance().getStatus().equals(InstanceStatus.ENDED)) || (scheduledContextInstanceRecord.getContextInstance().getStatus().equals(InstanceStatus.COMPLETE))) {

                            String errorMessage = String.format("Context name[%s] and context instance id [%s] has the status of [%s], therefore this event will be discarded. No further action is required. " +
                                "Active ContextMachineCache Contents - %s", contextualisedScheduledProcessEvent.getContextName(),
                                contextualisedScheduledProcessEvent.getContextInstanceId(), scheduledContextInstanceRecord.getContextInstance().getStatus(),
                                ContextMachineCache.instance().toString());

                            this.removeAgentInstances(scheduledContextInstanceRecord.getContextInstance());
                            logger.warn(errorMessage);
                            errorReportingService.notify(FLOW_NAME, payload, new InvalidContextInstanceIdException(errorMessage));
                            this.scheduledProcessProducerConnectionCallback = new ScheduledProcessProducerConnectionCallbackImpl(payload, null);
                            return;
                        }
                    }
                }

                throw new InvalidContextInstanceIdException(String.format("Could not resolve context machine with context name[%s] and context instance id [%s]. Does not exist in the system! " +
                    " Cache Contents - %s", contextualisedScheduledProcessEvent.getContextName(), contextualisedScheduledProcessEvent.getContextInstanceId(), ContextMachineCache.instance().toString()));
            }

            if(this.configuration.isLogDetails()) {
                logger.info(String.format("Received contextualisedScheduledProcessEvent with context instance id[%s]. " +
                    "Processing that against context instance name[%s], with id[%s]", contextualisedScheduledProcessEvent.getContextInstanceId(),
                    contextMachine.getContext().getName(), contextMachine.getContext().getId()));

                if(!contextualisedScheduledProcessEvent.getContextInstanceId().equals(contextMachine.getContext().getId())) {
                    logger.warn(String.format("contextualisedScheduledProcessEvent context instance id[%s] does not machine " +
                        "context machine instance id[%s] for context[%s]", contextualisedScheduledProcessEvent.getContextInstanceId(),
                        contextMachine.getContext().getId(), contextMachine.getContext().getName()));
                }
            }

            this.scheduledProcessProducerConnectionCallback = new ScheduledProcessProducerConnectionCallbackImpl(payload, contextMachine);
        }
        catch (InvalidContextInstanceIdException | ConfigurationException e) {
            e.printStackTrace();
            if(this.configuration.isIgnoreErrors()) {
                logger.info("Ignoring error [{}] for payload [{}]", e.getMessage(), payload);
            }
            else {
                throw e;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            if(this.configuration.isIgnoreErrors()) {
                logger.info("Ignoring error [{}] for payload [{}]", e.getMessage(), payload);
            }
            else {
                throw new EndpointException(e);
            }
        }
    }

    private void removeAgentInstances(ContextInstance instance) {
        HashMap<String, ModuleMetaData> agents = getAgents(instance);
        if (!agents.keySet().isEmpty()) {
            for (String key : agents.keySet()) {
                ModuleMetaData agent = agents.get(key);
                contextInstancePublicationService.remove(agent.getUrl(), instance);
            }
        }
    }

    private HashMap<String, ModuleMetaData> getAgents(Context context) {
        HashMap<String, ModuleMetaData> agents = new HashMap<>();

        List<String> contextAgents = ContextHelper.getAllAgents(context);

        ModuleMetadataSearchResults searchResults = moduleMetadataService
            .find(contextAgents, ModuleType.SCHEDULER_AGENT, -1, -1);

        searchResults.getResultList().forEach(agent -> agents.put(agent.getName(), agent));

        return agents;
    }

    /**
     * Enlist the resource in the current transaction if it is running.
     *
     * @throws SystemException
     * @throws RollbackException
     */
    private void enlist() throws SystemException, RollbackException {
        if(this.transactionManager.getTransaction() != null) {
            if (this.transactionManager.getTransaction().getStatus() != ActionStatus.RUNNING) {
                return;
            }

            this.transactionManager.getTransaction().enlistResource(this);
        }
    }

    @Override
    public String getConfiguredResourceId() {
        return configurationId;
    }

    @Override
    public void setConfiguredResourceId(String configurationId) {
        this.configurationId = configurationId;
    }

    @Override
    public ScheduleProcessInboundProducerConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public void setConfiguration(ScheduleProcessInboundProducerConfiguration scheduleProcessInboundProducerConfiguration) {
        this.configuration = scheduleProcessInboundProducerConfiguration;
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        logger.debug("commit");
        try {
            this.scheduledProcessProducerConnectionCallback.execute();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new XAException(String.format("Could not commit transaction! Cause[%s]", e.getMessage()));
        }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        logger.debug("end");
    }

    @Override
    public void forget(Xid xid) throws XAException {
        logger.debug("forget");
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        return false;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        return 0;
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        return new Xid[0];
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        logger.debug("rollback");
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return false;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        logger.debug("start");
    }

    @Override
    public void setErrorReportingService(ErrorReportingService errorReportingService) {
        this.errorReportingService = errorReportingService;
    }
}
