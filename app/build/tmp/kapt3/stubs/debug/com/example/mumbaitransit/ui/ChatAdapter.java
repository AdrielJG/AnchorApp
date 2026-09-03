package com.example.mumbaitransit.ui;

/**
 * Renders the stream. Own messages sit right and filled; everyone else's sit
 * left with the sender's name in the line colour.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0002\u0018\u0000 \u00112\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00030\u0001:\u0002\u0011\u0012B\u0015\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\u0007H\u0016J\u0018\u0010\r\u001a\u00020\u00032\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0007H\u0016R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/example/mumbaitransit/ui/ChatAdapter;", "Landroidx/recyclerview/widget/ListAdapter;", "Lcom/example/mumbaitransit/chat/ChatMessage;", "Lcom/example/mumbaitransit/ui/ChatAdapter$VH;", "myUid", "", "lineColor", "", "(Ljava/lang/String;I)V", "onBindViewHolder", "", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "Companion", "VH", "app_debug"})
final class ChatAdapter extends androidx.recyclerview.widget.ListAdapter<com.example.mumbaitransit.chat.ChatMessage, com.example.mumbaitransit.ui.ChatAdapter.VH> {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String myUid = null;
    private final int lineColor = 0;
    @org.jetbrains.annotations.NotNull()
    private static final androidx.recyclerview.widget.DiffUtil.ItemCallback<com.example.mumbaitransit.chat.ChatMessage> DIFF = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.text.SimpleDateFormat CLOCK = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.ui.ChatAdapter.Companion Companion = null;
    
    public ChatAdapter(@org.jetbrains.annotations.NotNull()
    java.lang.String myUid, int lineColor) {
        super(null);
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.example.mumbaitransit.ui.ChatAdapter.VH onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.ui.ChatAdapter.VH holder, int position) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/example/mumbaitransit/ui/ChatAdapter$Companion;", "", "()V", "CLOCK", "Ljava/text/SimpleDateFormat;", "DIFF", "Landroidx/recyclerview/widget/DiffUtil$ItemCallback;", "Lcom/example/mumbaitransit/chat/ChatMessage;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/example/mumbaitransit/ui/ChatAdapter$VH;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "b", "Lcom/example/mumbaitransit/databinding/ItemChatMessageBinding;", "(Lcom/example/mumbaitransit/databinding/ItemChatMessageBinding;)V", "getB", "()Lcom/example/mumbaitransit/databinding/ItemChatMessageBinding;", "app_debug"})
    public static final class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final com.example.mumbaitransit.databinding.ItemChatMessageBinding b = null;
        
        public VH(@org.jetbrains.annotations.NotNull()
        com.example.mumbaitransit.databinding.ItemChatMessageBinding b) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.databinding.ItemChatMessageBinding getB() {
            return null;
        }
    }
}