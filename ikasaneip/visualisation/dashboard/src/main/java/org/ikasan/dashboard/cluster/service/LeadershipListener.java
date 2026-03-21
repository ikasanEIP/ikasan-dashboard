package org.ikasan.dashboard.cluster.service;

/**
 * Listener interface for receiving leadership state change events.
 */
public interface LeadershipListener {

    /**
     * Called when this instance acquires leadership.
     */
    void onLeadershipAcquired();

    /**
     * Called when this instance loses leadership.
     */
    void onLeadershipLost();
}
