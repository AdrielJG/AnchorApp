package com.example.mumbaitransit.ui;

/**
 * Live running status for one train, opened from "View live status" on a train
 * timing card in [RoutesResultActivity].
 *
 * The screen is built around one honesty rule: RailRadar will happily return a
 * schedule replay labelled as status, so whenever the position is not an actual
 * sighting the screen says the times are projected instead of presenting them
 * as observed.
 *
 * Requests are metered. Each key allows 1,000 a month, responses are cached for
 * 30 seconds, and auto-refresh pauses itself after [MAX_AUTO_REFRESHES] rounds
 * so a screen left open overnight cannot drain the pool.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u008a\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010#\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\'\n\u0002\u0010\t\n\u0002\b\u0004\u0018\u0000 a2\u00020\u0001:\u0001aB\u0005\u00a2\u0006\u0002\u0010\u0002J(\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u000f2\u0006\u0010\u001c\u001a\u00020\u00062\u0006\u0010\u001d\u001a\u00020\u0006H\u0002J \u0010\u001e\u001a\u00020\u001f2\u0006\u0010 \u001a\u00020!2\u0006\u0010\"\u001a\u00020\u00042\u0006\u0010#\u001a\u00020\u0004H\u0002J\u0010\u0010$\u001a\u00020\u000f2\u0006\u0010 \u001a\u00020!H\u0002J\u0010\u0010%\u001a\u00020\u00062\u0006\u0010&\u001a\u00020\u0006H\u0002J\b\u0010\'\u001a\u00020\u0018H\u0002J\u0018\u0010(\u001a\u00020\u00182\u0006\u0010)\u001a\u00020\u000f2\u0006\u0010*\u001a\u00020\u0004H\u0002J\u001c\u0010+\u001a\b\u0012\u0004\u0012\u00020!0,2\f\u0010-\u001a\b\u0012\u0004\u0012\u00020!0,H\u0002J\u0012\u0010.\u001a\u00020\u00182\b\u0010/\u001a\u0004\u0018\u000100H\u0014J\u0010\u00101\u001a\u00020\u00042\u0006\u00102\u001a\u000203H\u0016J\u0010\u00104\u001a\u00020\u00042\u0006\u00105\u001a\u000206H\u0016J\b\u00107\u001a\u00020\u0018H\u0014J\u0010\u00108\u001a\u00020\u00042\u0006\u00102\u001a\u000203H\u0016J\b\u00109\u001a\u00020\u0018H\u0014J\b\u0010:\u001a\u00020\u0004H\u0016J\u001c\u0010;\u001a\u00020\u000f2\b\u0010<\u001a\u0004\u0018\u00010\u000f2\b\u0010=\u001a\u0004\u0018\u00010\u000fH\u0002J\u0012\u0010>\u001a\u00020\u000f2\b\u0010?\u001a\u0004\u0018\u00010\u000fH\u0002J\b\u0010@\u001a\u00020\u0018H\u0002J\u0010\u0010A\u001a\u00020\u00182\u0006\u0010B\u001a\u00020\u0015H\u0002J\u0017\u0010C\u001a\u00020\u00182\b\u0010D\u001a\u0004\u0018\u00010\u0006H\u0002\u00a2\u0006\u0002\u0010EJ\b\u0010F\u001a\u00020\u0018H\u0002J\u0010\u0010G\u001a\u00020\u00182\u0006\u0010B\u001a\u00020\u0015H\u0002J\u0010\u0010H\u001a\u00020\u00182\u0006\u0010B\u001a\u00020\u0015H\u0002J\u0010\u0010I\u001a\u00020\u00182\u0006\u0010B\u001a\u00020\u0015H\u0002J\u0010\u0010J\u001a\u00020\u00182\u0006\u0010B\u001a\u00020\u0015H\u0002J\u0010\u0010K\u001a\u00020\u00062\u0006\u0010L\u001a\u00020\u0004H\u0002J\u0010\u0010M\u001a\u00020\u00182\u0006\u0010N\u001a\u00020\u0004H\u0002J\u0010\u0010O\u001a\u00020\u00182\u0006\u0010P\u001a\u00020\u001fH\u0002J\b\u0010Q\u001a\u00020\u0018H\u0002J\u0018\u0010R\u001a\u00020\u00182\u0006\u0010S\u001a\u00020\u000f2\u0006\u0010T\u001a\u00020\u0004H\u0002J\b\u0010U\u001a\u00020\u0018H\u0002J\u001c\u0010V\u001a\u00020\u000f2\b\u0010W\u001a\u0004\u0018\u00010\u000f2\b\u0010X\u001a\u0004\u0018\u00010\u000fH\u0002J\b\u0010Y\u001a\u00020\u0018H\u0002J\u0010\u0010Z\u001a\u00020\u000f2\u0006\u0010 \u001a\u00020!H\u0002J\b\u0010[\u001a\u00020\u0018H\u0002J\u0010\u0010\\\u001a\u00020\u000f2\u0006\u0010]\u001a\u00020^H\u0002J\u0010\u0010_\u001a\u00020\u000f2\u0006\u0010?\u001a\u00020\u000fH\u0002J\b\u0010`\u001a\u00020\u0018H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0014\u001a\u0004\u0018\u00010\u0015X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006b"}, d2 = {"Lcom/example/mumbaitransit/ui/LiveStatusActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "autoRefresh", "", "autoRefreshCount", "", "autoRefreshJob", "Lkotlinx/coroutines/Job;", "binding", "Lcom/example/mumbaitransit/databinding/ActivityLiveStatusBinding;", "client", "Lcom/example/mumbaitransit/live/RailRadarClient;", "expandedStops", "", "", "legSignature", "numbers", "Lcom/example/mumbaitransit/live/TrainNumberStore;", "showAllStops", "status", "Lcom/example/mumbaitransit/live/LiveTrainStatus;", "trainNumber", "applyPill", "", "view", "Landroid/widget/TextView;", "text", "fg", "bg", "buildStopRow", "Landroid/view/View;", "stop", "Lcom/example/mumbaitransit/live/RouteStop;", "isFirst", "isLast", "buildSubtitle", "dp", "value", "hideKeyboard", "load", "number", "force", "nearbyWindow", "", "route", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onCreateOptionsMenu", "menu", "Landroid/view/Menu;", "onOptionsItemSelected", "item", "Landroid/view/MenuItem;", "onPause", "onPrepareOptionsMenu", "onResume", "onSupportNavigateUp", "pairTimes", "scheduled", "actual", "positionLabel", "raw", "refreshInPlace", "render", "s", "renderDelayChip", "delay", "(Ljava/lang/Integer;)V", "renderFooter", "renderLiveBadge", "renderPosition", "renderStatusLine", "renderStops", "segColor", "active", "setStopFilter", "showAll", "show", "state", "showAskState", "showError", "message", "retryable", "startAutoRefresh", "stationLabel", "name", "code", "stopAutoRefresh", "stopKey", "submitNumber", "timeAgo", "millis", "", "titleCase", "wireControls", "Companion", "app_debug"})
public final class LiveStatusActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.example.mumbaitransit.databinding.ActivityLiveStatusBinding binding;
    private com.example.mumbaitransit.live.RailRadarClient client;
    private com.example.mumbaitransit.live.TrainNumberStore numbers;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String trainNumber;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String legSignature = "";
    @org.jetbrains.annotations.Nullable()
    private com.example.mumbaitransit.live.LiveTrainStatus status;
    private boolean showAllStops = false;
    private boolean autoRefresh = true;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job autoRefreshJob;
    private int autoRefreshCount = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> expandedStops = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String EXTRA_TRAIN_NO = "train_no";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String EXTRA_LINE = "line";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String EXTRA_FROM = "from_stop";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String EXTRA_TO = "to_stop";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String EXTRA_DEPARTURE = "departure";
    private static final long REFRESH_INTERVAL_MS = 60000L;
    private static final int MAX_AUTO_REFRESHES = 20;
    private static final int WINDOW_BEHIND = 2;
    private static final int WINDOW_AHEAD = 5;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.ui.LiveStatusActivity.Companion Companion = null;
    
    public LiveStatusActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void wireControls() {
    }
    
    private final void show(android.view.View state) {
    }
    
    /**
     * Fallback when the timetable has no number for this service, or the user corrects one.
     */
    private final void showAskState() {
    }
    
    private final void submitNumber() {
    }
    
    private final void load(java.lang.String number, boolean force) {
    }
    
    /**
     * Refresh triggered while content is already on screen — keeps the old view visible.
     */
    private final void refreshInPlace() {
    }
    
    private final void showError(java.lang.String message, boolean retryable) {
    }
    
    private final void render(com.example.mumbaitransit.live.LiveTrainStatus s) {
    }
    
    private final void renderLiveBadge(com.example.mumbaitransit.live.LiveTrainStatus s) {
    }
    
    private final void renderStatusLine(com.example.mumbaitransit.live.LiveTrainStatus s) {
    }
    
    private final void renderPosition(com.example.mumbaitransit.live.LiveTrainStatus s) {
    }
    
    private final void renderDelayChip(java.lang.Integer delay) {
    }
    
    private final void setStopFilter(boolean showAll) {
    }
    
    private final void renderStops(com.example.mumbaitransit.live.LiveTrainStatus s) {
    }
    
    /**
     * Trims the route to what a passenger actually looks at: the last stop passed,
     * the current one, the next few, and the destination.
     */
    private final java.util.List<com.example.mumbaitransit.live.RouteStop> nearbyWindow(java.util.List<com.example.mumbaitransit.live.RouteStop> route) {
        return null;
    }
    
    private final android.view.View buildStopRow(com.example.mumbaitransit.live.RouteStop stop, boolean isFirst, boolean isLast) {
        return null;
    }
    
    /**
     * Stable per-stop key; sequence alone is unreliable when the API omits it.
     */
    private final java.lang.String stopKey(com.example.mumbaitransit.live.RouteStop stop) {
        return null;
    }
    
    private final java.lang.String buildSubtitle(com.example.mumbaitransit.live.RouteStop stop) {
        return null;
    }
    
    private final java.lang.String pairTimes(java.lang.String scheduled, java.lang.String actual) {
        return null;
    }
    
    private final void renderFooter() {
    }
    
    private final void startAutoRefresh() {
    }
    
    private final void stopAutoRefresh() {
    }
    
    @java.lang.Override()
    protected void onPause() {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    @java.lang.Override()
    public boolean onCreateOptionsMenu(@org.jetbrains.annotations.NotNull()
    android.view.Menu menu) {
        return false;
    }
    
    @java.lang.Override()
    public boolean onPrepareOptionsMenu(@org.jetbrains.annotations.NotNull()
    android.view.Menu menu) {
        return false;
    }
    
    @java.lang.Override()
    public boolean onOptionsItemSelected(@org.jetbrains.annotations.NotNull()
    android.view.MenuItem item) {
        return false;
    }
    
    @java.lang.Override()
    public boolean onSupportNavigateUp() {
        return false;
    }
    
    private final void applyPill(android.widget.TextView view, java.lang.String text, int fg, int bg) {
    }
    
    private final int segColor(boolean active) {
        return 0;
    }
    
    private final java.lang.String stationLabel(java.lang.String name, java.lang.String code) {
        return null;
    }
    
    private final java.lang.String positionLabel(java.lang.String raw) {
        return null;
    }
    
    /**
     * RailRadar sends ALL-CAPS station names; this makes them readable.
     */
    private final java.lang.String titleCase(java.lang.String raw) {
        return null;
    }
    
    private final java.lang.String timeAgo(long millis) {
        return null;
    }
    
    private final void hideKeyboard() {
    }
    
    private final int dp(int value) {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J:\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00042\u0006\u0010\u0014\u001a\u00020\u00042\u0006\u0010\u0015\u001a\u00020\u00042\u0006\u0010\u0016\u001a\u00020\u00042\n\b\u0002\u0010\u0017\u001a\u0004\u0018\u00010\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\nX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\nX\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lcom/example/mumbaitransit/ui/LiveStatusActivity$Companion;", "", "()V", "EXTRA_DEPARTURE", "", "EXTRA_FROM", "EXTRA_LINE", "EXTRA_TO", "EXTRA_TRAIN_NO", "MAX_AUTO_REFRESHES", "", "REFRESH_INTERVAL_MS", "", "WINDOW_AHEAD", "WINDOW_BEHIND", "start", "", "context", "Landroid/content/Context;", "line", "fromStop", "toStop", "departure", "trainNo", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        /**
         * Opens live tracking for one leg. [trainNo] may be null — the screen then
         * asks for it once and remembers it against this leg.
         */
        public final void start(@org.jetbrains.annotations.NotNull()
        android.content.Context context, @org.jetbrains.annotations.NotNull()
        java.lang.String line, @org.jetbrains.annotations.NotNull()
        java.lang.String fromStop, @org.jetbrains.annotations.NotNull()
        java.lang.String toStop, @org.jetbrains.annotations.NotNull()
        java.lang.String departure, @org.jetbrains.annotations.Nullable()
        java.lang.String trainNo) {
        }
    }
}