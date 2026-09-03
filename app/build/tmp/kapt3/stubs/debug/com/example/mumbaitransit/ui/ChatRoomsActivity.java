package com.example.mumbaitransit.ui;

/**
 * The eight line rooms.
 *
 * The rows are drawn from [ChatRooms.all], which is static data, so they render
 * before anything touches the network. Last-message previews arrive afterwards
 * and are allowed to fail — a slow or refused backend read should cost you the
 * preview text, never the list itself.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0003\u0018\u0000 \u001f2\u00020\u0001:\u0001\u001fB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\u0010\u001a\u00020\u0011H\u0002J\b\u0010\u0012\u001a\u00020\u0013H\u0002J\u0012\u0010\u0014\u001a\u00020\u00132\b\u0010\u0015\u001a\u0004\u0018\u00010\u0016H\u0014J\b\u0010\u0017\u001a\u00020\u0013H\u0014J\u0010\u0010\u0018\u001a\u00020\r2\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J\u0010\u0010\u001b\u001a\u00020\r2\u0006\u0010\u001c\u001a\u00020\u001dH\u0002J\b\u0010\u001e\u001a\u00020\u0013H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0005\u001a\u00020\u00068BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\t\u0010\n\u001a\u0004\b\u0007\u0010\bR\u001a\u0010\u000b\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\u000e0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006 "}, d2 = {"Lcom/example/mumbaitransit/ui/ChatRoomsActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/example/mumbaitransit/databinding/ActivityChatRoomsBinding;", "repo", "Lcom/example/mumbaitransit/chat/ChatRepository;", "getRepo", "()Lcom/example/mumbaitransit/chat/ChatRepository;", "repo$delegate", "Lkotlin/Lazy;", "rows", "", "", "Lcom/example/mumbaitransit/databinding/ItemChatRoomBinding;", "addRoomRow", "room", "Lcom/example/mumbaitransit/chat/ChatRoom;", "loadPreviews", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onResume", "preview", "message", "Lcom/example/mumbaitransit/chat/ChatMessage;", "relativeTime", "millis", "", "renderRooms", "Companion", "app_debug"})
public final class ChatRoomsActivity extends androidx.appcompat.app.AppCompatActivity {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "AnchorChatRooms";
    
    /**
     * Past this, previews aren't worth waiting for.
     */
    private static final long PREVIEW_TIMEOUT_MS = 6000L;
    private com.example.mumbaitransit.databinding.ActivityChatRoomsBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy repo$delegate = null;
    
    /**
     * Room id -> its row, so previews can be filled in after the fact.
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, com.example.mumbaitransit.databinding.ItemChatRoomBinding> rows = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.ui.ChatRoomsActivity.Companion Companion = null;
    
    public ChatRoomsActivity() {
        super();
    }
    
    private final com.example.mumbaitransit.chat.ChatRepository getRepo() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    private final void renderRooms() {
    }
    
    private final com.example.mumbaitransit.databinding.ItemChatRoomBinding addRoomRow(com.example.mumbaitransit.chat.ChatRoom room) {
        return null;
    }
    
    private final void loadPreviews() {
    }
    
    private final java.lang.String preview(com.example.mumbaitransit.chat.ChatMessage message) {
        return null;
    }
    
    private final java.lang.String relativeTime(long millis) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/example/mumbaitransit/ui/ChatRoomsActivity$Companion;", "", "()V", "PREVIEW_TIMEOUT_MS", "", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}