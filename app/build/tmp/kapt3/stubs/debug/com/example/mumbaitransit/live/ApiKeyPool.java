package com.example.mumbaitransit.live;

/**
 * Holds the RailRadar keys and decides which one to spend.
 *
 * Each key is capped at 1,000 requests a month, so the pool:
 * - counts every request it hands out, per key, per calendar month;
 * - retires a key for the rest of the month the moment the API reports it
 *   exhausted (or the local count reaches the cap);
 * - moves to the next key and remembers that choice across app launches, so a
 *   burnt key is never retried on every cold start.
 *
 * Counters reset automatically when the month rolls over.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\t\u0018\u0000 \u001e2\u00020\u0001:\u0002\u001e\u001fB\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fJ\u0006\u0010\u0010\u001a\u00020\u0011J\u0010\u0010\u0012\u001a\u00020\u00112\u0006\u0010\u0013\u001a\u00020\u000bH\u0002J\u0006\u0010\u0014\u001a\u00020\u000bJ\u0006\u0010\u0015\u001a\u00020\u000bJ\u000e\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u000fJ\u000e\u0010\u0019\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u000fJ\b\u0010\u001a\u001a\u00020\u0017H\u0002J\u0010\u0010\u001b\u001a\u00020\u00072\u0006\u0010\u0013\u001a\u00020\u000bH\u0002J\u0010\u0010\u001c\u001a\u00020\u000b2\u0006\u0010\u0013\u001a\u00020\u000bH\u0002J\u0006\u0010\u001d\u001a\u00020\u000bR\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\n\u001a\u00020\u000b8F\u00a2\u0006\u0006\u001a\u0004\b\f\u0010\r\u00a8\u0006 "}, d2 = {"Lcom/example/mumbaitransit/live/ApiKeyPool;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "keys", "", "", "prefs", "Landroid/content/SharedPreferences;", "size", "", "getSize", "()I", "currentKey", "Lcom/example/mumbaitransit/live/ApiKeyPool$KeyHandle;", "hasUsableKey", "", "isUsable", "index", "keysRemaining", "monthlyBudget", "recordUse", "", "handle", "retire", "rollMonthIfNeeded", "usageKey", "usedBy", "usedThisMonth", "Companion", "KeyHandle", "app_debug"})
public final class ApiKeyPool {
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences prefs = null;
    
    /**
     * Keys come from local.properties via BuildConfig, with the known set as fallback.
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> keys = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFS = "railradar_keys";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_ACTIVE = "active_index";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_MONTH = "month_stamp";
    public static final int MONTHLY_CAP = 1000;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.live.ApiKeyPool.Companion Companion = null;
    
    public ApiKeyPool(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    public final int getSize() {
        return 0;
    }
    
    /**
     * Whether any key still has headroom this month.
     */
    public final boolean hasUsableKey() {
        return false;
    }
    
    /**
     * The key to try next, or null when every key is spent for the month.
     * Does not consume anything — call [recordUse] once the request is sent.
     */
    @org.jetbrains.annotations.Nullable()
    public final com.example.mumbaitransit.live.ApiKeyPool.KeyHandle currentKey() {
        return null;
    }
    
    /**
     * Counts one request against [handle].
     */
    public final void recordUse(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.live.ApiKeyPool.KeyHandle handle) {
    }
    
    /**
     * Retires a key for the remainder of the month and advances the pool.
     * Called when the API answers with a quota or auth failure.
     */
    public final void retire(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.live.ApiKeyPool.KeyHandle handle) {
    }
    
    /**
     * Requests spent this month across every key.
     */
    public final int usedThisMonth() {
        return 0;
    }
    
    /**
     * Total monthly allowance across the pool.
     */
    public final int monthlyBudget() {
        return 0;
    }
    
    /**
     * Keys that still have requests left.
     */
    public final int keysRemaining() {
        return 0;
    }
    
    private final boolean isUsable(int index) {
        return false;
    }
    
    private final int usedBy(int index) {
        return 0;
    }
    
    private final java.lang.String usageKey(int index) {
        return null;
    }
    
    /**
     * Wipes counters when the calendar month changes.
     */
    private final void rollMonthIfNeeded() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/example/mumbaitransit/live/ApiKeyPool$Companion;", "", "()V", "KEY_ACTIVE", "", "KEY_MONTH", "MONTHLY_CAP", "", "PREFS", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\t\u0010\u000b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\f\u001a\u00020\u0005H\u00c6\u0003J\u001d\u0010\r\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0011\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u0012\u001a\u00020\u0005H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0013"}, d2 = {"Lcom/example/mumbaitransit/live/ApiKeyPool$KeyHandle;", "", "index", "", "value", "", "(ILjava/lang/String;)V", "getIndex", "()I", "getValue", "()Ljava/lang/String;", "component1", "component2", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
    public static final class KeyHandle {
        private final int index = 0;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String value = null;
        
        public KeyHandle(int index, @org.jetbrains.annotations.NotNull()
        java.lang.String value) {
            super();
        }
        
        public final int getIndex() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getValue() {
            return null;
        }
        
        public final int component1() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.live.ApiKeyPool.KeyHandle copy(int index, @org.jetbrains.annotations.NotNull()
        java.lang.String value) {
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