package com.example.mumbaitransit.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u001d\b\u0086\b\u0018\u00002\u00020\u0001BK\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\u0003\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u000b\u001a\u00020\u0003\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\rJ\t\u0010\u0019\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001a\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u001c\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001d\u001a\u00020\tH\u00c6\u0003J\t\u0010\u001e\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u001f\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010 \u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J[\u0010!\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u00032\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\u00062\b\b\u0002\u0010\u000b\u001a\u00020\u00032\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u0003H\u00c6\u0001J\u0013\u0010\"\u001a\u00020\t2\b\u0010#\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010$\u001a\u00020\u0006H\u00d6\u0001J\t\u0010%\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0011R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\u000b\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0011R\u0013\u0010\f\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0011R\u0011\u0010\u0007\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0011R\u0011\u0010\n\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0014\u00a8\u0006&"}, d2 = {"Lcom/example/mumbaitransit/model/TrainTiming;", "", "depFrom", "", "arrTo", "journeyMin", "", "trainType", "ac", "", "waitMin", "terminus", "trainNo", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;ZILjava/lang/String;Ljava/lang/String;)V", "getAc", "()Z", "getArrTo", "()Ljava/lang/String;", "getDepFrom", "getJourneyMin", "()I", "getTerminus", "getTrainNo", "getTrainType", "getWaitMin", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class TrainTiming {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String depFrom = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String arrTo = null;
    private final int journeyMin = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String trainType = null;
    private final boolean ac = false;
    private final int waitMin = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String terminus = null;
    
    /**
     * Indian Railways train number, e.g. "91045". The bundled timetable has no
     * such column, so this is null today; LiveStatusActivity asks the user once
     * and remembers it. Populate here if the CSV ever gains train numbers and
     * live tracking will open with no prompt.
     */
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String trainNo = null;
    
    public TrainTiming(@org.jetbrains.annotations.NotNull()
    java.lang.String depFrom, @org.jetbrains.annotations.NotNull()
    java.lang.String arrTo, int journeyMin, @org.jetbrains.annotations.NotNull()
    java.lang.String trainType, boolean ac, int waitMin, @org.jetbrains.annotations.NotNull()
    java.lang.String terminus, @org.jetbrains.annotations.Nullable()
    java.lang.String trainNo) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDepFrom() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getArrTo() {
        return null;
    }
    
    public final int getJourneyMin() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getTrainType() {
        return null;
    }
    
    public final boolean getAc() {
        return false;
    }
    
    public final int getWaitMin() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getTerminus() {
        return null;
    }
    
    /**
     * Indian Railways train number, e.g. "91045". The bundled timetable has no
     * such column, so this is null today; LiveStatusActivity asks the user once
     * and remembers it. Populate here if the CSV ever gains train numbers and
     * live tracking will open with no prompt.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getTrainNo() {
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
    
    public final int component3() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    public final boolean component5() {
        return false;
    }
    
    public final int component6() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component8() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.model.TrainTiming copy(@org.jetbrains.annotations.NotNull()
    java.lang.String depFrom, @org.jetbrains.annotations.NotNull()
    java.lang.String arrTo, int journeyMin, @org.jetbrains.annotations.NotNull()
    java.lang.String trainType, boolean ac, int waitMin, @org.jetbrains.annotations.NotNull()
    java.lang.String terminus, @org.jetbrains.annotations.Nullable()
    java.lang.String trainNo) {
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