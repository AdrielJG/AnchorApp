package com.example.mumbaitransit.live;

/**
 * Fallback store for train numbers the timetable can't supply.
 *
 * Rail services now carry a real `train_no` from the timetable, so this is
 * rarely reached. It still covers the gaps: a service with a blank number, or a
 * corrected number the user enters after RailRadar rejects the one on file.
 * Numbers are keyed to a leg signature (line, stops, departure time), so a
 * correction sticks to that service and no other.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\b\u0018\u0000 \u00152\u00020\u0001:\u0001\u0015B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bJ\u0010\u0010\f\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\n\u001a\u00020\u000bJ\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000eJ\u0016\u0010\u000f\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0010\u001a\u00020\u000bJ&\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0011\u001a\u00020\u000b2\u0006\u0010\u0012\u001a\u00020\u000b2\u0006\u0010\u0013\u001a\u00020\u000b2\u0006\u0010\u0014\u001a\u00020\u000bR\u0016\u0010\u0005\u001a\n \u0007*\u0004\u0018\u00010\u00060\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lcom/example/mumbaitransit/live/TrainNumberStore;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "prefs", "Landroid/content/SharedPreferences;", "kotlin.jvm.PlatformType", "forget", "", "signature", "", "lookup", "recents", "", "remember", "trainNumber", "line", "from", "to", "departure", "Companion", "app_debug"})
public final class TrainNumberStore {
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String KEY_RECENTS = "recents";
    @java.lang.Deprecated()
    public static final int MAX_RECENTS = 8;
    @org.jetbrains.annotations.NotNull()
    private static final com.example.mumbaitransit.live.TrainNumberStore.Companion Companion = null;
    
    public TrainNumberStore(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    /**
     * Stable identifier for a leg, independent of how it was rendered.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String signature(@org.jetbrains.annotations.NotNull()
    java.lang.String line, @org.jetbrains.annotations.NotNull()
    java.lang.String from, @org.jetbrains.annotations.NotNull()
    java.lang.String to, @org.jetbrains.annotations.NotNull()
    java.lang.String departure) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String lookup(@org.jetbrains.annotations.NotNull()
    java.lang.String signature) {
        return null;
    }
    
    public final void remember(@org.jetbrains.annotations.NotNull()
    java.lang.String signature, @org.jetbrains.annotations.NotNull()
    java.lang.String trainNumber) {
    }
    
    public final void forget(@org.jetbrains.annotations.NotNull()
    java.lang.String signature) {
    }
    
    /**
     * Recently tracked numbers, newest first — offered as one-tap chips.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> recents() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\b\u0082\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/example/mumbaitransit/live/TrainNumberStore$Companion;", "", "()V", "KEY_RECENTS", "", "MAX_RECENTS", "", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
    }
}