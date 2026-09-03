package com.example.mumbaitransit.share;

/**
 * Turns a [RouteCard] into the picture that gets sent to someone else.
 *
 * The card is inflated, bound and measured off-screen, then drawn straight onto
 * a Bitmap — it is never added to the activity's view tree, so the user sees the
 * share sheet and nothing else. Output is always [OUTPUT_WIDTH_PX] wide whatever
 * the device density, so the same route looks identical shared from any phone.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000|\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u00015B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J0\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0017H\u0002J(\u0010\u0019\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u00062\u0006\u0010\u001d\u001a\u00020\u0006H\u0002J(\u0010\u001e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u001f\u001a\u00020 2\u0006\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020$H\u0002J(\u0010%\u001a\u00020&2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\'\u001a\u00020\u00132\u0006\u0010(\u001a\u00020)2\u0006\u0010*\u001a\u00020\u000bH\u0002J\u0018\u0010+\u001a\u00020\u000b2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010,\u001a\u00020\u000bH\u0002J\u0018\u0010-\u001a\u00020.2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u001a\u001a\u00020&H\u0002J\u001e\u0010/\u001a\u00020.2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020$J\u0016\u00100\u001a\b\u0012\u0004\u0012\u00020\u0015012\u0006\u0010!\u001a\u00020\"H\u0002J\u0010\u00102\u001a\u00020\u00172\u0006\u00103\u001a\u00020\u000bH\u0002J\u001e\u00104\u001a\b\u0012\u0004\u0012\u00020\u0015012\u0006\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020$H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u000bX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u00066"}, d2 = {"Lcom/example/mumbaitransit/share/RouteShareCard;", "", "()V", "AUTO_SUGGEST_KM", "", "COLOR_AC", "", "COLOR_FAST", "COLOR_MEDIUM", "COLOR_SLOW", "LAYOUT_WIDTH_DP", "", "OUTPUT_WIDTH_PX", "WALK_KMPH", "addStep", "", "context", "Landroid/content/Context;", "container", "Landroid/widget/LinearLayout;", "step", "Lcom/example/mumbaitransit/share/RouteShareCard$Step;", "isFirst", "", "isLast", "badge", "view", "Landroid/widget/TextView;", "label", "colorHex", "bind", "b", "Lcom/example/mumbaitransit/databinding/ShareRouteCardBinding;", "card", "Lcom/example/mumbaitransit/model/RouteCard;", "ctx", "Lcom/example/mumbaitransit/share/ShareContext;", "buildTrainBlock", "Landroid/view/View;", "parent", "train", "Lcom/example/mumbaitransit/model/TrainTiming;", "lineColor", "dp", "value", "draw", "Landroid/graphics/Bitmap;", "render", "simpleSteps", "", "suggestAuto", "walkMin", "transitSteps", "Step", "app_debug"})
public final class RouteShareCard {
    
    /**
     * Width the card is laid out at, in dp. Roughly a phone's content width.
     */
    private static final int LAYOUT_WIDTH_DP = 400;
    
    /**
     * Width of the PNG that actually gets shared. Sharp on any messenger.
     */
    private static final int OUTPUT_WIDTH_PX = 1080;
    
    /**
     * Walking further than this is worth an auto instead — same rule as the app.
     */
    private static final double AUTO_SUGGEST_KM = 1.0;
    private static final double WALK_KMPH = 5.0;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String COLOR_FAST = "#DC2626";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String COLOR_SLOW = "#0A7C42";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String COLOR_MEDIUM = "#B45309";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String COLOR_AC = "#0891B2";
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.share.RouteShareCard INSTANCE = null;
    
    private RouteShareCard() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final android.graphics.Bitmap render(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.model.RouteCard card, @org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.share.ShareContext ctx) {
        return null;
    }
    
    private final void bind(android.content.Context context, com.example.mumbaitransit.databinding.ShareRouteCardBinding b, com.example.mumbaitransit.model.RouteCard card, com.example.mumbaitransit.share.ShareContext ctx) {
    }
    
    /**
     * Mirrors RoutesResultActivity.renderJourney: consecutive in-vehicle edges on
     * the same line collapse into one segment, and each segment becomes a single
     * boarding node carrying the train to catch. The alighting station is the
     * next node down — the transfer, or the destination — so it isn't repeated.
     */
    private final java.util.List<com.example.mumbaitransit.share.RouteShareCard.Step> transitSteps(com.example.mumbaitransit.model.RouteCard card, com.example.mumbaitransit.share.ShareContext ctx) {
        return null;
    }
    
    /**
     * Bus / auto / cab cards carry no path, so the card is just the two ends.
     */
    private final java.util.List<com.example.mumbaitransit.share.RouteShareCard.Step> simpleSteps(com.example.mumbaitransit.model.RouteCard card) {
        return null;
    }
    
    private final boolean suggestAuto(int walkMin) {
        return false;
    }
    
    private final void addStep(android.content.Context context, android.widget.LinearLayout container, com.example.mumbaitransit.share.RouteShareCard.Step step, boolean isFirst, boolean isLast) {
    }
    
    /**
     * The train to board. Train numbers are left off deliberately — the
     * destination board shows the terminus, which is what a commuter looks for.
     */
    private final android.view.View buildTrainBlock(android.content.Context context, android.widget.LinearLayout parent, com.example.mumbaitransit.model.TrainTiming train, int lineColor) {
        return null;
    }
    
    /**
     * Same F / S / AC chip the route list draws, so the card reads identically.
     */
    private final void badge(android.content.Context context, android.widget.TextView view, java.lang.String label, java.lang.String colorHex) {
    }
    
    /**
     * Lays the card out at a fixed dp width and draws it onto a bitmap scaled to
     * [OUTPUT_WIDTH_PX]. Scaling the canvas rather than the finished bitmap keeps
     * the text vector-sharp instead of resampled.
     */
    private final android.graphics.Bitmap draw(android.content.Context context, android.view.View view) {
        return null;
    }
    
    private final int dp(android.content.Context context, int value) {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0015\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0082\b\u0018\u00002\u00020\u0001BO\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n\u0012\b\b\u0002\u0010\u000b\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\fJ\t\u0010\u0017\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010\u0018\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\u0019\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010\u001a\u001a\u00020\u0007H\u00c6\u0003J\u000b\u0010\u001b\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\u001c\u001a\u0004\u0018\u00010\nH\u00c6\u0003J\t\u0010\u001d\u001a\u00020\u0007H\u00c6\u0003JW\u0010\u001e\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00072\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n2\b\b\u0002\u0010\u000b\u001a\u00020\u0007H\u00c6\u0001J\u0013\u0010\u001f\u001a\u00020 2\b\u0010!\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\"\u001a\u00020\u0007H\u00d6\u0001J\t\u0010#\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0013\u0010\b\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\u000b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u000eR\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0010R\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0010R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0010R\u0013\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016\u00a8\u0006$"}, d2 = {"Lcom/example/mumbaitransit/share/RouteShareCard$Step;", "", "title", "", "sub", "pill", "dotColor", "", "icon", "train", "Lcom/example/mumbaitransit/model/TrainTiming;", "lineColor", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Lcom/example/mumbaitransit/model/TrainTiming;I)V", "getDotColor", "()I", "getIcon", "()Ljava/lang/String;", "getLineColor", "getPill", "getSub", "getTitle", "getTrain", "()Lcom/example/mumbaitransit/model/TrainTiming;", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
    static final class Step {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String title = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String sub = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String pill = null;
        private final int dotColor = 0;
        
        /**
         * Emoji shown in place of the dot — walk / auto legs read faster this way.
         */
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String icon = null;
        @org.jetbrains.annotations.Nullable()
        private final com.example.mumbaitransit.model.TrainTiming train = null;
        private final int lineColor = 0;
        
        public Step(@org.jetbrains.annotations.NotNull()
        java.lang.String title, @org.jetbrains.annotations.Nullable()
        java.lang.String sub, @org.jetbrains.annotations.Nullable()
        java.lang.String pill, int dotColor, @org.jetbrains.annotations.Nullable()
        java.lang.String icon, @org.jetbrains.annotations.Nullable()
        com.example.mumbaitransit.model.TrainTiming train, int lineColor) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTitle() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getSub() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getPill() {
            return null;
        }
        
        public final int getDotColor() {
            return 0;
        }
        
        /**
         * Emoji shown in place of the dot — walk / auto legs read faster this way.
         */
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getIcon() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.example.mumbaitransit.model.TrainTiming getTrain() {
            return null;
        }
        
        public final int getLineColor() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component3() {
            return null;
        }
        
        public final int component4() {
            return 0;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component5() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.example.mumbaitransit.model.TrainTiming component6() {
            return null;
        }
        
        public final int component7() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.share.RouteShareCard.Step copy(@org.jetbrains.annotations.NotNull()
        java.lang.String title, @org.jetbrains.annotations.Nullable()
        java.lang.String sub, @org.jetbrains.annotations.Nullable()
        java.lang.String pill, int dotColor, @org.jetbrains.annotations.Nullable()
        java.lang.String icon, @org.jetbrains.annotations.Nullable()
        com.example.mumbaitransit.model.TrainTiming train, int lineColor) {
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