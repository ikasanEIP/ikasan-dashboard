package org.ikasan.scheduler.broker;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import com.leansoft.bigqueue.IBigQueue;
import org.ikasan.scheduler.context.cache.ContextMachineCache;
import org.ikasan.scheduler.core.machine.ContextMachine;
import org.ikasan.scheduler.core.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduler.util.ObjectMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InboundScheduledEventBroker {

    private Logger logger = LoggerFactory.getLogger(InboundScheduledEventBroker.class);

    private IBigQueue inboundQueue;
    private ExecutorService bigQueueListenerExecutor;
    private ListenableFuture<byte[]> listenableFuture;
    private ObjectMapper objectMapper;


    public InboundScheduledEventBroker(IBigQueue inboundQueue) {
        this.inboundQueue = inboundQueue;
        this.bigQueueListenerExecutor = Executors.newSingleThreadExecutor();
        this.addInboundListener();
        this.objectMapper = ObjectMapperFactory.newInstance();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void stop() {
        this.listenableFuture.cancel(false);
        this.listenableFuture = null;
        this.bigQueueListenerExecutor.shutdownNow();
        this.bigQueueListenerExecutor = null;

    }

    private void addInboundListener() {
        if(this.bigQueueListenerExecutor != null) {
            this.listenableFuture = this.inboundQueue.peekAsync();
            this.listenableFuture.addListener(new InboundQueueMessageRunnable()
                , bigQueueListenerExecutor);
        }
    }

    /**
     * Inner class to allow for us to listen asynchronously to the BigQueue.
     */
    private class InboundQueueMessageRunnable implements Runnable {

        @Override
        public void run() {
            try {
                byte[] event = inboundQueue.peek();
                if(event == null) {
                    return;
                }

                ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEventInstance
                    = objectMapper.readValue(event, ContextualisedScheduledProcessEventImpl.class);

                ContextMachine contextMachine;
                if(contextualisedScheduledProcessEventInstance.getContextInstanceId() != null) {
                    contextMachine = ContextMachineCache.instance()
                        .getByContextName(contextualisedScheduledProcessEventInstance.getContextInstanceId());
                }
                else {
                    contextMachine = ContextMachineCache.instance()
                        .getByContextName(contextualisedScheduledProcessEventInstance.getContextId());
                }

                if(contextMachine == null) {
                   logger.warn("Could not get context machine for context: " + contextualisedScheduledProcessEventInstance.getContextId());
                    inboundQueue.dequeue();
                    inboundQueue.gc();
                    return;
//                    throw new RuntimeException(String.format("Could not get context instance[%s] from ContextMachineCache", "test"));
                }

                contextMachine.eventReceived(new String(event));

                inboundQueue.dequeue();
                inboundQueue.gc();
            }
            catch (Exception e) {
                e.printStackTrace();
                // todo notification
            }
            finally {
                addInboundListener();
            }
        }
    }
}
