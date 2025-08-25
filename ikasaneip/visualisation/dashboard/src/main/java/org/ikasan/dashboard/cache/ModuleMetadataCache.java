package org.ikasan.dashboard.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ModuleMetadataCache {
    private static final String KEY = "KEY";
    private Cache<String, List<ModuleMetaData>> moduleMetadataCache;
    private static ModuleMetadataCache instance;
    private ModuleMetaDataService moduleMetaDataService;
    private int cacheExpirySeconds;

    /**
     * ModuleMetadataCache is a private class used to cache module metadata.
     *
     * @param moduleMetaDataService  An instance of ModuleMetaDataService used to fetch module metadata.
     * @param cacheExpirySeconds     The time in seconds after which the cache will expire and be invalidated.
     *
     * Usage Example:
     *
     * ModuleMetadataCache cache = ModuleMetadataCache.init(moduleMetaDataService, 3600);
     * List<ModuleMetaData> metadata = cache.getModuleMetadata();
     */
    private ModuleMetadataCache(ModuleMetaDataService moduleMetaDataService, int cacheExpirySeconds) {
        this.moduleMetaDataService = moduleMetaDataService;
        this.cacheExpirySeconds = cacheExpirySeconds;
         this.moduleMetadataCache = Caffeine.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(cacheExpirySeconds, TimeUnit.SECONDS)
            .build();

         this.moduleMetadataCache.put(KEY, moduleMetaDataService.findAll());
    }

    /**
     * Returns the singleton instance of the ModuleMetadataCache.
     *
     * @return the singleton instance of the ModuleMetadataCache
     */
    public static ModuleMetadataCache instance() {
        return instance;
    }

    /**
     * Initializes the ModuleMetadataCache singleton instance if it is not already created.
     * Otherwise, returns the existing instance.
     *
     * @param moduleMetaDataService An instance of ModuleMetaDataService used to fetch module metadata.
     * @param cacheExpirySeconds    The time in seconds after which the cache will expire and be invalidated.
     * @return The singleton instance of the ModuleMetadataCache.
     */
    public synchronized static ModuleMetadataCache init(ModuleMetaDataService moduleMetaDataService, int cacheExpirySeconds) {
        if (instance != null)
        {
            return instance;
        }

        instance = new ModuleMetadataCache(moduleMetaDataService, cacheExpirySeconds);
        return instance;
    }

    /**
     * Retrieves the cached module metadata from the Module Metadata Cache.
     *
     * @return The list of ModuleMetaData objects representing the module metadata,
     *         or null if the cache does not contain any metadata.
     */
    public List<ModuleMetaData> getModuleMetadata() {
        return this.moduleMetadataCache.get(KEY, k -> moduleMetaDataService.findAll());
    }

    /**
     * Resets the ModuleMetadataCache by setting the singleton instance to null and
     * reinitializing it with the provided ModuleMetaDataService and cache expiry seconds.
     */
    public void reset() {
        instance = null;
        init(this.moduleMetaDataService, this.cacheExpirySeconds);
    }
}
