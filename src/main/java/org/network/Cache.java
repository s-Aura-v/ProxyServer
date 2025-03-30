package org.network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Cache {
    private ConcurrentHashMap<String, byte[]> cache;
    private int numCached;
    private ThreadLocalRandom random;

    public Cache(){
        cache = new ConcurrentHashMap<>();
        numCached = 0;
        random = ThreadLocalRandom.current();
    }

    public void addToCache(String url, byte[] bytes) {
        if (numCached > 5) {
            // remove oldest value
        }
        cache.put(url, bytes);
        numCached++;
    }

    public void removeFromCache(String url) {
        cache.remove(url);
        numCached--;
    }

    public boolean hasKey(String url) {
        return cache.containsKey(url);
    }

    public int getNumCached() {
        return numCached;
    }
}
