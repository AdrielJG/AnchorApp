package com.example.mumbaitransit.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0018\b\u0086\b\u0018\u00002\u00020\u0001BO\u0012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\b\u0012\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0003\u0012\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0003\u0012\u0006\u0010\r\u001a\u00020\u000e\u00a2\u0006\u0002\u0010\u000fJ\u000f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u001c\u001a\u00020\bH\u00c6\u0003J\t\u0010\u001d\u001a\u00020\bH\u00c6\u0003J\u000f\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0003H\u00c6\u0003J\u000f\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0003H\u00c6\u0003J\t\u0010 \u001a\u00020\u000eH\u00c6\u0003Ja\u0010!\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\b2\u000e\b\u0002\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\u00032\u000e\b\u0002\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000b0\u00032\b\b\u0002\u0010\r\u001a\u00020\u000eH\u00c6\u0001J\u0013\u0010\"\u001a\u00020\u000e2\b\u0010#\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010$\u001a\u00020\bH\u00d6\u0001J\t\u0010%\u001a\u00020\u000bH\u00d6\u0001R\u0011\u0010\r\u001a\u00020\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u0010R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0017\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0012R\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0012R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0011\u0010\t\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0016\u00a8\u0006&"}, d2 = {"Lcom/example/mumbaitransit/model/RouteResult;", "", "path", "", "Lcom/example/mumbaitransit/model/PathEdge;", "totalMin", "", "totalFare", "", "transfers", "linesUsed", "", "modesUsed", "isMultimodal", "", "(Ljava/util/List;DIILjava/util/List;Ljava/util/List;Z)V", "()Z", "getLinesUsed", "()Ljava/util/List;", "getModesUsed", "getPath", "getTotalFare", "()I", "getTotalMin", "()D", "getTransfers", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class RouteResult {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.mumbaitransit.model.PathEdge> path = null;
    private final double totalMin = 0.0;
    private final int totalFare = 0;
    private final int transfers = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> linesUsed = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> modesUsed = null;
    private final boolean isMultimodal = false;
    
    public RouteResult(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.mumbaitransit.model.PathEdge> path, double totalMin, int totalFare, int transfers, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> linesUsed, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> modesUsed, boolean isMultimodal) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.mumbaitransit.model.PathEdge> getPath() {
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
    public final java.util.List<java.lang.String> getLinesUsed() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getModesUsed() {
        return null;
    }
    
    public final boolean isMultimodal() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.mumbaitransit.model.PathEdge> component1() {
        return null;
    }
    
    public final double component2() {
        return 0.0;
    }
    
    public final int component3() {
        return 0;
    }
    
    public final int component4() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> component6() {
        return null;
    }
    
    public final boolean component7() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.model.RouteResult copy(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.mumbaitransit.model.PathEdge> path, double totalMin, int totalFare, int transfers, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> linesUsed, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> modesUsed, boolean isMultimodal) {
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