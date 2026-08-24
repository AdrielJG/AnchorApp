package com.example.mumbaitransit.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\b\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001\u001aB\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010\u0007J\u0016\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\t2\u0006\u0010\u0005\u001a\u00020\u0006H\u0002J(\u0010\u000b\u001a\u001a\u0012\u0004\u0012\u00020\r\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\f0\f2\u0006\u0010\u0005\u001a\u00020\u0006H\u0002J\u0016\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\t2\u0006\u0010\u0005\u001a\u00020\u0006H\u0002J\u0016\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\t2\u0006\u0010\u0005\u001a\u00020\u0006H\u0002J*\u0010\u0012\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00140\u00130\t2\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0015\u001a\u00020\u0016H\u0002J \u0010\u0017\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\f0\t2\u0006\u0010\u0005\u001a\u00020\u0006J\u0014\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\r0\t2\u0006\u0010\u0019\u001a\u00020\r\u00a8\u0006\u001b"}, d2 = {"Lcom/example/mumbaitransit/data/DataLoader;", "", "()V", "load", "Lcom/example/mumbaitransit/data/DataLoader$LoadedData;", "context", "Landroid/content/Context;", "(Landroid/content/Context;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "loadEdges", "", "Lcom/example/mumbaitransit/model/GraphEdge;", "loadFareLookup", "", "", "loadMri", "Lcom/example/mumbaitransit/model/MriRow;", "loadNodes", "Lcom/example/mumbaitransit/model/GraphNode;", "loadTariff", "Lkotlin/Pair;", "", "resId", "", "loadTimetable", "parseCsvLine", "line", "LoadedData", "app_debug"})
public final class DataLoader {
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.data.DataLoader INSTANCE = null;
    
    private DataLoader() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object load(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.mumbaitransit.data.DataLoader.LoadedData> $completion) {
        return null;
    }
    
    private final java.util.List<com.example.mumbaitransit.model.GraphNode> loadNodes(android.content.Context context) {
        return null;
    }
    
    private final java.util.List<com.example.mumbaitransit.model.GraphEdge> loadEdges(android.content.Context context) {
        return null;
    }
    
    private final java.util.List<com.example.mumbaitransit.model.MriRow> loadMri(android.content.Context context) {
        return null;
    }
    
    @kotlin.Suppress(names = {"UNCHECKED_CAST"})
    private final java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.String>> loadFareLookup(android.content.Context context) {
        return null;
    }
    
