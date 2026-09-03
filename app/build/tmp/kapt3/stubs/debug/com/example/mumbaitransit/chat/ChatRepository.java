package com.example.mumbaitransit.chat;

/**
 * Where chat messages live.
 *
 * The screens talk to this interface only. With Firebase configured they reach
 * every device; without it they fall back to the on-device store, so the app
 * still builds and runs before the backend exists. See FIREBASE_SETUP.md.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0007\bf\u0018\u0000 \u001c2\u00020\u0001:\u0001\u001cJ\u001a\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u0003H\u00a6@\u00a2\u0006\u0002\u0010\u0006J\u001c\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\t0\b2\u0006\u0010\n\u001a\u00020\u0004H&J\u001e\u0010\u000b\u001a\u00020\f2\u0006\u0010\n\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u000eH\u00a6@\u00a2\u0006\u0002\u0010\u000fJj\u0010\u0010\u001a\u00020\u00112\u0006\u0010\n\u001a\u00020\u00042\u0006\u0010\u0012\u001a\u00020\u00042\u0006\u0010\u0013\u001a\u00020\u00042\u0006\u0010\u0014\u001a\u00020\u00042\n\b\u0002\u0010\u0015\u001a\u0004\u0018\u00010\u00162\n\b\u0002\u0010\u0017\u001a\u0004\u0018\u00010\u00042\n\b\u0002\u0010\u0018\u001a\u0004\u0018\u00010\u00042\n\b\u0002\u0010\u0019\u001a\u0004\u0018\u00010\u00042\n\b\u0002\u0010\u001a\u001a\u0004\u0018\u00010\u0004H\u00a6@\u00a2\u0006\u0002\u0010\u001b\u00a8\u0006\u001d"}, d2 = {"Lcom/example/mumbaitransit/chat/ChatRepository;", "", "latestPerRoom", "", "", "Lcom/example/mumbaitransit/chat/ChatMessage;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeMessages", "Lkotlinx/coroutines/flow/Flow;", "", "roomId", "recentCount", "", "windowMs", "", "(Ljava/lang/String;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "send", "", "uid", "username", "text", "report", "Lcom/example/mumbaitransit/chat/QuickReport;", "trainNo", "trainLabel", "station", "platform", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/example/mumbaitransit/chat/QuickReport;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_debug"})
public abstract interface ChatRepository {
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.chat.ChatRepository.Companion Companion = null;
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.example.mumbaitransit.chat.ChatMessage>> observeMessages(@org.jetbrains.annotations.NotNull()
    java.lang.String roomId);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object send(@org.jetbrains.annotations.NotNull()
    java.lang.String roomId, @org.jetbrains.annotations.NotNull()
    java.lang.String uid, @org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.Nullable()
    com.example.mumbaitransit.chat.QuickReport report, @org.jetbrains.annotations.Nullable()
    java.lang.String trainNo, @org.jetbrains.annotations.Nullable()
    java.lang.String trainLabel, @org.jetbrains.annotations.Nullable()
    java.lang.String station, @org.jetbrains.annotations.Nullable()
    java.lang.String platform, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Newest message per room, keyed by room id. Empty rooms are absent.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object latestPerRoom(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, com.example.mumbaitransit.chat.ChatMessage>> $completion);
    
    /**
     * Messages posted in the last [windowMs], for the "active now" hint.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object recentCount(@org.jetbrains.annotations.NotNull()
    java.lang.String roomId, long windowMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u0007H\u0002J\u000e\u0010\b\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u0007R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/example/mumbaitransit/chat/ChatRepository$Companion;", "", "()V", "instance", "Lcom/example/mumbaitransit/chat/ChatRepository;", "build", "context", "Landroid/content/Context;", "get", "app_debug"})
    public static final class Companion {
        @kotlin.jvm.Volatile()
        @org.jetbrains.annotations.Nullable()
        private static volatile com.example.mumbaitransit.chat.ChatRepository instance;
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.chat.ChatRepository get(@org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
        
        private final com.example.mumbaitransit.chat.ChatRepository build(android.content.Context context) {
            return null;
        }
    }
    
    /**
     * Where chat messages live.
     *
     * The screens talk to this interface only. With Firebase configured they reach
     * every device; without it they fall back to the on-device store, so the app
     * still builds and runs before the backend exists. See FIREBASE_SETUP.md.
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}