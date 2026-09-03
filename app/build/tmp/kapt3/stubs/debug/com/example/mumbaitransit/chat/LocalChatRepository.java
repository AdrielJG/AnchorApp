package com.example.mumbaitransit.chat;

/**
 * On-device fallback.
 *
 * Messages are real and persist across restarts, but they never leave the
 * phone. Used only when Firebase has not been configured yet.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u001a\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006H\u0096@\u00a2\u0006\u0002\u0010\tJ\u001c\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\f0\u000b2\u0006\u0010\r\u001a\u00020\u0007H\u0016J\u001e\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\r\u001a\u00020\u00072\u0006\u0010\u0010\u001a\u00020\u0011H\u0096@\u00a2\u0006\u0002\u0010\u0012J`\u0010\u0013\u001a\u00020\u00142\u0006\u0010\r\u001a\u00020\u00072\u0006\u0010\u0015\u001a\u00020\u00072\u0006\u0010\u0016\u001a\u00020\u00072\u0006\u0010\u0017\u001a\u00020\u00072\b\u0010\u0018\u001a\u0004\u0018\u00010\u00192\b\u0010\u001a\u001a\u0004\u0018\u00010\u00072\b\u0010\u001b\u001a\u0004\u0018\u00010\u00072\b\u0010\u001c\u001a\u0004\u0018\u00010\u00072\b\u0010\u001d\u001a\u0004\u0018\u00010\u0007H\u0096@\u00a2\u0006\u0002\u0010\u001eR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001f"}, d2 = {"Lcom/example/mumbaitransit/chat/LocalChatRepository;", "Lcom/example/mumbaitransit/chat/ChatRepository;", "dao", "Lcom/example/mumbaitransit/chat/ChatDao;", "(Lcom/example/mumbaitransit/chat/ChatDao;)V", "latestPerRoom", "", "", "Lcom/example/mumbaitransit/chat/ChatMessage;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeMessages", "Lkotlinx/coroutines/flow/Flow;", "", "roomId", "recentCount", "", "windowMs", "", "(Ljava/lang/String;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "send", "", "uid", "username", "text", "report", "Lcom/example/mumbaitransit/chat/QuickReport;", "trainNo", "trainLabel", "station", "platform", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/example/mumbaitransit/chat/QuickReport;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class LocalChatRepository implements com.example.mumbaitransit.chat.ChatRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.example.mumbaitransit.chat.ChatDao dao = null;
    
    public LocalChatRepository(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.chat.ChatDao dao) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.example.mumbaitransit.chat.ChatMessage>> observeMessages(@org.jetbrains.annotations.NotNull()
    java.lang.String roomId) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object send(@org.jetbrains.annotations.NotNull()
    java.lang.String roomId, @org.jetbrains.annotations.NotNull()
    java.lang.String uid, @org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.Nullable()
    com.example.mumbaitransit.chat.QuickReport report, @org.jetbrains.annotations.Nullable()
    java.lang.String trainNo, @org.jetbrains.annotations.Nullable()
    java.lang.String trainLabel, @org.jetbrains.annotations.Nullable()
    java.lang.String station, @org.jetbrains.annotations.Nullable()
    java.lang.String platform, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object latestPerRoom(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, com.example.mumbaitransit.chat.ChatMessage>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object recentCount(@org.jetbrains.annotations.NotNull()
    java.lang.String roomId, long windowMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
}