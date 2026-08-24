package com.example.mumbaitransit.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010 \n\u0002\b*\b\u0086\b\u0018\u00002\u00020\u0001Bu\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0003\u0012\u0006\u0010\u0007\u001a\u00020\u0003\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\t\u0012\u0006\u0010\r\u001a\u00020\t\u0012\u0006\u0010\u000e\u001a\u00020\u0003\u0012\u0006\u0010\u000f\u001a\u00020\u0010\u0012\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00030\u0012\u0012\b\b\u0002\u0010\u0013\u001a\u00020\u0010\u00a2\u0006\u0002\u0010\u0014J\t\u0010*\u001a\u00020\u0003H\u00c6\u0003J\t\u0010+\u001a\u00020\u0003H\u00c6\u0003J\t\u0010,\u001a\u00020\u0010H\u00c6\u0003J\u000f\u0010-\u001a\b\u0012\u0004\u0012\u00020\u00030\u0012H\u00c6\u0003J\t\u0010.\u001a\u00020\u0010H\u00c6\u0003J\t\u0010/\u001a\u00020\u0003H\u00c6\u0003J\t\u00100\u001a\u00020\u0003H\u00c6\u0003J\t\u00101\u001a\u00020\u0003H\u00c6\u0003J\t\u00102\u001a\u00020\u0003H\u00c6\u0003J\t\u00103\u001a\u00020\tH\u00c6\u0003J\t\u00104\u001a\u00020\u000bH\u00c6\u0003J\t\u00105\u001a\u00020\tH\u00c6\u0003J\t\u00106\u001a\u00020\tH\u00c6\u0003J\u0091\u0001\u00107\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00032\b\b\u0002\u0010\u0007\u001a\u00020\u00032\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\u000b2\b\b\u0002\u0010\f\u001a\u00020\t2\b\b\u0002\u0010\r\u001a\u00020\t2\b\b\u0002\u0010\u000e\u001a\u00020\u00032\b\b\u0002\u0010\u000f\u001a\u00020\u00102\u000e\b\u0002\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00030\u00122\b\b\u0002\u0010\u0013\u001a\u00020\u0010H\u00c6\u0001J\u0013\u00108\u001a\u00020\u00102\b\u00109\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010:\u001a\u00020\u000bH\u00d6\u0001J\t\u0010;\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u000e\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0016R\u001a\u0010\n\u001a\u00020\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0018\u0010\u0019\"\u0004\b\u001a\u0010\u001bR\u001a\u0010\u0013\u001a\u00020\u0010X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001c\u0010\u001d\"\u0004\b\u001e\u0010\u001fR\u0011\u0010\r\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010!R\u0011\u0010\u0006\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u0016R\u0011\u0010\u000f\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u001dR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u0016R\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u0016R\u0011\u0010\f\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010!R\u0017\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00030\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\'R\u0011\u0010\u0007\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010\u0016R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010!\u00a8\u0006<"}, d2 = {"Lcom/example/mumbaitransit/model/PathEdge;", "", "edgeType", "", "line", "mode", "fromStop", "toStop", "travelMin", "", "fareInr", "", "reliability", "freq", "direction", "isFast", "", "skippedStops", "", "fareReal", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;DIDDLjava/lang/String;ZLjava/util/List;Z)V", "getDirection", "()Ljava/lang/String;", "getEdgeType", "getFareInr", "()I", "setFareInr", "(I)V", "getFareReal", "()Z", "setFareReal", "(Z)V", "getFreq", "()D", "getFromStop", "getLine", "getMode", "getReliability", "getSkippedStops", "()Ljava/util/List;", "getToStop", "getTravelMin", "component1", "component10", "component11", "component12", "component13", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class PathEdge {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String edgeType = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String line = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String mode = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String fromStop = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String toStop = null;
    private final double travelMin = 0.0;
    private int fareInr;
    private final double reliability = 0.0;
    private final double freq = 0.0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String direction = null;
    private final boolean isFast = false;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> skippedStops = null;
    private boolean fareReal;
    
    public PathEdge(@org.jetbrains.annotations.NotNull()
    java.lang.String edgeType, @org.jetbrains.annotations.NotNull()
    java.lang.String line, @org.jetbrains.annotations.NotNull()
    java.lang.String mode, @org.jetbrains.annotations.NotNull()
    java.lang.String fromStop, @org.jetbrains.annotations.NotNull()
    java.lang.String toStop, double travelMin, int fareInr, double reliability, double freq, @org.jetbrains.annotations.NotNull()
    java.lang.String direction, boolean isFast, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> skippedStops, boolean fareReal) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getEdgeType() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLine() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getMode() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFromStop() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getToStop() {
        return null;
    }
    
    public final double getTravelMin() {
        return 0.0;
    }
    
    public final int getFareInr() {
        return 0;
    }
    
    public final void setFareInr(int p0) {
    }
    
    public final double getReliability() {
        return 0.0;
    }
    
    public final double getFreq() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDirection() {
        return null;
    }
    
    public final boolean isFast() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getSkippedStops() {
        return null;
    }
    
    public final boolean getFareReal() {
        return false;
    }
    
    public final void setFareReal(boolean p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component10() {
        return null;
    }
    
    public final boolean component11() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> component12() {
        return null;
    }
    
    public final boolean component13() {
        return false;
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
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component5() {
        return null;
    }
    
    public final double component6() {
        return 0.0;
    }
    
    public final int component7() {
        return 0;
    }
    
    public final double component8() {
        return 0.0;
    }
    
    public final double component9() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.model.PathEdge copy(@org.jetbrains.annotations.NotNull()
    java.lang.String edgeType, @org.jetbrains.annotations.NotNull()
    java.lang.String line, @org.jetbrains.annotations.NotNull()
    java.lang.String mode, @org.jetbrains.annotations.NotNull()
    java.lang.String fromStop, @org.jetbrains.annotations.NotNull()
    java.lang.String toStop, double travelMin, int fareInr, double reliability, double freq, @org.jetbrains.annotations.NotNull()
    java.lang.String direction, boolean isFast, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> skippedStops, boolean fareReal) {
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