package com.garyjacobs.weathertest;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.squareup.picasso.Cache;

/**
 * Created by gjacobs on 11/23/15.
 */
public class MemoryCache implements Cache {
    private static LruCache<String, Bitmap> lruCache;
    private static MemoryCache me;

    private MemoryCache() {
    }

    public static MemoryCache getInstance(int size) {
        if (MemoryCache.me == null) {
            MemoryCache.lruCache = new LruCache<>(size);
            MemoryCache.me = new MemoryCache();
        }
        return MemoryCache.me;
    }

    @Override
    public Bitmap get(String s) {
        return MemoryCache.lruCache.get(s);
    }

    @Override
    public void set(String s, Bitmap bitmap) {
        MemoryCache.lruCache.put(s, bitmap);
    }

    @Override
    public int size() {
        return MemoryCache.lruCache.size();
    }

    @Override
    public int maxSize() {
        return MemoryCache.lruCache.maxSize();
    }

    @Override
    public void clear() {
        MemoryCache.lruCache.evictAll();
    }

    @Override
    public void clearKeyUri(String s) {
        MemoryCache.lruCache.remove(s);
    }
}
