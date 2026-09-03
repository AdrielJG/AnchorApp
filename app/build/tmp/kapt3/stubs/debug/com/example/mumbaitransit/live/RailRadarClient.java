package com.example.mumbaitransit.live;

/**
 * Talks to RailRadar's live-status endpoint.
 *
 * Two things this class is deliberately careful about:
 *
 * 1. **Key failover.** Each key allows 1,000 requests a month. On a quota or auth
 *   failure the key is retired for the month and the next one is tried inside the
 *   same call, so the user never sees a failure that a spare key could have covered.
 *
 * 2. **Loose parsing.** Field names are read from a list of candidates rather than
 *   one fixed spelling, and anything missing simply comes back null. A payload
 *   that changes shape degrades the screen instead of crashing it.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000d\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 )2\u00020\u0001:\u0002()B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J \u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\b\b\u0002\u0010\u0013\u001a\u00020\u0014H\u0086@\u00a2\u0006\u0002\u0010\u0015J\u0010\u0010\u0016\u001a\u00020\u00142\u0006\u0010\u0017\u001a\u00020\u0012H\u0002J\u001a\u0010\u0018\u001a\u0004\u0018\u00010\u00192\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u001a\u001a\u00020\u0012H\u0002J\u0014\u0010\u001b\u001a\u0004\u0018\u00010\u001c2\b\u0010\u001d\u001a\u0004\u0018\u00010\u001eH\u0002J\u0012\u0010\u001f\u001a\u0004\u0018\u00010 2\u0006\u0010\u001d\u001a\u00020\u001eH\u0002J\u0018\u0010!\u001a\u00020\"2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010#\u001a\u00020$H\u0002J\u0012\u0010%\u001a\u00020&2\b\u0010\'\u001a\u0004\u0018\u00010\u0012H\u0002R\u0011\u0010\u0005\u001a\u00020\u00068F\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u000b\u001a\u00020\u00068F\u00a2\u0006\u0006\u001a\u0004\b\f\u0010\bR\u0011\u0010\r\u001a\u00020\u00068F\u00a2\u0006\u0006\u001a\u0004\b\u000e\u0010\b\u00a8\u0006*"}, d2 = {"Lcom/example/mumbaitransit/live/RailRadarClient;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "keysRemaining", "", "getKeysRemaining", "()I", "pool", "Lcom/example/mumbaitransit/live/ApiKeyPool;", "quotaTotal", "getQuotaTotal", "quotaUsed", "getQuotaUsed", "fetchLive", "Lcom/example/mumbaitransit/live/LiveResult;", "trainNumber", "", "forceRefresh", "", "(Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "looksLikeQuotaOrAuth", "message", "parse", "Lcom/example/mumbaitransit/live/LiveTrainStatus;", "body", "parseHalt", "Lcom/example/mumbaitransit/live/Halt;", "element", "Lcom/google/gson/JsonElement;", "parseStop", "Lcom/example/mumbaitransit/live/RouteStop;", "request", "Lcom/example/mumbaitransit/live/RailRadarClient$Attempt;", "handle", "Lcom/example/mumbaitransit/live/ApiKeyPool$KeyHandle;", "toStopStatus", "Lcom/example/mumbaitransit/live/StopStatus;", "raw", "Attempt", "Companion", "app_debug"})
public final class RailRadarClient {
    @org.jetbrains.annotations.NotNull()
    private final com.example.mumbaitransit.live.ApiKeyPool pool = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String BASE_URL = "https://api.railradar.in";
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> ISO_FORMATS = null;
    
    /**
     * IST — every station on this network is in one zone, so this is safe.
     */
    @org.jetbrains.annotations.NotNull()
    private static final java.util.TimeZone IST = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.live.RailRadarClient.Companion Companion = null;
    
    public RailRadarClient(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    public final int getQuotaUsed() {
        return 0;
    }
    
    public final int getQuotaTotal() {
        return 0;
    }
    
    public final int getKeysRemaining() {
        return 0;
    }
    
    /**
     * Fetches live status for [trainNumber], serving a recent cached copy when one
     * exists so that back-navigation and rotation don't spend requests.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchLive(@org.jetbrains.annotations.NotNull()
    java.lang.String trainNumber, boolean forceRefresh, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.mumbaitransit.live.LiveResult> $completion) {
        return null;
    }
    
    private final com.example.mumbaitransit.live.RailRadarClient.Attempt request(java.lang.String trainNumber, com.example.mumbaitransit.live.ApiKeyPool.KeyHandle handle) {
        return null;
    }
    
    private final boolean looksLikeQuotaOrAuth(java.lang.String message) {
        return false;
    }
    
    private final com.example.mumbaitransit.live.LiveTrainStatus parse(java.lang.String trainNumber, java.lang.String body) {
        return null;
    }
    
    /**
     * Halts arrive either as an object or as a bare station code string.
     */
    private final com.example.mumbaitransit.live.Halt parseHalt(com.google.gson.JsonElement element) {
        return null;
    }
    
    private final com.example.mumbaitransit.live.RouteStop parseStop(com.google.gson.JsonElement element) {
        return null;
    }
    
    private final com.example.mumbaitransit.live.StopStatus toStopStatus(java.lang.String raw) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b2\u0018\u00002\u00020\u0001:\u0003\u0003\u0004\u0005B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0003\u0006\u0007\b\u00a8\u0006\t"}, d2 = {"Lcom/example/mumbaitransit/live/RailRadarClient$Attempt;", "", "()V", "Failed", "KeyDead", "Ok", "Lcom/example/mumbaitransit/live/RailRadarClient$Attempt$Failed;", "Lcom/example/mumbaitransit/live/RailRadarClient$Attempt$KeyDead;", "Lcom/example/mumbaitransit/live/RailRadarClient$Attempt$Ok;", "app_debug"})
    static abstract class Attempt {
        
        private Attempt() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\n\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\t\u0010\u000b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\f\u001a\u00020\u0005H\u00c6\u0003J\u001d\u0010\r\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u000e\u001a\u00020\u00052\b\u0010\u000f\u001a\u0004\u0018\u00010\u0010H\u00d6\u0003J\t\u0010\u0011\u001a\u00020\u0012H\u00d6\u0001J\t\u0010\u0013\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0014"}, d2 = {"Lcom/example/mumbaitransit/live/RailRadarClient$Attempt$Failed;", "Lcom/example/mumbaitransit/live/RailRadarClient$Attempt;", "message", "", "retryable", "", "(Ljava/lang/String;Z)V", "getMessage", "()Ljava/lang/String;", "getRetryable", "()Z", "component1", "component2", "copy", "equals", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Failed extends com.example.mumbaitransit.live.RailRadarClient.Attempt {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String message = null;
            private final boolean retryable = false;
            
            public Failed(@org.jetbrains.annotations.NotNull()
            java.lang.String message, boolean retryable) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getMessage() {
                return null;
            }
            
            public final boolean getRetryable() {
                return false;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            public final boolean component2() {
                return false;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.example.mumbaitransit.live.RailRadarClient.Attempt.Failed copy(@org.jetbrains.annotations.NotNull()
            java.lang.String message, boolean retryable) {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/example/mumbaitransit/live/RailRadarClient$Attempt$KeyDead;", "Lcom/example/mumbaitransit/live/RailRadarClient$Attempt;", "message", "", "(Ljava/lang/String;)V", "getMessage", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class KeyDead extends com.example.mumbaitransit.live.RailRadarClient.Attempt {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String message = null;
            
            public KeyDead(@org.jetbrains.annotations.NotNull()
            java.lang.String message) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getMessage() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.example.mumbaitransit.live.RailRadarClient.Attempt.KeyDead copy(@org.jetbrains.annotations.NotNull()
            java.lang.String message) {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/example/mumbaitransit/live/RailRadarClient$Attempt$Ok;", "Lcom/example/mumbaitransit/live/RailRadarClient$Attempt;", "body", "", "(Ljava/lang/String;)V", "getBody", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Ok extends com.example.mumbaitransit.live.RailRadarClient.Attempt {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String body = null;
            
            public Ok(@org.jetbrains.annotations.NotNull()
            java.lang.String body) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getBody() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.example.mumbaitransit.live.RailRadarClient.Attempt.Ok copy(@org.jetbrains.annotations.NotNull()
            java.lang.String body) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\t\u001a\u0004\u0018\u00010\u00042\b\u0010\n\u001a\u0004\u0018\u00010\u0004J\u0018\u0010\u000b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u0004H\u0002J\u0010\u0010\u000b\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u0010H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00040\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/example/mumbaitransit/live/RailRadarClient$Companion;", "", "()V", "BASE_URL", "", "ISO_FORMATS", "", "IST", "Ljava/util/TimeZone;", "formatTime", "raw", "render", "hour24", "", "minute", "millis", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        /**
         * Normalises whatever the API sends into a "01:04 AM" display string.
         * Accepts ISO-8601, epoch millis/seconds, and plain "HH:mm".
         */
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String formatTime(@org.jetbrains.annotations.Nullable()
        java.lang.String raw) {
            return null;
        }
        
        private final java.lang.String render(long millis) {
            return null;
        }
        
        private final java.lang.String render(int hour24, java.lang.String minute) {
            return null;
        }
    }
}