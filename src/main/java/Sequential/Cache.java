package Sequential;

import java.util.concurrent.ConcurrentHashMap;

public class Cache {
    private ConcurrentHashMap<String, byte[]> cache;
    private int numCached;

    public Cache(){
        cache = new ConcurrentHashMap<>();
        numCached = 0;
    }

    public void addToCache(String url, byte[] bytes) {
        if (numCached > 5) {
            cache.keySet().stream().findAny().ifPresent(key -> cache.remove(key));
            numCached--;
        }
        cache.put(url, bytes);
        numCached++;
    }

    public boolean hasKey(String url) {
        return cache.containsKey(url);
    }

    public byte[] get(String url) {
        return cache.get(url);
    }
}
