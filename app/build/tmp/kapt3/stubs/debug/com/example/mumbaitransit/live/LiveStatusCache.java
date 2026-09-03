package com.example.mumbaitransit.live;

/**
 * Short-lived in-memory cache of live responses.
 *
 * Live positions only move every few tens of seconds, and every request costs
 * quota, so repeated opens of the same train inside the window are served from here.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001\u0011B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0015\u0010\t\u001a\u0004\u0018\u00010\u00042\u0006\u0010\n\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\u000bJ\u0010\u0010\f\u001a\u0004\u0018\u00010\r2\u0006\u0010\n\u001a\u00020\u0007J\u0016\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\n\u001a\u00020\u00072\u0006\u0010\u0010\u001a\u00020\rR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lcom/example/mumbaitransit/live/LiveStatusCache;", "", "()V", "TTL_MS", "", "entries", "", "", "Lcom/example/mumbaitransit/live/LiveStatusCache$Entry;", "ageOf", "trainNumber", "(Ljava/lang/String;)Ljava/lang/Long;", "get", "Lcom/example/mumbaitransit/live/LiveTrainStatus;", "put", "", "status", "Entry", "app_debug"})
public final class LiveStatusCache {
    private static final long TTL_MS = 30000L;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Map<java.lang.String, com.example.mumbaitransit.live.LiveStatusCache.Entry> entries = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.live.LiveStatusCache INSTANCE = null;
    
    private LiveStatusCache() {
        super();
    }
    
    @kotlin.jvm.Synchronized()
    @org.jetbrains.annotations.Nullable()
    public final synchronized com.example.mumbaitransit.live.LiveTrainStatus get(@org.jetbrains.annotations.NotNull()
    java.lang.String trainNumber) {
        return null;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void put(@org.jetbrains.annotations.NotNull()
    java.lang.String trainNumber, @org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.live.LiveTrainStatus status) {
    }
    
    /**
     * Age of the cached copy in millis, or null when nothing is held.
     */
    @kotlin.jvm.Synchronized()
    @org.jetbrains.annotations.Nullable()
    public final synchronized java.lang.Long ageOf(@org.jetbrains.annotations.NotNull()
    java.lang.String trainNumber) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0082\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\t\u0010\u000b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\f\u001a\u00020\u0005H\u00c6\u0003J\u001d\u0010\r\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0011\u001a\u00020\u0012H\u00d6\u0001J\t\u0010\u0013\u001a\u00020\u0014H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0015"}, d2 = {"Lcom/example/mumbaitransit/live/LiveStatusCache$Entry;", "", "status", "Lcom/example/mumbaitransit/live/LiveTrainStatus;", "at", "", "(Lcom/example/mumbaitransit/live/LiveTrainStatus;J)V", "getAt", "()J", "getStatus", "()Lcom/example/mumbaitransit/live/LiveTrainStatus;", "component1", "component2", "copy", "equals", "", "other", "hashCode", "", "toString", "", "app_debug"})
    static final class Entry {
        @org.jetbrains.annotations.NotNull()
        private final com.example.mumbaitransit.live.LiveTrainStatus status = null;
        private final long at = 0L;
        
        public Entry(@org.jetbrains.annotations.NotNull()
        com.example.mumbaitransit.live.LiveTrainStatus status, long at) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.live.LiveTrainStatus getStatus() {
            return null;
        }
        
        public final long getAt() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.live.LiveTrainStatus component1() {
            return null;
        }
        
        public final long component2() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.live.LiveStatusCache.Entry copy(@org.jetbrains.annotations.NotNull()
        com.example.mumbaitransit.live.LiveTrainStatus status, long at) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}