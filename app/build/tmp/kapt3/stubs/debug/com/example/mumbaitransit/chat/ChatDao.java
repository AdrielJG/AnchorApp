package com.example.mumbaitransit.chat;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\bg\u0018\u00002\u00020\u0001J\u001e\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u0016\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u0007H\u00a7@\u00a2\u0006\u0002\u0010\fJ\u0016\u0010\r\u001a\u00020\u00072\u0006\u0010\u000e\u001a\u00020\u000fH\u00a7@\u00a2\u0006\u0002\u0010\u0010J\u0014\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0012H\u00a7@\u00a2\u0006\u0002\u0010\u0013J\u001c\u0010\u0014\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000f0\u00120\u00152\u0006\u0010\u0004\u001a\u00020\u0005H\'\u00a8\u0006\u0016"}, d2 = {"Lcom/example/mumbaitransit/chat/ChatDao;", "", "countSince", "", "roomId", "", "since", "", "(Ljava/lang/String;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteOlderThan", "", "before", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insert", "message", "Lcom/example/mumbaitransit/chat/ChatMessage;", "(Lcom/example/mumbaitransit/chat/ChatMessage;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "latestPerRoom", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeRoom", "Lkotlinx/coroutines/flow/Flow;", "app_debug"})
@androidx.room.Dao()
public abstract interface ChatDao {
    
    /**
     * Live message list for one room, oldest first so the list reads downward.
     */
    @androidx.room.Query(value = "SELECT * FROM chat_messages WHERE roomId = :roomId ORDER BY sentAt ASC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.example.mumbaitransit.chat.ChatMessage>> observeRoom(@org.jetbrains.annotations.NotNull()
    java.lang.String roomId);
    
    @androidx.room.Insert()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.chat.ChatMessage message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion);
    
    /**
     * The newest message in each room, for the room list previews. MAX(id)
     * rather than MAX(sentAt) so two messages posted in the same millisecond
     * still resolve to exactly one row per room.
     */
    @androidx.room.Query(value = "SELECT * FROM chat_messages WHERE id IN (SELECT MAX(id) FROM chat_messages GROUP BY roomId)")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object latestPerRoom(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.example.mumbaitransit.chat.ChatMessage>> $completion);
    
    @androidx.room.Query(value = "SELECT COUNT(*) FROM chat_messages WHERE roomId = :roomId AND sentAt > :since")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object countSince(@org.jetbrains.annotations.NotNull()
    java.lang.String roomId, long since, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion);
    
    @androidx.room.Query(value = "DELETE FROM chat_messages WHERE sentAt < :before")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteOlderThan(long before, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}