package com.example.mumbaitransit.ui;

/**
 * A searchable list of anything.
 *
 * Used for trains, stations and platforms alike — the caller supplies the rows
 * already filtered to the room, so this class never needs to know which line it
 * is showing.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\n\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u0000 .2\u00020\u0001:\u0002./B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010!\u001a\u00020\r2\f\u0010\"\u001a\b\u0012\u0004\u0012\u00020\f0\u0013H\u0002J$\u0010#\u001a\u00020$2\u0006\u0010%\u001a\u00020&2\b\u0010\'\u001a\u0004\u0018\u00010(2\b\u0010)\u001a\u0004\u0018\u00010*H\u0016J\b\u0010+\u001a\u00020\rH\u0016J\u001a\u0010,\u001a\u00020\r2\u0006\u0010-\u001a\u00020$2\b\u0010)\u001a\u0004\u0018\u00010*H\u0016R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0007\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\b\u0010\tR(\u0010\n\u001a\u0010\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\r\u0018\u00010\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000e\u0010\u000f\"\u0004\b\u0010\u0010\u0011R \u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\f0\u0013X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0014\u0010\u0015\"\u0004\b\u0016\u0010\u0017R\u001a\u0010\u0018\u001a\u00020\u0019X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001a\u0010\u001b\"\u0004\b\u001c\u0010\u001dR\u001a\u0010\u001e\u001a\u00020\u0019X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001f\u0010\u001b\"\u0004\b \u0010\u001d\u00a8\u00060"}, d2 = {"Lcom/example/mumbaitransit/ui/OptionPickerSheet;", "Lcom/google/android/material/bottomsheet/BottomSheetDialogFragment;", "()V", "_binding", "Lcom/example/mumbaitransit/databinding/SheetOptionPickerBinding;", "adapter", "Lcom/example/mumbaitransit/ui/OptionAdapter;", "binding", "getBinding", "()Lcom/example/mumbaitransit/databinding/SheetOptionPickerBinding;", "onPicked", "Lkotlin/Function1;", "Lcom/example/mumbaitransit/ui/OptionPickerSheet$Option;", "", "getOnPicked", "()Lkotlin/jvm/functions/Function1;", "setOnPicked", "(Lkotlin/jvm/functions/Function1;)V", "options", "", "getOptions", "()Ljava/util/List;", "setOptions", "(Ljava/util/List;)V", "sheetSubtitle", "", "getSheetSubtitle", "()Ljava/lang/String;", "setSheetSubtitle", "(Ljava/lang/String;)V", "sheetTitle", "getSheetTitle", "setSheetTitle", "apply", "list", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "saved", "Landroid/os/Bundle;", "onDestroyView", "onViewCreated", "view", "Companion", "Option", "app_debug"})
public final class OptionPickerSheet extends com.google.android.material.bottomsheet.BottomSheetDialogFragment {
    @org.jetbrains.annotations.Nullable()
    private com.example.mumbaitransit.databinding.SheetOptionPickerBinding _binding;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String sheetTitle = "";
    @org.jetbrains.annotations.NotNull()
    private java.lang.String sheetSubtitle = "";
    @org.jetbrains.annotations.NotNull()
    private java.util.List<com.example.mumbaitransit.ui.OptionPickerSheet.Option> options;
    @org.jetbrains.annotations.Nullable()
    private kotlin.jvm.functions.Function1<? super com.example.mumbaitransit.ui.OptionPickerSheet.Option, kotlin.Unit> onPicked;
    private com.example.mumbaitransit.ui.OptionAdapter adapter;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TAG = "OptionPickerSheet";
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.ui.OptionPickerSheet.Companion Companion = null;
    
    public OptionPickerSheet() {
        super();
    }
    
    private final com.example.mumbaitransit.databinding.SheetOptionPickerBinding getBinding() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSheetTitle() {
        return null;
    }
    
    public final void setSheetTitle(@org.jetbrains.annotations.NotNull()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSheetSubtitle() {
        return null;
    }
    
    public final void setSheetSubtitle(@org.jetbrains.annotations.NotNull()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.mumbaitransit.ui.OptionPickerSheet.Option> getOptions() {
        return null;
    }
    
    public final void setOptions(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.mumbaitransit.ui.OptionPickerSheet.Option> p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final kotlin.jvm.functions.Function1<com.example.mumbaitransit.ui.OptionPickerSheet.Option, kotlin.Unit> getOnPicked() {
        return null;
    }
    
    public final void setOnPicked(@org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function1<? super com.example.mumbaitransit.ui.OptionPickerSheet.Option, kotlin.Unit> p0) {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public android.view.View onCreateView(@org.jetbrains.annotations.NotNull()
    android.view.LayoutInflater inflater, @org.jetbrains.annotations.Nullable()
    android.view.ViewGroup container, @org.jetbrains.annotations.Nullable()
    android.os.Bundle saved) {
        return null;
    }
    
    @java.lang.Override()
    public void onViewCreated(@org.jetbrains.annotations.NotNull()
    android.view.View view, @org.jetbrains.annotations.Nullable()
    android.os.Bundle saved) {
    }
    
    private final void apply(java.util.List<com.example.mumbaitransit.ui.OptionPickerSheet.Option> list) {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/example/mumbaitransit/ui/OptionPickerSheet$Companion;", "", "()V", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    /**
     * [key] is what gets stored, [title]/[subtitle] are what the user reads.
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B-\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\u0007J\t\u0010\r\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000e\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010\u000f\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\u0010\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J5\u0010\u0011\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0003H\u00c6\u0001J\u0013\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0015\u001a\u00020\u0016H\u00d6\u0001J\t\u0010\u0017\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\tR\u0013\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\tR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\t\u00a8\u0006\u0018"}, d2 = {"Lcom/example/mumbaitransit/ui/OptionPickerSheet$Option;", "", "key", "", "title", "subtitle", "tag", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getKey", "()Ljava/lang/String;", "getSubtitle", "getTag", "getTitle", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class Option {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String key = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String title = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String subtitle = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String tag = null;
        
        public Option(@org.jetbrains.annotations.NotNull()
        java.lang.String key, @org.jetbrains.annotations.NotNull()
        java.lang.String title, @org.jetbrains.annotations.Nullable()
        java.lang.String subtitle, @org.jetbrains.annotations.Nullable()
        java.lang.String tag) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getKey() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTitle() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getSubtitle() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getTag() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component3() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component4() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.ui.OptionPickerSheet.Option copy(@org.jetbrains.annotations.NotNull()
        java.lang.String key, @org.jetbrains.annotations.NotNull()
        java.lang.String title, @org.jetbrains.annotations.Nullable()
        java.lang.String subtitle, @org.jetbrains.annotations.Nullable()
        java.lang.String tag) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}