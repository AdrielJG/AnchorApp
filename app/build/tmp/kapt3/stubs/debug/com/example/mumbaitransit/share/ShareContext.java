package com.example.mumbaitransit.share;

/**
 * Everything the share card needs that isn't already on the [RouteCard].
 *
 * Timetable legs and auto fares are produced by the ViewModel, so they're
 * gathered once by the caller and handed over rather than looked up here.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001B5\u0012\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\b\b\u0002\u0010\t\u001a\u00020\b\u00a2\u0006\u0002\u0010\nJ\u000f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\u000b\u0010\u0013\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\bH\u00c6\u0003J\t\u0010\u0015\u001a\u00020\bH\u00c6\u0003J9\u0010\u0016\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\bH\u00c6\u0001J\u0013\u0010\u0017\u001a\u00020\u00182\b\u0010\u0019\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001a\u001a\u00020\bH\u00d6\u0001J\t\u0010\u001b\u001a\u00020\u0006H\u00d6\u0001R\u0011\u0010\t\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\fR\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011\u00a8\u0006\u001c"}, d2 = {"Lcom/example/mumbaitransit/share/ShareContext;", "", "legs", "", "Lcom/example/mumbaitransit/model/TimetableLeg;", "departWindow", "", "autoFareTo", "", "autoFareFrom", "(Ljava/util/List;Ljava/lang/String;II)V", "getAutoFareFrom", "()I", "getAutoFareTo", "getDepartWindow", "()Ljava/lang/String;", "getLegs", "()Ljava/util/List;", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
public final class ShareContext {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.mumbaitransit.model.TimetableLeg> legs = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String departWindow = null;
    private final int autoFareTo = 0;
    private final int autoFareFrom = 0;
    
    public ShareContext(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.mumbaitransit.model.TimetableLeg> legs, @org.jetbrains.annotations.Nullable()
    java.lang.String departWindow, int autoFareTo, int autoFareFrom) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.mumbaitransit.model.TimetableLeg> getLegs() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDepartWindow() {
        return null;
    }
    
    public final int getAutoFareTo() {
        return 0;
    }
    
    public final int getAutoFareFrom() {
        return 0;
    }
    
    public ShareContext() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.mumbaitransit.model.TimetableLeg> component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component2() {
        return null;
    }
    
    public final int component3() {
        return 0;
    }
    
    public final int component4() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.share.ShareContext copy(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.mumbaitransit.model.TimetableLeg> legs, @org.jetbrains.annotations.Nullable()
    java.lang.String departWindow, int autoFareTo, int autoFareFrom) {
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