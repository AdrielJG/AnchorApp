package com.example.mumbaitransit.chat;

/**
 * Live chat over Firebase Realtime Database.
 *
 * Layout: rooms/{roomId}/messages/{pushId}. Push keys are time-ordered, so
 * reading them back in key order gives the conversation in the order it
 * happened without needing a sort, and limitToLast keeps a busy room from
 * pulling months of history onto a phone.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0007\u0018\u0000  2\u00020\u0001:\u0001 B\u0005\u00a2\u0006\u0002\u0010\u0002J\u001a\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006H\u0096@\u00a2\u0006\u0002\u0010\tJ\u001c\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\f0\u000b2\u0006\u0010\r\u001a\u00020\u0007H\u0016J\u001e\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\r\u001a\u00020\u00072\u0006\u0010\u0010\u001a\u00020\u0011H\u0096@\u00a2\u0006\u0002\u0010\u0012J\u0010\u0010\u0013\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u0007H\u0002J`\u0010\u0014\u001a\u00020\u00152\u0006\u0010\r\u001a\u00020\u00072\u0006\u0010\u0016\u001a\u00020\u00072\u0006\u0010\u0017\u001a\u00020\u00072\u0006\u0010\u0018\u001a\u00020\u00072\b\u0010\u0019\u001a\u0004\u0018\u00010\u001a2\b\u0010\u001b\u001a\u0004\u0018\u00010\u00072\b\u0010\u001c\u001a\u0004\u0018\u00010\u00072\b\u0010\u001d\u001a\u0004\u0018\u00010\u00072\b\u0010\u001e\u001a\u0004\u0018\u00010\u0007H\u0096@\u00a2\u0006\u0002\u0010\u001fR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006!"}, d2 = {"Lcom/example/mumbaitransit/chat/FirebaseChatRepository;", "Lcom/example/mumbaitransit/chat/ChatRepository;", "()V", "root", "Lcom/google/firebase/database/DatabaseReference;", "latestPerRoom", "", "", "Lcom/example/mumbaitransit/chat/ChatMessage;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeMessages", "Lkotlinx/coroutines/flow/Flow;", "", "roomId", "recentCount", "", "windowMs", "", "(Ljava/lang/String;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "room", "send", "", "uid", "username", "text", "report", "Lcom/example/mumbaitransit/chat/QuickReport;", "trainNo", "trainLabel", "station", "platform", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/example/mumbaitransit/chat/QuickReport;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_debug"})
public final class FirebaseChatRepository implements com.example.mumbaitransit.chat.ChatRepository {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "AnchorChat";
    
    /**
     * Messages kept in view. Older ones stay in the database, just off-screen.
     */
    private static final int HISTORY = 300;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.database.DatabaseReference root = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.chat.FirebaseChatRepository.Companion Companion = null;
    
    public FirebaseChatRepository() {
        super();
    }
    
    private final com.google.firebase.database.DatabaseReference room(java.lang.String roomId) {
        return null;
    }
    
    /**
     * Emits the room's messages and re-emits on every change.
     *
     * A whole-list listener rather than per-child: the list is capped at
     * [HISTORY] and the UI diffs it anyway, so the simpler contract is worth
     * more than the saved bytes.
     */
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
    
    /**
     * Eight reads in parallel, each allowed to fail on its own.
     *
     * Sequential awaits meant one slow or refused room held up all eight; a
     * thrown one killed the whole call and, with it, the room list that was
     * waiting on it. Previews are cosmetic, so a room that can't be read just
     * comes back absent.
     */
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/example/mumbaitransit/chat/FirebaseChatRepository$Companion;", "", "()V", "HISTORY", "", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}