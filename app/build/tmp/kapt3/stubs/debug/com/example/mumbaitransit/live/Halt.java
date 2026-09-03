package com.example.mumbaitransit.live;

/**
 * A previous / next halt reference, which the API may send as an object or a bare code.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0010\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B-\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u0012\b\u0010\u0007\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\bJ\u000b\u0010\u0010\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\u0011\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u0010\u0010\u0012\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003\u00a2\u0006\u0002\u0010\nJ\u000b\u0010\u0013\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J>\u0010\u0014\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00062\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u0003H\u00c6\u0001\u00a2\u0006\u0002\u0010\u0015J\u0013\u0010\u0016\u001a\u00020\u00172\b\u0010\u0018\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0019\u001a\u00020\u001aH\u00d6\u0001J\t\u0010\u001b\u001a\u00020\u0003H\u00d6\u0001R\u0015\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\n\n\u0002\u0010\u000b\u001a\u0004\b\t\u0010\nR\u0013\u0010\u0007\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\r\u00a8\u0006\u001c"}, d2 = {"Lcom/example/mumbaitransit/live/Halt;", "", "stationCode", "", "stationName", "distanceKm", "", "scheduledTime", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/String;)V", "getDistanceKm", "()Ljava/lang/Double;", "Ljava/lang/Double;", "getScheduledTime", "()Ljava/lang/String;", "getStationCode", "getStationName", "component1", "component2", "component3", "component4", "copy", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/String;)Lcom/example/mumbaitransit/live/Halt;", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
public final class Halt {
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String stationCode = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String stationName = null;
    
    /**
     * Distance to this halt from the train's current position, in km.
     */
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double distanceKm = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String scheduledTime = null;
    
    public Halt(@org.jetbrains.annotations.Nullable()
    java.lang.String stationCode, @org.jetbrains.annotations.Nullable()
    java.lang.String stationName, @org.jetbrains.annotations.Nullable()
    java.lang.Double distanceKm, @org.jetbrains.annotations.Nullable()
    java.lang.String scheduledTime) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getStationCode() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getStationName() {
        return null;
    }
    
    /**
     * Distance to this halt from the train's current position, in km.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getDistanceKm() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getScheduledTime() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component3() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.live.Halt copy(@org.jetbrains.annotations.Nullable()
    java.lang.String stationCode, @org.jetbrains.annotations.Nullable()
    java.lang.String stationName, @org.jetbrains.annotations.Nullable()
    java.lang.Double distanceKm, @org.jetbrains.annotations.Nullable()
    java.lang.String scheduledTime) {
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