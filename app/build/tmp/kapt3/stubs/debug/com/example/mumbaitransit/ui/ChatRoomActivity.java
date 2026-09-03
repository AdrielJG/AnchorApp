package com.example.mumbaitransit.ui;

/**
 * One line, one direction.
 *
 * Free text and the nine one-tap reports post into the same stream, so a
 * report and the follow-up question about it sit next to each other.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\n\u0018\u0000 -2\u00020\u0001:\u0001-B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u001dH\u0002J\b\u0010\u001e\u001a\u00020\u001bH\u0002J\u0012\u0010\u001f\u001a\u00020\u001b2\b\u0010 \u001a\u0004\u0018\u00010!H\u0014JB\u0010\"\u001a\u00020\u001b2\b\u0010\u001c\u001a\u0004\u0018\u00010\u001d2\u0006\u0010#\u001a\u00020$2\b\u0010%\u001a\u0004\u0018\u00010$2\b\u0010&\u001a\u0004\u0018\u00010$2\b\u0010\'\u001a\u0004\u0018\u00010$2\b\u0010(\u001a\u0004\u0018\u00010$H\u0002J\b\u0010)\u001a\u00020\u001bH\u0002J\b\u0010*\u001a\u00020\u001bH\u0002J\b\u0010+\u001a\u00020\u001bH\u0002J\b\u0010,\u001a\u00020\u001bH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0005\u001a\u00020\u00068BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\t\u0010\n\u001a\u0004\b\u0007\u0010\bR\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u0016\u0010\r\u001a\u0004\u0018\u00010\u000e8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000f\u0010\u0010R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0013\u001a\u00020\u00148BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0017\u0010\n\u001a\u0004\b\u0015\u0010\u0016R\u000e\u0010\u0018\u001a\u00020\u0019X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006."}, d2 = {"Lcom/example/mumbaitransit/ui/ChatRoomActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "adapter", "Lcom/example/mumbaitransit/ui/ChatAdapter;", "authRepo", "Lcom/example/mumbaitransit/auth/AuthRepository;", "getAuthRepo", "()Lcom/example/mumbaitransit/auth/AuthRepository;", "authRepo$delegate", "Lkotlin/Lazy;", "binding", "Lcom/example/mumbaitransit/databinding/ActivityChatRoomBinding;", "engine", "Lcom/example/mumbaitransit/engine/TransitEngine;", "getEngine", "()Lcom/example/mumbaitransit/engine/TransitEngine;", "lineColor", "", "repo", "Lcom/example/mumbaitransit/chat/ChatRepository;", "getRepo", "()Lcom/example/mumbaitransit/chat/ChatRepository;", "repo$delegate", "room", "Lcom/example/mumbaitransit/chat/ChatRoom;", "compose", "", "report", "Lcom/example/mumbaitransit/chat/QuickReport;", "observeMessages", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "post", "text", "", "trainNo", "trainLabel", "station", "platform", "send", "setupInput", "setupList", "setupQuickReports", "Companion", "app_debug"})
public final class ChatRoomActivity extends androidx.appcompat.app.AppCompatActivity {
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_ROOM_ID = "room_id";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "AnchorChat";
    private com.example.mumbaitransit.databinding.ActivityChatRoomBinding binding;
    private com.example.mumbaitransit.chat.ChatRoom room;
    private com.example.mumbaitransit.ui.ChatAdapter adapter;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy repo$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy authRepo$delegate = null;
    private int lineColor = android.graphics.Color.GRAY;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.ui.ChatRoomActivity.Companion Companion = null;
    
    public ChatRoomActivity() {
        super();
    }
    
    private final com.example.mumbaitransit.chat.ChatRepository getRepo() {
        return null;
    }
    
    private final com.example.mumbaitransit.auth.AuthRepository getAuthRepo() {
        return null;
    }
    
    private final com.example.mumbaitransit.engine.TransitEngine getEngine() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupList() {
    }
    
    private final void observeMessages() {
    }
    
    private final void setupQuickReports() {
    }
    
    private final void setupInput() {
    }
    
    /**
     * Opens the composer so the report can be pinned to a train, a station or a
     * platform. Reports that need nothing attached post straight away.
     */
    private final void compose(com.example.mumbaitransit.chat.QuickReport report) {
    }
    
    /**
     * Plain typed message, no report attached.
     */
    private final void send() {
    }
    
    private final void post(com.example.mumbaitransit.chat.QuickReport report, java.lang.String text, java.lang.String trainNo, java.lang.String trainLabel, java.lang.String station, java.lang.String platform) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/example/mumbaitransit/ui/ChatRoomActivity$Companion;", "", "()V", "EXTRA_ROOM_ID", "", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}