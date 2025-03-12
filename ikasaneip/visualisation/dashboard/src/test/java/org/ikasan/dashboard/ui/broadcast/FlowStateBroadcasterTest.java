package org.ikasan.dashboard.ui.broadcast;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.FlowStateBroadcastListener;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.broadcast.State;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FlowStateBroadcasterTest
{
    @Before
    public void setup()
    {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.WARN);
    }

    @Test
    public void test_broadcast() throws InterruptedException
    {
        MyConsumer myConsumer1 = new MyConsumer();
        MyConsumer myConsumer2 = new MyConsumer();
        MyConsumer myConsumer3 = new MyConsumer();
        MyConsumer myConsumer4 = new MyConsumer();
        MyConsumer myConsumer5 = new MyConsumer();

        FlowStateBroadcaster.register(myConsumer1);
        FlowStateBroadcaster.register(myConsumer2);
        FlowStateBroadcaster.register(myConsumer3);
        FlowStateBroadcaster.register(myConsumer4);
        FlowStateBroadcaster.register(myConsumer5);

        FlowState flowState = new FlowState("moduleName", "flowState", State.RUNNING_STATE);
        FlowStateBroadcaster.broadcast(flowState);

        Thread.sleep(20);

        Assertions.assertTrue(myConsumer1.flowStates.size() == 1, "One flow state has been broadcast!");
        Assertions.assertEquals(flowState, myConsumer1.flowStates.get(0), "flow state equals");
        Assertions.assertEquals( new FlowState("moduleName", "flowState", State.RUNNING_STATE), myConsumer1.flowStates.get(0), "flow state equals");
        Assertions.assertTrue(myConsumer2.flowStates.size() == 1, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer3.flowStates.size() == 1, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer4.flowStates.size() == 1, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer5.flowStates.size() == 1, "One flow state has been broadcast!");

        FlowStateBroadcaster.unregister(myConsumer1);

        FlowStateBroadcaster.broadcast(new FlowState("moduleName", "flowState", State.RUNNING_STATE));

        Thread.sleep(20);

        Assertions.assertTrue(myConsumer1.flowStates.size() == 1, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer2.flowStates.size() == 2, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer3.flowStates.size() == 2, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer4.flowStates.size() == 2, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer5.flowStates.size() == 2, "One flow state has been broadcast!");

        FlowStateBroadcaster.unregister(myConsumer2);

        FlowStateBroadcaster.broadcast(new FlowState("moduleName", "flowState", State.RUNNING_STATE));

        Thread.sleep(20);

        Assertions.assertTrue(myConsumer1.flowStates.size() == 1, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer2.flowStates.size() == 2, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer3.flowStates.size() == 3, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer4.flowStates.size() == 3, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer5.flowStates.size() == 3, "One flow state has been broadcast!");

        FlowStateBroadcaster.unregister(myConsumer3);

        FlowStateBroadcaster.broadcast(new FlowState("moduleName", "flowState", State.RUNNING_STATE));

        Thread.sleep(20);

        Assertions.assertTrue(myConsumer1.flowStates.size() == 1, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer2.flowStates.size() == 2, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer3.flowStates.size() == 3, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer4.flowStates.size() == 4, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer5.flowStates.size() == 4, "One flow state has been broadcast!");

        FlowStateBroadcaster.unregister(myConsumer4);

        FlowStateBroadcaster.broadcast(new FlowState("moduleName", "flowState", State.RUNNING_STATE));

        Thread.sleep(20);

        Assertions.assertTrue(myConsumer1.flowStates.size() == 1, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer2.flowStates.size() == 2, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer3.flowStates.size() == 3, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer4.flowStates.size() == 4, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer5.flowStates.size() == 5, "One flow state has been broadcast!");

        FlowStateBroadcaster.unregister(myConsumer5);

        FlowStateBroadcaster.broadcast(new FlowState("moduleName", "flowState", State.RUNNING_STATE));

        Thread.sleep(20);

        Assertions.assertTrue(myConsumer1.flowStates.size() == 1, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer2.flowStates.size() == 2, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer3.flowStates.size() == 3, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer4.flowStates.size() == 4, "One flow state has been broadcast!");
        Assertions.assertTrue(myConsumer5.flowStates.size() == 5, "One flow state has been broadcast!");

    }

    private class MyConsumer implements FlowStateBroadcastListener
    {
        private List<FlowState> flowStates = new ArrayList<>();

        @Override
        public void receiveFlowStateBroadcast(FlowState message) {
            flowStates.add(message);
        }
    }
}
