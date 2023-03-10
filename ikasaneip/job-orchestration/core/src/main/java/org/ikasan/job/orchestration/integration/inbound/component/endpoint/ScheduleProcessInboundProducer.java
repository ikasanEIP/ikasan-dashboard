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
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.component.endpoint.EndpointException;
import org.ikasan.spec.component.endpoint.Producer;
import org.ikasan.spec.configuration.ConfigurationException;
import org.ikasan.spec.configuration.ConfiguredResource;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class ScheduleProcessInboundProducer implements Producer<String>, ConfiguredResource<ScheduleProcessInboundProducerConfiguration>, LastResourceCommitOptimisation {

    private Logger logger = LoggerFactory.getLogger(ScheduleProcessInboundProducer.class);
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private ScheduleProcessInboundProducerConfiguration configuration;
    private String configurationId;
    private ScheduledProcessProducerConnectionCallback scheduledProcessProducerConnectionCallback;
    private TransactionManager transactionManager;

    public ScheduleProcessInboundProducer(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void invoke(String payload) throws EndpointException {
        try {
            this.enlist();

            BigQueueMessage bigQueueMessage = objectMapper.readValue(payload, BigQueueMessageImpl.class);
            String message = (String) bigQueueMessage.getMessage();

            ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent
                = objectMapper.readValue(message, ContextualisedScheduledProcessEventImpl.class);

            if(contextualisedScheduledProcessEvent.getContextInstanceId() == null) {
                throw new InvalidContextInstanceIdException(String.format("Received scheduler event with null context instance id [%s]." +
                    " Cache Contents - %s", contextualisedScheduledProcessEvent, ContextMachineCache.instance().toString()));
            }

            ContextMachine contextMachine = ContextMachineCache.instance()
                .getByContextInstanceId(contextualisedScheduledProcessEvent.getContextInstanceId());

            if(contextMachine == null) {
                throw new InvalidContextInstanceIdException(String.format("Could not resolve context machine with context name[%s] and context instance id [%s]." +
                    " Cache Contents - %s", contextualisedScheduledProcessEvent.getContextName(), contextualisedScheduledProcessEvent.getContextInstanceId(), ContextMachineCache.instance().toString()));
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
            throw new XAException("Could not commit transaction!");
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
        logger.debug("commit");
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return false;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        logger.debug("start");
    }
}
