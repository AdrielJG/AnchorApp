package com.example.mumbaitransit.auth;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\b\n\u0002\b%\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0087\b\u0018\u00002\u00020\u0001Bq\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u0012\u0006\u0010\b\u001a\u00020\u0006\u0012\u0006\u0010\t\u001a\u00020\u0006\u0012\u0006\u0010\n\u001a\u00020\u0006\u0012\u0006\u0010\u000b\u001a\u00020\f\u0012\u0006\u0010\r\u001a\u00020\u000e\u0012\u0006\u0010\u000f\u001a\u00020\u000e\u0012\u0006\u0010\u0010\u001a\u00020\u0006\u0012\u0006\u0010\u0011\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u0012\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0013J\t\u0010%\u001a\u00020\u0003H\u00c6\u0003J\t\u0010&\u001a\u00020\u000eH\u00c6\u0003J\t\u0010\'\u001a\u00020\u0006H\u00c6\u0003J\t\u0010(\u001a\u00020\u0006H\u00c6\u0003J\t\u0010)\u001a\u00020\u0003H\u00c6\u0003J\t\u0010*\u001a\u00020\u0003H\u00c6\u0003J\t\u0010+\u001a\u00020\u0006H\u00c6\u0003J\t\u0010,\u001a\u00020\u0006H\u00c6\u0003J\t\u0010-\u001a\u00020\u0006H\u00c6\u0003J\t\u0010.\u001a\u00020\u0006H\u00c6\u0003J\t\u0010/\u001a\u00020\u0006H\u00c6\u0003J\t\u00100\u001a\u00020\fH\u00c6\u0003J\t\u00101\u001a\u00020\u000eH\u00c6\u0003J\u008b\u0001\u00102\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u00062\b\b\u0002\u0010\b\u001a\u00020\u00062\b\b\u0002\u0010\t\u001a\u00020\u00062\b\b\u0002\u0010\n\u001a\u00020\u00062\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\u000e2\b\b\u0002\u0010\u000f\u001a\u00020\u000e2\b\b\u0002\u0010\u0010\u001a\u00020\u00062\b\b\u0002\u0010\u0011\u001a\u00020\u00062\b\b\u0002\u0010\u0012\u001a\u00020\u0003H\u00c6\u0001J\u0013\u00103\u001a\u0002042\b\u00105\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00106\u001a\u00020\u000eH\u00d6\u0001J\t\u00107\u001a\u00020\u0006H\u00d6\u0001R\u0016\u0010\u0007\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0016\u0010\u0010\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0015R\u0016\u0010\n\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0015R\u0016\u0010\u0005\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0015R\u0016\u0010\u0011\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0015R\u0016\u0010\u0012\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0017R\u0016\u0010\b\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0015R\u0016\u0010\t\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0015R\u0016\u0010\r\u001a\u00020\u000e8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u0016\u0010\u000b\u001a\u00020\f8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\"R\u0016\u0010\u000f\u001a\u00020\u000e8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010 R\u0016\u0010\u0004\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u0017\u00a8\u00068"}, d2 = {"Lcom/example/mumbaitransit/auth/SavedRouteEntity;", "", "id", "", "userId", "originLabel", "", "destLabel", "scenario", "scenarioLabel", "modeStr", "totalMin", "", "totalFare", "", "transfers", "linesUsed", "routeType", "savedAt", "(JJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;DIILjava/lang/String;Ljava/lang/String;J)V", "getDestLabel", "()Ljava/lang/String;", "getId", "()J", "getLinesUsed", "getModeStr", "getOriginLabel", "getRouteType", "getSavedAt", "getScenario", "getScenarioLabel", "getTotalFare", "()I", "getTotalMin", "()D", "getTransfers", "getUserId", "component1", "component10", "component11", "component12", "component13", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
@androidx.room.Entity(tableName = "saved_routes")
public final class SavedRouteEntity {
    @androidx.room.PrimaryKey(autoGenerate = true)
    private final long id = 0L;
    @androidx.room.ColumnInfo(name = "user_id")
    private final long userId = 0L;
    @androidx.room.ColumnInfo(name = "origin_label")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String originLabel = null;
    @androidx.room.ColumnInfo(name = "dest_label")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String destLabel = null;
    @androidx.room.ColumnInfo(name = "scenario")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String scenario = null;
    @androidx.room.ColumnInfo(name = "scenario_label")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String scenarioLabel = null;
    @androidx.room.ColumnInfo(name = "mode_str")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String modeStr = null;
    @androidx.room.ColumnInfo(name = "total_min")
    private final double totalMin = 0.0;
    @androidx.room.ColumnInfo(name = "total_fare")
    private final int totalFare = 0;
    @androidx.room.ColumnInfo(name = "transfers")
    private final int transfers = 0;
    @androidx.room.ColumnInfo(name = "lines_used")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String linesUsed = null;
    @androidx.room.ColumnInfo(name = "route_type")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String routeType = null;
    @androidx.room.ColumnInfo(name = "saved_at")
    private final long savedAt = 0L;
    
    public SavedRouteEntity(long id, long userId, @org.jetbrains.annotations.NotNull()
    java.lang.String originLabel, @org.jetbrains.annotations.NotNull()
    java.lang.String destLabel, @org.jetbrains.annotations.NotNull()
    java.lang.String scenario, @org.jetbrains.annotations.NotNull()
    java.lang.String scenarioLabel, @org.jetbrains.annotations.NotNull()
    java.lang.String modeStr, double totalMin, int totalFare, int transfers, @org.jetbrains.annotations.NotNull()
    java.lang.String linesUsed, @org.jetbrains.annotations.NotNull()
    java.lang.String routeType, long savedAt) {
        super();
    }
    
    public final long getId() {
        return 0L;
    }
    
    public final long getUserId() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getOriginLabel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDestLabel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getScenario() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getScenarioLabel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getModeStr() {
        return null;
    }
    
    public final double getTotalMin() {
        return 0.0;
    }
    
    public final int getTotalFare() {
        return 0;
    }
    
    public final int getTransfers() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLinesUsed() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getRouteType() {
        return null;
    }
    
    public final long getSavedAt() {
        return 0L;
    }
    
    public final long component1() {
        return 0L;
    }
    
    public final int component10() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component11() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component12() {
        return null;
    }
    
    public final long component13() {
        return 0L;
    }
    
    public final long component2() {
        return 0L;
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
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component6() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component7() {
        return null;
    }
    
    public final double component8() {
        return 0.0;
    }
    
    public final int component9() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.auth.SavedRouteEntity copy(long id, long userId, @org.jetbrains.annotations.NotNull()
    java.lang.String originLabel, @org.jetbrains.annotations.NotNull()
    java.lang.String destLabel, @org.jetbrains.annotations.NotNull()
    java.lang.String scenario, @org.jetbrains.annotations.NotNull()
    java.lang.String scenarioLabel, @org.jetbrains.annotations.NotNull()
    java.lang.String modeStr, double totalMin, int totalFare, int transfers, @org.jetbrains.annotations.NotNull()
    java.lang.String linesUsed, @org.jetbrains.annotations.NotNull()
    java.lang.String routeType, long savedAt) {
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