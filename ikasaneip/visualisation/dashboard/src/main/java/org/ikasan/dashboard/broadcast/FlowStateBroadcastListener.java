package org.ikasan.dashboard.broadcast;

public interface FlowStateBroadcastListener {

    /**
     * Receive the flow state broadcast.
     *
     * @param message
     */
    void receiveFlowStateBroadcast(FlowState message);
}
