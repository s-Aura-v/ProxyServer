package Sequential;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple cache for storing image bytes.
 * <p>
 * This class uses a hashmap with key, safe_url, to store value, byte[] image.
 */
public class Cache {
    private ConcurrentHashMap<String, byte[]> cache;
    private int numCached;

    public Cache(){
        cache = new ConcurrentHashMap<>();
        numCached = 0;
    }

    /**
     * Adds image bytes[] to cache using safe_url as key.
     * @param safeUrl - image address (url) where all the "/" have been replaced with "__"
     * @param bytes - image bytes obtained from the url
     */
    public void addToCache(String safeUrl, byte[] bytes) {
        if (numCached > 5) {
            cache.keySet().stream().findAny().ifPresent(key -> cache.remove(key));
            numCached--;
        }
        cache.put(safeUrl, bytes);
        numCached++;
    }

    /**
     * Check if url is in cache
     *
     * @param url - key used to store imageBytes
     * @return the truth value answering if the url is in the cache
     */
    public boolean hasKey(String url) {
        return cache.containsKey(url);
    }

    /**
     * Retrieves the image bytes[] given a key
     *
     * @param url - the key used to store byte[]
     * @return image byte[] storing the value of the url address
     */
    public byte[] get(String url) {
        return cache.get(url);
    }
}
