package com.example.mumbaitransit.ui;

/**
 * Pins a one-tap report to the thing it is about.
 *
 * "Delay" on its own tells nobody anything; "delay on the 09:14 Kalyan fast at
 * Ghatkopar" is actionable. Each [QuickReport] declares what it requires, and
 * the post button stays disabled until those are filled — which is also why the
 * sheet never has to validate anything by hand.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0080\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u0003\u0018\u0000 C2\u00020\u0001:\u0002CDB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\'\u001a\u00020\u00172\u0006\u0010(\u001a\u00020)2\u0006\u0010*\u001a\u00020\u0017H\u0002J$\u0010+\u001a\u00020,2\u0006\u0010-\u001a\u00020.2\b\u0010/\u001a\u0004\u0018\u0001002\b\u00101\u001a\u0004\u0018\u000102H\u0016J\b\u00103\u001a\u00020\u0011H\u0016J\u001a\u00104\u001a\u00020\u00112\u0006\u00105\u001a\u00020,2\b\u00101\u001a\u0004\u0018\u000102H\u0016J:\u00106\u001a\u00020\u00112\u0006\u00107\u001a\u00020\u00172\u0006\u00108\u001a\u00020\u00172\f\u00109\u001a\b\u0012\u0004\u0012\u00020;0:2\u0012\u0010<\u001a\u000e\u0012\u0004\u0012\u00020;\u0012\u0004\u0012\u00020\u00110\u000fH\u0002J\b\u0010=\u001a\u00020\u0011H\u0002J\b\u0010>\u001a\u00020\u0011H\u0002J\b\u0010?\u001a\u00020\u0011H\u0002J\u0010\u0010@\u001a\u00020\u00112\u0006\u0010A\u001a\u00020BH\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007R\u001c\u0010\b\u001a\u0004\u0018\u00010\tX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\n\u0010\u000b\"\u0004\b\f\u0010\rR(\u0010\u000e\u001a\u0010\u0012\u0004\u0012\u00020\u0010\u0012\u0004\u0012\u00020\u0011\u0018\u00010\u000fX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u0017X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0018\u001a\u00020\u0019X\u0086.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001a\u0010\u001b\"\u0004\b\u001c\u0010\u001dR\u001a\u0010\u001e\u001a\u00020\u001fX\u0086.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b \u0010!\"\u0004\b\"\u0010#R\u0010\u0010$\u001a\u0004\u0018\u00010\u0017X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010%\u001a\u0004\u0018\u00010&X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006E"}, d2 = {"Lcom/example/mumbaitransit/ui/ReportComposerSheet;", "Lcom/google/android/material/bottomsheet/BottomSheetDialogFragment;", "()V", "_binding", "Lcom/example/mumbaitransit/databinding/SheetReportComposerBinding;", "binding", "getBinding", "()Lcom/example/mumbaitransit/databinding/SheetReportComposerBinding;", "engine", "Lcom/example/mumbaitransit/engine/TransitEngine;", "getEngine", "()Lcom/example/mumbaitransit/engine/TransitEngine;", "setEngine", "(Lcom/example/mumbaitransit/engine/TransitEngine;)V", "onPost", "Lkotlin/Function1;", "Lcom/example/mumbaitransit/ui/ReportComposerSheet$Result;", "", "getOnPost", "()Lkotlin/jvm/functions/Function1;", "setOnPost", "(Lkotlin/jvm/functions/Function1;)V", "platform", "", "report", "Lcom/example/mumbaitransit/chat/QuickReport;", "getReport", "()Lcom/example/mumbaitransit/chat/QuickReport;", "setReport", "(Lcom/example/mumbaitransit/chat/QuickReport;)V", "room", "Lcom/example/mumbaitransit/chat/ChatRoom;", "getRoom", "()Lcom/example/mumbaitransit/chat/ChatRoom;", "setRoom", "(Lcom/example/mumbaitransit/chat/ChatRoom;)V", "station", "train", "Lcom/example/mumbaitransit/chat/TrainOption;", "labelFor", "attachment", "Lcom/example/mumbaitransit/chat/Attachment;", "base", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "saved", "Landroid/os/Bundle;", "onDestroyView", "onViewCreated", "view", "openPicker", "title", "subtitle", "options", "", "Lcom/example/mumbaitransit/ui/OptionPickerSheet$Option;", "onPicked", "refreshPostButton", "setupPlatformRow", "setupStationRow", "setupTrainRow", "tint", "", "Companion", "Result", "app_debug"})
public final class ReportComposerSheet extends com.google.android.material.bottomsheet.BottomSheetDialogFragment {
    @org.jetbrains.annotations.Nullable()
    private com.example.mumbaitransit.databinding.SheetReportComposerBinding _binding;
    public com.example.mumbaitransit.chat.QuickReport report;
    public com.example.mumbaitransit.chat.ChatRoom room;
    @org.jetbrains.annotations.Nullable()
    private com.example.mumbaitransit.engine.TransitEngine engine;
    @org.jetbrains.annotations.Nullable()
    private kotlin.jvm.functions.Function1<? super com.example.mumbaitransit.ui.ReportComposerSheet.Result, kotlin.Unit> onPost;
    @org.jetbrains.annotations.Nullable()
    private com.example.mumbaitransit.chat.TrainOption train;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String station;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String platform;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TAG = "ReportComposerSheet";
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.ui.ReportComposerSheet.Companion Companion = null;
    
    public ReportComposerSheet() {
        super();
    }
    
    private final com.example.mumbaitransit.databinding.SheetReportComposerBinding getBinding() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.chat.QuickReport getReport() {
        return null;
    }
    
    public final void setReport(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.chat.QuickReport p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.chat.ChatRoom getRoom() {
        return null;
    }
    
    public final void setRoom(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.chat.ChatRoom p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.mumbaitransit.engine.TransitEngine getEngine() {
        return null;
    }
    
    public final void setEngine(@org.jetbrains.annotations.Nullable()
    com.example.mumbaitransit.engine.TransitEngine p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final kotlin.jvm.functions.Function1<com.example.mumbaitransit.ui.ReportComposerSheet.Result, kotlin.Unit> getOnPost() {
        return null;
    }
    
    public final void setOnPost(@org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function1<? super com.example.mumbaitransit.ui.ReportComposerSheet.Result, kotlin.Unit> p0) {
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
    
    private final void setupTrainRow(int tint) {
    }
    
    private final void setupStationRow() {
    }
    
    private final void setupPlatformRow() {
    }
    
    private final java.lang.String labelFor(com.example.mumbaitransit.chat.Attachment attachment, java.lang.String base) {
        return null;
    }
    
    private final void openPicker(java.lang.String title, java.lang.String subtitle, java.util.List<com.example.mumbaitransit.ui.OptionPickerSheet.Option> options, kotlin.jvm.functions.Function1<? super com.example.mumbaitransit.ui.OptionPickerSheet.Option, kotlin.Unit> onPicked) {
    }
    
    /**
     * A report is postable once everything it requires has been chosen.
     */
    private final void refreshPostButton() {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J<\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u000e2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00060\u0010R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lcom/example/mumbaitransit/ui/ReportComposerSheet$Companion;", "", "()V", "TAG", "", "show", "", "fm", "Landroidx/fragment/app/FragmentManager;", "report", "Lcom/example/mumbaitransit/chat/QuickReport;", "room", "Lcom/example/mumbaitransit/chat/ChatRoom;", "engine", "Lcom/example/mumbaitransit/engine/TransitEngine;", "onPost", "Lkotlin/Function1;", "Lcom/example/mumbaitransit/ui/ReportComposerSheet$Result;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        public final void show(@org.jetbrains.annotations.NotNull()
        androidx.fragment.app.FragmentManager fm, @org.jetbrains.annotations.NotNull()
        com.example.mumbaitransit.chat.QuickReport report, @org.jetbrains.annotations.NotNull()
        com.example.mumbaitransit.chat.ChatRoom room, @org.jetbrains.annotations.Nullable()
        com.example.mumbaitransit.engine.TransitEngine engine, @org.jetbrains.annotations.NotNull()
        kotlin.jvm.functions.Function1<? super com.example.mumbaitransit.ui.ReportComposerSheet.Result, kotlin.Unit> onPost) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0015\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B=\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\u0007\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\b\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\t\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0002\u0010\nJ\t\u0010\u0013\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\u0005H\u00c6\u0003J\u000b\u0010\u0015\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u000b\u0010\u0016\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u000b\u0010\u0017\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u000b\u0010\u0018\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003JM\u0010\u0019\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u00052\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00052\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u0005H\u00c6\u0001J\u0013\u0010\u001a\u001a\u00020\u001b2\b\u0010\u001c\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001d\u001a\u00020\u001eH\u00d6\u0001J\t\u0010\u001f\u001a\u00020\u0005H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0013\u0010\t\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0013\u0010\b\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\fR\u0013\u0010\u0007\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\fR\u0013\u0010\u0006\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\f\u00a8\u0006 "}, d2 = {"Lcom/example/mumbaitransit/ui/ReportComposerSheet$Result;", "", "report", "Lcom/example/mumbaitransit/chat/QuickReport;", "note", "", "trainNo", "trainLabel", "station", "platform", "(Lcom/example/mumbaitransit/chat/QuickReport;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getNote", "()Ljava/lang/String;", "getPlatform", "getReport", "()Lcom/example/mumbaitransit/chat/QuickReport;", "getStation", "getTrainLabel", "getTrainNo", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class Result {
        @org.jetbrains.annotations.NotNull()
        private final com.example.mumbaitransit.chat.QuickReport report = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String note = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String trainNo = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String trainLabel = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String station = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String platform = null;
        
        public Result(@org.jetbrains.annotations.NotNull()
        com.example.mumbaitransit.chat.QuickReport report, @org.jetbrains.annotations.NotNull()
        java.lang.String note, @org.jetbrains.annotations.Nullable()
        java.lang.String trainNo, @org.jetbrains.annotations.Nullable()
        java.lang.String trainLabel, @org.jetbrains.annotations.Nullable()
        java.lang.String station, @org.jetbrains.annotations.Nullable()
        java.lang.String platform) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.chat.QuickReport getReport() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getNote() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getTrainNo() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getTrainLabel() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getStation() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getPlatform() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.chat.QuickReport component1() {
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
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component5() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component6() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.ui.ReportComposerSheet.Result copy(@org.jetbrains.annotations.NotNull()
        com.example.mumbaitransit.chat.QuickReport report, @org.jetbrains.annotations.NotNull()
        java.lang.String note, @org.jetbrains.annotations.Nullable()
        java.lang.String trainNo, @org.jetbrains.annotations.Nullable()
        java.lang.String trainLabel, @org.jetbrains.annotations.Nullable()
        java.lang.String station, @org.jetbrains.annotations.Nullable()
        java.lang.String platform) {
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