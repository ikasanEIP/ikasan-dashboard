package org.ikasan.dashboard.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

public class ContextCache {
    private static Cache<String, String> CONTEXT_CACHE;

    static {
        CONTEXT_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    }

    public static void addContext(String id, String context) {
        CONTEXT_CACHE.put(id, context);
    }

    public static String getContext(String id) {
        String context =  CONTEXT_CACHE.getIfPresent(id);

        return context;
    }

    /**
     * Get and remove the cached context for the given id, so a deep link is only followed once.
     *
     * @param id the http session id
     * @return the cached context, or null
     */
    public static String consumeContext(String id) {
        String context = CONTEXT_CACHE.getIfPresent(id);
        if(context != null) {
            CONTEXT_CACHE.invalidate(id);
        }
        return context;
    }
}
