package com.example.mumbaitransit.chat;

/**
 * Which way the train is heading.
 *
 * Mumbai suburban convention: Up runs towards the city terminus, Down runs away
 * from it. Commuters on opposite platforms have almost nothing useful to tell
 * each other, which is why every line gets two rooms rather than one.
 *
 * [key] must match the `direction` column in phase2_unified_enriched.csv — it is
 * what filters the train picker down to this room's services.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\b\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0017\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0007j\u0002\b\tj\u0002\b\n\u00a8\u0006\u000b"}, d2 = {"Lcom/example/mumbaitransit/chat/Direction;", "", "label", "", "key", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V", "getKey", "()Ljava/lang/String;", "getLabel", "UP", "DOWN", "app_debug"})
public enum Direction {
    /*public static final*/ UP /* = new UP(null, null) */,
    /*public static final*/ DOWN /* = new DOWN(null, null) */;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String label = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String key = null;
    
    Direction(java.lang.String label, java.lang.String key) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLabel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getKey() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.example.mumbaitransit.chat.Direction> getEntries() {
        return null;
    }
}