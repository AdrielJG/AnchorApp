package com.example.mumbaitransit.live;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u0006\n\u0002\b\u000e\n\u0002\u0010\u000b\n\u0002\b\u001e\b\u0086\b\u0018\u00002\u00020\u0001Bu\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\b\u0010\t\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\n\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\u000b\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\f\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\r\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u000e\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u000f\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\u0010\u001a\u0004\u0018\u00010\u0011\u00a2\u0006\u0002\u0010\u0012J\t\u0010,\u001a\u00020\u0003H\u00c6\u0003J\u0010\u0010-\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0018J\u000b\u0010.\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u0010\u0010/\u001a\u0004\u0018\u00010\u0011H\u00c6\u0003\u00a2\u0006\u0002\u0010\u001dJ\t\u00100\u001a\u00020\u0005H\u00c6\u0003J\t\u00101\u001a\u00020\u0005H\u00c6\u0003J\t\u00102\u001a\u00020\bH\u00c6\u0003J\u000b\u00103\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u000b\u00104\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u000b\u00105\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u000b\u00106\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u0010\u00107\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0018J\u0096\u0001\u00108\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\b2\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u00052\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u00052\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\u00052\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u00052\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u00052\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u0011H\u00c6\u0001\u00a2\u0006\u0002\u00109J\u0013\u0010:\u001a\u00020 2\b\u0010;\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010<\u001a\u00020\u0003H\u00d6\u0001J\t\u0010=\u001a\u00020\u0005H\u00d6\u0001R\u0013\u0010\u000b\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0013\u0010\f\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0014R\u0013\u0010\u0016\u001a\u0004\u0018\u00010\u00038F\u00a2\u0006\u0006\u001a\u0004\b\u0017\u0010\u0018R\u0015\u0010\r\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\n\n\u0002\u0010\u001a\u001a\u0004\b\u0019\u0010\u0018R\u0015\u0010\u000e\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\n\n\u0002\u0010\u001a\u001a\u0004\b\u001b\u0010\u0018R\u0015\u0010\u0010\u001a\u0004\u0018\u00010\u0011\u00a2\u0006\n\n\u0002\u0010\u001e\u001a\u0004\b\u001c\u0010\u001dR\u0011\u0010\u001f\u001a\u00020 8F\u00a2\u0006\u0006\u001a\u0004\b!\u0010\"R\u0013\u0010\u000f\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u0014R\u0013\u0010\t\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u0014R\u0013\u0010\n\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u0014R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\'R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010\u0014R\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010\u0014R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010+\u00a8\u0006>"}, d2 = {"Lcom/example/mumbaitransit/live/RouteStop;", "", "sequence", "", "stationCode", "", "stationName", "status", "Lcom/example/mumbaitransit/live/StopStatus;", "scheduledArrival", "scheduledDeparture", "actualArrival", "actualDeparture", "delayArrival", "delayDeparture", "platform", "distanceKm", "", "(ILjava/lang/String;Ljava/lang/String;Lcom/example/mumbaitransit/live/StopStatus;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/String;Ljava/lang/Double;)V", "getActualArrival", "()Ljava/lang/String;", "getActualDeparture", "delay", "getDelay", "()Ljava/lang/Integer;", "getDelayArrival", "Ljava/lang/Integer;", "getDelayDeparture", "getDistanceKm", "()Ljava/lang/Double;", "Ljava/lang/Double;", "hasActual", "", "getHasActual", "()Z", "getPlatform", "getScheduledArrival", "getScheduledDeparture", "getSequence", "()I", "getStationCode", "getStationName", "getStatus", "()Lcom/example/mumbaitransit/live/StopStatus;", "component1", "component10", "component11", "component12", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(ILjava/lang/String;Ljava/lang/String;Lcom/example/mumbaitransit/live/StopStatus;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/String;Ljava/lang/Double;)Lcom/example/mumbaitransit/live/RouteStop;", "equals", "other", "hashCode", "toString", "app_debug"})
public final class RouteStop {
    private final int sequence = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String stationCode = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String stationName = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.mumbaitransit.live.StopStatus status = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String scheduledArrival = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String scheduledDeparture = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String actualArrival = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String actualDeparture = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Integer delayArrival = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Integer delayDeparture = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String platform = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double distanceKm = null;
    
    public RouteStop(int sequence, @org.jetbrains.annotations.NotNull()
    java.lang.String stationCode, @org.jetbrains.annotations.NotNull()
    java.lang.String stationName, @org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.live.StopStatus status, @org.jetbrains.annotations.Nullable()
    java.lang.String scheduledArrival, @org.jetbrains.annotations.Nullable()
    java.lang.String scheduledDeparture, @org.jetbrains.annotations.Nullable()
    java.lang.String actualArrival, @org.jetbrains.annotations.Nullable()
    java.lang.String actualDeparture, @org.jetbrains.annotations.Nullable()
    java.lang.Integer delayArrival, @org.jetbrains.annotations.Nullable()
    java.lang.Integer delayDeparture, @org.jetbrains.annotations.Nullable()
    java.lang.String platform, @org.jetbrains.annotations.Nullable()
    java.lang.Double distanceKm) {
        super();
    }
    
    public final int getSequence() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getStationCode() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getStationName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.live.StopStatus getStatus() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getScheduledArrival() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getScheduledDeparture() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getActualArrival() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getActualDeparture() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getDelayArrival() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getDelayDeparture() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getPlatform() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getDistanceKm() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getDelay() {
        return null;
    }
    
    public final boolean getHasActual() {
        return false;
    }
    
    public final int component1() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component10() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component11() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component12() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.live.StopStatus component4() {
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
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component8() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.live.RouteStop copy(int sequence, @org.jetbrains.annotations.NotNull()
    java.lang.String stationCode, @org.jetbrains.annotations.NotNull()
    java.lang.String stationName, @org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.live.StopStatus status, @org.jetbrains.annotations.Nullable()
    java.lang.String scheduledArrival, @org.jetbrains.annotations.Nullable()
    java.lang.String scheduledDeparture, @org.jetbrains.annotations.Nullable()
    java.lang.String actualArrival, @org.jetbrains.annotations.Nullable()
    java.lang.String actualDeparture, @org.jetbrains.annotations.Nullable()
    java.lang.Integer delayArrival, @org.jetbrains.annotations.Nullable()
    java.lang.Integer delayDeparture, @org.jetbrains.annotations.Nullable()
    java.lang.String platform, @org.jetbrains.annotations.Nullable()
    java.lang.Double distanceKm) {
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