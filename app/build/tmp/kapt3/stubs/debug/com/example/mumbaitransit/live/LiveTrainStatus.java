package com.example.mumbaitransit.live;

/**
 * Parsed shape of RailRadar's `/v1/trains/{no}/live` response.
 *
 * Every field is nullable on purpose. The API returns different subsets depending
 * on whether a train is actually being tracked or is merely scheduled, so the UI
 * is built to degrade rather than crash on a missing key.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b,\b\u0086\b\u0018\u00002\u00020\u0001B\u0091\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0007\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\b\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\t\u001a\u00020\n\u0012\b\u0010\u000b\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\f\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\r\u001a\u0004\u0018\u00010\u000e\u0012\b\u0010\u000f\u001a\u0004\u0018\u00010\u0010\u0012\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012\u0012\b\u0010\u0013\u001a\u0004\u0018\u00010\u0012\u0012\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u0015\u00a2\u0006\u0002\u0010\u0017J\t\u00103\u001a\u00020\u0003H\u00c6\u0003J\u0010\u00104\u001a\u0004\u0018\u00010\u000eH\u00c6\u0003\u00a2\u0006\u0002\u0010&J\u000b\u00105\u001a\u0004\u0018\u00010\u0010H\u00c6\u0003J\u000b\u00106\u001a\u0004\u0018\u00010\u0012H\u00c6\u0003J\u000b\u00107\u001a\u0004\u0018\u00010\u0012H\u00c6\u0003J\u000f\u00108\u001a\b\u0012\u0004\u0012\u00020\u00160\u0015H\u00c6\u0003J\u000b\u00109\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010:\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010;\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010<\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010=\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010>\u001a\u00020\nH\u00c6\u0003J\u000b\u0010?\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010@\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u00b6\u0001\u0010A\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\t\u001a\u00020\n2\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u000e2\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u00102\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u00122\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\u00122\u000e\b\u0002\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u0015H\u00c6\u0001\u00a2\u0006\u0002\u0010BJ\u0013\u0010C\u001a\u00020\n2\b\u0010D\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010E\u001a\u00020\u001bH\u00d6\u0001J\t\u0010F\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\u000f\u001a\u0004\u0018\u00010\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0013\u0010\u001a\u001a\u0004\u0018\u00010\u001b8F\u00a2\u0006\u0006\u001a\u0004\b\u001c\u0010\u001dR\u0013\u0010\u0007\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001fR\u0013\u0010\b\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u001fR\u0011\u0010!\u001a\u00020\u00038F\u00a2\u0006\u0006\u001a\u0004\b\"\u0010\u001fR\u0011\u0010#\u001a\u00020\n8F\u00a2\u0006\u0006\u001a\u0004\b#\u0010$R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010$R\u0015\u0010\r\u001a\u0004\u0018\u00010\u000e\u00a2\u0006\n\n\u0002\u0010\'\u001a\u0004\b%\u0010&R\u0013\u0010\u0013\u001a\u0004\u0018\u00010\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010)R\u0013\u0010\u0011\u001a\u0004\u0018\u00010\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010)R\u0017\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010,R\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010\u001fR\u0013\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b.\u0010\u001fR\u0013\u0010\f\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u0010\u001fR\u0013\u0010\u000b\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u0010\u001fR\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b1\u0010\u001fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b2\u0010\u001f\u00a8\u0006G"}, d2 = {"Lcom/example/mumbaitransit/live/LiveTrainStatus;", "", "trainNumber", "", "trainName", "sourceCode", "sourceName", "destCode", "destName", "isLive", "", "trackingMode", "statusText", "lastUpdatedAt", "", "current", "Lcom/example/mumbaitransit/live/CurrentPosition;", "previousHalt", "Lcom/example/mumbaitransit/live/Halt;", "nextHalt", "route", "", "Lcom/example/mumbaitransit/live/RouteStop;", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/Long;Lcom/example/mumbaitransit/live/CurrentPosition;Lcom/example/mumbaitransit/live/Halt;Lcom/example/mumbaitransit/live/Halt;Ljava/util/List;)V", "getCurrent", "()Lcom/example/mumbaitransit/live/CurrentPosition;", "delayMinutes", "", "getDelayMinutes", "()Ljava/lang/Integer;", "getDestCode", "()Ljava/lang/String;", "getDestName", "headerTitle", "getHeaderTitle", "isActualPosition", "()Z", "getLastUpdatedAt", "()Ljava/lang/Long;", "Ljava/lang/Long;", "getNextHalt", "()Lcom/example/mumbaitransit/live/Halt;", "getPreviousHalt", "getRoute", "()Ljava/util/List;", "getSourceCode", "getSourceName", "getStatusText", "getTrackingMode", "getTrainName", "getTrainNumber", "component1", "component10", "component11", "component12", "component13", "component14", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/Long;Lcom/example/mumbaitransit/live/CurrentPosition;Lcom/example/mumbaitransit/live/Halt;Lcom/example/mumbaitransit/live/Halt;Ljava/util/List;)Lcom/example/mumbaitransit/live/LiveTrainStatus;", "equals", "other", "hashCode", "toString", "app_debug"})
public final class LiveTrainStatus {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String trainNumber = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String trainName = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String sourceCode = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String sourceName = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String destCode = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String destName = null;
    
    /**
     * True only when RailRadar has a real position feed, not a schedule replay.
     */
    private final boolean isLive = false;
    
    /**
     * e.g. "real-time", "schedule". Shown verbatim so we never overstate accuracy.
     */
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String trackingMode = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String statusText = null;
    
    /**
     * Epoch millis, or null if the response carried no usable timestamp.
     */
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long lastUpdatedAt = null;
    @org.jetbrains.annotations.Nullable()
    private final com.example.mumbaitransit.live.CurrentPosition current = null;
    @org.jetbrains.annotations.Nullable()
    private final com.example.mumbaitransit.live.Halt previousHalt = null;
    @org.jetbrains.annotations.Nullable()
    private final com.example.mumbaitransit.live.Halt nextHalt = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.mumbaitransit.live.RouteStop> route = null;
    
    public LiveTrainStatus(@org.jetbrains.annotations.NotNull()
    java.lang.String trainNumber, @org.jetbrains.annotations.Nullable()
    java.lang.String trainName, @org.jetbrains.annotations.Nullable()
    java.lang.String sourceCode, @org.jetbrains.annotations.Nullable()
    java.lang.String sourceName, @org.jetbrains.annotations.Nullable()
    java.lang.String destCode, @org.jetbrains.annotations.Nullable()
    java.lang.String destName, boolean isLive, @org.jetbrains.annotations.Nullable()
    java.lang.String trackingMode, @org.jetbrains.annotations.Nullable()
    java.lang.String statusText, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastUpdatedAt, @org.jetbrains.annotations.Nullable()
    com.example.mumbaitransit.live.CurrentPosition current, @org.jetbrains.annotations.Nullable()
    com.example.mumbaitransit.live.Halt previousHalt, @org.jetbrains.annotations.Nullable()
    com.example.mumbaitransit.live.Halt nextHalt, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.mumbaitransit.live.RouteStop> route) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getTrainNumber() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getTrainName() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSourceCode() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSourceName() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDestCode() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDestName() {
        return null;
    }
    
    /**
     * True only when RailRadar has a real position feed, not a schedule replay.
     */
    public final boolean isLive() {
        return false;
    }
    
    /**
     * e.g. "real-time", "schedule". Shown verbatim so we never overstate accuracy.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getTrackingMode() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getStatusText() {
        return null;
    }
    
    /**
     * Epoch millis, or null if the response carried no usable timestamp.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getLastUpdatedAt() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.mumbaitransit.live.CurrentPosition getCurrent() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.mumbaitransit.live.Halt getPreviousHalt() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.mumbaitransit.live.Halt getNextHalt() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.mumbaitransit.live.RouteStop> getRoute() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getDelayMinutes() {
        return null;
    }
    
    public final boolean isActualPosition() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getHeaderTitle() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component10() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.mumbaitransit.live.CurrentPosition component11() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.mumbaitransit.live.Halt component12() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.mumbaitransit.live.Halt component13() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.mumbaitransit.live.RouteStop> component14() {
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
    
    public final boolean component7() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component8() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.live.LiveTrainStatus copy(@org.jetbrains.annotations.NotNull()
    java.lang.String trainNumber, @org.jetbrains.annotations.Nullable()
    java.lang.String trainName, @org.jetbrains.annotations.Nullable()
    java.lang.String sourceCode, @org.jetbrains.annotations.Nullable()
    java.lang.String sourceName, @org.jetbrains.annotations.Nullable()
    java.lang.String destCode, @org.jetbrains.annotations.Nullable()
    java.lang.String destName, boolean isLive, @org.jetbrains.annotations.Nullable()
    java.lang.String trackingMode, @org.jetbrains.annotations.Nullable()
    java.lang.String statusText, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastUpdatedAt, @org.jetbrains.annotations.Nullable()
    com.example.mumbaitransit.live.CurrentPosition current, @org.jetbrains.annotations.Nullable()
    com.example.mumbaitransit.live.Halt previousHalt, @org.jetbrains.annotations.Nullable()
    com.example.mumbaitransit.live.Halt nextHalt, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.mumbaitransit.live.RouteStop> route) {
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