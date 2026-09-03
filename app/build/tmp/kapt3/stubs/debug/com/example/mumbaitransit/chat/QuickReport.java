package com.example.mumbaitransit.chat;

/**
 * The nine one-tap reports.
 *
 * These exist because the moment worth reporting — a train that just stopped
 * dead outside Kurla — is the moment a commuter least wants to type. Each one
 * declares what it must be pinned to: "delay" with no train attached is noise,
 * and a safety alert only means something if you know which station.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0002\b\u0016\b\u0086\u0081\u0002\u0018\u0000 \u001d2\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\u001dB=\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u0012\u000e\b\u0002\u0010\t\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\u0002\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\fR\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000fR\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\fR\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\b0\u00078F\u00a2\u0006\u0006\u001a\u0004\b\u0013\u0010\u000fj\u0002\b\u0014j\u0002\b\u0015j\u0002\b\u0016j\u0002\b\u0017j\u0002\b\u0018j\u0002\b\u0019j\u0002\b\u001aj\u0002\b\u001bj\u0002\b\u001c\u00a8\u0006\u001e"}, d2 = {"Lcom/example/mumbaitransit/chat/QuickReport;", "", "label", "", "emoji", "tint", "requires", "", "Lcom/example/mumbaitransit/chat/Attachment;", "optional", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/Set;Ljava/util/Set;)V", "getEmoji", "()Ljava/lang/String;", "getLabel", "getOptional", "()Ljava/util/Set;", "getRequires", "getTint", "uses", "getUses", "DELAY", "CANCELLED", "EARLY", "SLOW", "PLATFORM_CHANGE", "CROWDING", "STATION_ISSUE", "DISRUPTION", "SAFETY", "Companion", "app_debug"})
public enum QuickReport {
    /*public static final*/ DELAY /* = new DELAY(null, null, null, null, null) */,
    /*public static final*/ CANCELLED /* = new CANCELLED(null, null, null, null, null) */,
    /*public static final*/ EARLY /* = new EARLY(null, null, null, null, null) */,
    /*public static final*/ SLOW /* = new SLOW(null, null, null, null, null) */,
    /*public static final*/ PLATFORM_CHANGE /* = new PLATFORM_CHANGE(null, null, null, null, null) */,
    /*public static final*/ CROWDING /* = new CROWDING(null, null, null, null, null) */,
    /*public static final*/ STATION_ISSUE /* = new STATION_ISSUE(null, null, null, null, null) */,
    /*public static final*/ DISRUPTION /* = new DISRUPTION(null, null, null, null, null) */,
    /*public static final*/ SAFETY /* = new SAFETY(null, null, null, null, null) */;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String label = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String emoji = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String tint = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<com.example.mumbaitransit.chat.Attachment> requires = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<com.example.mumbaitransit.chat.Attachment> optional = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.chat.QuickReport.Companion Companion = null;
    
    QuickReport(java.lang.String label, java.lang.String emoji, java.lang.String tint, java.util.Set<? extends com.example.mumbaitransit.chat.Attachment> requires, java.util.Set<? extends com.example.mumbaitransit.chat.Attachment> optional) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLabel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getEmoji() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getTint() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<com.example.mumbaitransit.chat.Attachment> getRequires() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<com.example.mumbaitransit.chat.Attachment> getOptional() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<com.example.mumbaitransit.chat.Attachment> getUses() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.example.mumbaitransit.chat.QuickReport> getEntries() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u0004\u0018\u00010\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/example/mumbaitransit/chat/QuickReport$Companion;", "", "()V", "from", "Lcom/example/mumbaitransit/chat/QuickReport;", "name", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.example.mumbaitransit.chat.QuickReport from(@org.jetbrains.annotations.Nullable()
        java.lang.String name) {
            return null;
        }
    }
}