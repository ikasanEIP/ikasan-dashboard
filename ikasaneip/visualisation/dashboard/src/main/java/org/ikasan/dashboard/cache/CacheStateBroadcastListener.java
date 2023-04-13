package org.ikasan.dashboard.cache;

import org.ikasan.dashboard.broadcast.FlowState;

public interface CacheStateBroadcastListener {

    /**
     * Receive the cache state broadcast.
     *
     * @param message
     */
    void receiveCacheStateBroadcast(FlowState message);
}
