package org.ikasan.dashboard.cluster.service;

/**
 * Service interface for managing leader election in a clustered dashboard deployment.
 */
public interface LeaderElectionService {

    /**
     * Start participating in leader election.
     *
     * @throws Exception if the service fails to start
     */
    void start() throws Exception;

    /**
     * Stop participating in leader election and release leadership.
     */
    void stop();

    /**
     * Check if this instance is currently the leader.
     *
     * @return true if this instance is the leader, false otherwise
     */
    boolean isLeader();

    /**
     * Block until this instance becomes the leader.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void awaitLeadership() throws InterruptedException;

    /**
     * Add a listener for leadership events.
     *
     * @param listener the listener to add
     */
    void addLeadershipListener(LeadershipListener listener);

    /**
     * Remove a leadership listener.
     *
     * @param listener the listener to remove
     */
    void removeLeadershipListener(LeadershipListener listener);
}