    private final java.util.List<kotlin.Pair<java.lang.Double, java.lang.Double>> loadTariff(android.content.Context context, int resId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.util.Map<java.lang.String, java.lang.String>> loadTimetable(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    /**
     * Minimal CSV line parser handling quoted fields
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> parseCsvLine(@org.jetbrains.annotations.NotNull()
    java.lang.String line) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000F\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0002\b\u0015\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u009d\u0001\u0012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003\u0012\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u0003\u0012\u001e\u0010\t\u001a\u001a\u0012\u0004\u0012\u00020\u000b\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\n0\n\u0012\u0018\u0010\f\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u000e0\r0\u0003\u0012\u0018\u0010\u000f\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u000e0\r0\u0003\u0012\u0018\u0010\u0010\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\n0\u0003\u00a2\u0006\u0002\u0010\u0011J\u000f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\u000f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003H\u00c6\u0003J\u000f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\b0\u0003H\u00c6\u0003J!\u0010\u001e\u001a\u001a\u0012\u0004\u0012\u00020\u000b\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\n0\nH\u00c6\u0003J\u001b\u0010\u001f\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u000e0\r0\u0003H\u00c6\u0003J\u001b\u0010 \u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u000e0\r0\u0003H\u00c6\u0003J\u001b\u0010!\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\n0\u0003H\u00c6\u0003J\u00af\u0001\u0010\"\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u000e\b\u0002\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00060\u00032\u000e\b\u0002\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u00032 \b\u0002\u0010\t\u001a\u001a\u0012\u0004\u0012\u00020\u000b\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\n0\n2\u001a\b\u0002\u0010\f\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u000e0\r0\u00032\u001a\b\u0002\u0010\u000f\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u000e0\r0\u00032\u001a\b\u0002\u0010\u0010\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\n0\u0003H\u00c6\u0001J\u0013\u0010#\u001a\u00020$2\b\u0010%\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010&\u001a\u00020\'H\u00d6\u0001J\t\u0010(\u001a\u00020\u000bH\u00d6\u0001R#\u0010\f\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u000e0\r0\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0013R)\u0010\t\u001a\u001a\u0012\u0004\u0012\u00020\u000b\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\n0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0017\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0013R\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0013R#\u0010\u000f\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u000e0\r0\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0013R#\u0010\u0010\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\n0\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0013\u00a8\u0006)"}, d2 = {"Lcom/example/mumbaitransit/data/DataLoader$LoadedData;", "", "nodes", "", "Lcom/example/mumbaitransit/model/GraphNode;", "edges", "Lcom/example/mumbaitransit/model/GraphEdge;", "mriRows", "Lcom/example/mumbaitransit/model/MriRow;", "fareLookup", "", "", "autoTariff", "Lkotlin/Pair;", "", "taxiTariff", "timetable", "(Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/Map;Ljava/util/List;Ljava/util/List;Ljava/util/List;)V", "getAutoTariff", "()Ljava/util/List;", "getEdges", "getFareLookup", "()Ljava/util/Map;", "getMriRows", "getNodes", "getTaxiTariff", "getTimetable", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class LoadedData {
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<com.example.mumbaitransit.model.GraphNode> nodes = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<com.example.mumbaitransit.model.GraphEdge> edges = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<com.example.mumbaitransit.model.MriRow> mriRows = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.String>> fareLookup = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<kotlin.Pair<java.lang.Double, java.lang.Double>> autoTariff = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<kotlin.Pair<java.lang.Double, java.lang.Double>> taxiTariff = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.util.Map<java.lang.String, java.lang.String>> timetable = null;
        
        public LoadedData(@org.jetbrains.annotations.NotNull()
        java.util.List<com.example.mumbaitransit.model.GraphNode> nodes, @org.jetbrains.annotations.NotNull()
        java.util.List<com.example.mumbaitransit.model.GraphEdge> edges, @org.jetbrains.annotations.NotNull()
        java.util.List<com.example.mumbaitransit.model.MriRow> mriRows, @org.jetbrains.annotations.NotNull()
        java.util.Map<java.lang.String, ? extends java.util.Map<java.lang.String, java.lang.String>> fareLookup, @org.jetbrains.annotations.NotNull()
        java.util.List<kotlin.Pair<java.lang.Double, java.lang.Double>> autoTariff, @org.jetbrains.annotations.NotNull()
        java.util.List<kotlin.Pair<java.lang.Double, java.lang.Double>> taxiTariff, @org.jetbrains.annotations.NotNull()
        java.util.List<? extends java.util.Map<java.lang.String, java.lang.String>> timetable) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.example.mumbaitransit.model.GraphNode> getNodes() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.example.mumbaitransit.model.GraphEdge> getEdges() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.example.mumbaitransit.model.MriRow> getMriRows() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.String>> getFareLookup() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<kotlin.Pair<java.lang.Double, java.lang.Double>> getAutoTariff() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<kotlin.Pair<java.lang.Double, java.lang.Double>> getTaxiTariff() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.util.Map<java.lang.String, java.lang.String>> getTimetable() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.example.mumbaitransit.model.GraphNode> component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.example.mumbaitransit.model.GraphEdge> component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.example.mumbaitransit.model.MriRow> component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.String>> component4() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<kotlin.Pair<java.lang.Double, java.lang.Double>> component5() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<kotlin.Pair<java.lang.Double, java.lang.Double>> component6() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.util.Map<java.lang.String, java.lang.String>> component7() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.data.DataLoader.LoadedData copy(@org.jetbrains.annotations.NotNull()
        java.util.List<com.example.mumbaitransit.model.GraphNode> nodes, @org.jetbrains.annotations.NotNull()
        java.util.List<com.example.mumbaitransit.model.GraphEdge> edges, @org.jetbrains.annotations.NotNull()
        java.util.List<com.example.mumbaitransit.model.MriRow> mriRows, @org.jetbrains.annotations.NotNull()
        java.util.Map<java.lang.String, ? extends java.util.Map<java.lang.String, java.lang.String>> fareLookup, @org.jetbrains.annotations.NotNull()
        java.util.List<kotlin.Pair<java.lang.Double, java.lang.Double>> autoTariff, @org.jetbrains.annotations.NotNull()
        java.util.List<kotlin.Pair<java.lang.Double, java.lang.Double>> taxiTariff, @org.jetbrains.annotations.NotNull()
        java.util.List<? extends java.util.Map<java.lang.String, java.lang.String>> timetable) {
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