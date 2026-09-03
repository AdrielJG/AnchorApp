package com.example.mumbaitransit.engine;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0098\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u0006\n\u0002\b\u0010\n\u0002\u0010%\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0013\n\u0002\b\u0002\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010#\n\u0002\b\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b0\u0018\u0000 \u008c\u00012\u00020\u0001:\n\u008b\u0001\u008c\u0001\u008d\u0001\u008e\u0001\u008f\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010E\u001a\u00020\b2\u0006\u0010F\u001a\u00020\rJt\u0010G\u001a\b\u0012\u0004\u0012\u00020H0+2\f\u0010I\u001a\b\u0012\u0004\u0012\u00020J0+2\f\u0010K\u001a\b\u0012\u0004\u0012\u00020J0+2\u0006\u0010L\u001a\u00020\r2\u0006\u0010M\u001a\u00020\r2\u0006\u0010N\u001a\u00020\r2\u0006\u0010O\u001a\u00020\r2\n\b\u0002\u0010P\u001a\u0004\u0018\u00010\u00072\n\b\u0002\u0010Q\u001a\u0004\u0018\u00010\u00072\b\b\u0002\u0010R\u001a\u00020\u00072\b\b\u0002\u0010S\u001a\u00020\u0007J\u0010\u0010T\u001a\u00020U2\u0006\u0010\u0002\u001a\u00020\u0003H\u0002J<\u0010V\u001a\u0004\u0018\u00010W2\f\u0010X\u001a\b\u0012\u0004\u0012\u00020\u00070+2\f\u0010Y\u001a\b\u0012\u0004\u0012\u00020\u00070+2\u0006\u0010Z\u001a\u00020\r2\u0006\u0010[\u001a\u00020\r2\u0006\u0010\\\u001a\u00020\rJ\u001c\u0010]\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u00062\u0006\u0010^\u001a\u00020\u0007H\u0002J@\u0010_\u001a\b\u0012\u0004\u0012\u00020`0+2\u0006\u0010^\u001a\u00020\u00072\u0006\u0010a\u001a\u00020\u00072\u0006\u0010b\u001a\u00020\u00072\u0006\u0010c\u001a\u00020\b2\b\b\u0002\u0010d\u001a\u00020\b2\b\b\u0002\u0010e\u001a\u00020\bJ\u001c\u0010f\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u00062\u0006\u0010^\u001a\u00020\u0007H\u0002J&\u0010g\u001a\u00020\r2\u0006\u0010h\u001a\u00020\r2\u0006\u0010i\u001a\u00020\r2\u0006\u0010j\u001a\u00020\r2\u0006\u0010k\u001a\u00020\rJ \u0010l\u001a\u00020\r2\u0006\u0010m\u001a\u00020\r2\u0006\u0010n\u001a\u00020\"2\u0006\u0010o\u001a\u00020\"H\u0002J\u001f\u0010p\u001a\u0004\u0018\u00010\b2\u0006\u0010q\u001a\u00020\u00072\u0006\u0010r\u001a\u00020\u0007H\u0002\u00a2\u0006\u0002\u0010sJ\u000e\u0010t\u001a\u00020\u00072\u0006\u0010u\u001a\u00020\bJ\u0010\u0010v\u001a\u00020\u00072\u0006\u0010w\u001a\u00020\u0007H\u0002J&\u0010x\u001a\b\u0012\u0004\u0012\u00020J0+2\u0006\u0010y\u001a\u00020\r2\u0006\u0010z\u001a\u00020\r2\b\b\u0002\u0010{\u001a\u00020\bJ(\u0010|\u001a\u0014\u0012\u0004\u0012\u00020\u0007\u0012\n\u0012\b\u0012\u0004\u0012\u00020J0+0\u00062\u0006\u0010y\u001a\u00020\r2\u0006\u0010z\u001a\u00020\rJB\u0010}\u001a\b\u0012\u0004\u0012\u00020`0+2\u0006\u0010^\u001a\u00020\u00072\u0006\u0010a\u001a\u00020\u00072\u0006\u0010b\u001a\u00020\u00072\u0006\u0010c\u001a\u00020\b2\b\b\u0002\u0010d\u001a\u00020\b2\b\b\u0002\u0010e\u001a\u00020\bH\u0002J>\u0010~\u001a\b\u0012\u0004\u0012\u00020`0+2\u0006\u0010^\u001a\u00020\u00072\u0006\u0010a\u001a\u00020\u00072\u0006\u0010b\u001a\u00020\u00072\u0006\u0010c\u001a\u00020\b2\u0006\u0010d\u001a\u00020\b2\u0006\u0010e\u001a\u00020\bH\u0002J\u0011\u0010\u007f\u001a\u00020\u00072\u0007\u0010\u0080\u0001\u001a\u00020\u0007H\u0002J\u0018\u0010\u0081\u0001\u001a\u00020U2\r\u0010\u0082\u0001\u001a\b\u0012\u0004\u0012\u00020\u00070+H\u0002J\u001c\u0010\u0083\u0001\u001a\u0004\u0018\u00010\u00072\u0006\u0010^\u001a\u00020\u00072\u0007\u0010\u0084\u0001\u001a\u00020\u0007H\u0002J*\u0010\u0085\u0001\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u00060+2\u0006\u0010^\u001a\u00020\u00072\u0007\u0010\u0086\u0001\u001a\u00020\u0007J\u001e\u0010\u0087\u0001\u001a\b\u0012\u0004\u0012\u00020\u00070+2\u0006\u0010^\u001a\u00020\u00072\u0007\u0010\u0086\u0001\u001a\u00020\u0007J\u000f\u0010\u0088\u0001\u001a\u00020\b2\u0006\u0010F\u001a\u00020\rJ*\u0010\u0089\u0001\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u00062\u0013\u0010\u008a\u0001\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006H\u0002R\u001a\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000b\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082D\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0013\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0015\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0016\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0018\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0019\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u001a\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u001b\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u001c\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\u001d\u001a\u0014\u0012\u0004\u0012\u00020\u0007\u0012\n\u0012\b\u0012\u0004\u0012\u00020 0\u001f0\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\"X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010#\u001a\u00020\"X\u0082\u0004\u00a2\u0006\u0002\n\u0000R&\u0010$\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070&0%X\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\'\u001a\u0014\u0012\u0004\u0012\u00020\u0007\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u001f0\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010(\u001a\u0014\u0012\u0004\u0012\u00020\u0007\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u001f0\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010)\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010*\u001a\b\u0012\u0004\u0012\u00020\u00070+8F\u00a2\u0006\u0006\u001a\u0004\b,\u0010-R\u0014\u0010.\u001a\b\u0012\u0004\u0012\u00020\u00070%X\u0082\u0004\u00a2\u0006\u0002\n\u0000R&\u0010/\u001a\u001a\u0012\u0004\u0012\u00020\u0007\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u00060\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u00100\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u0002010\u001e\u00a2\u0006\b\n\u0000\u001a\u0004\b2\u00103R\u001a\u00104\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u0002050\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u00106\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b7\u00103R,\u00108\u001a \u0012\u0004\u0012\u00020\u0007\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0&0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u00109\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010:\u001a\b\u0012\u0004\u0012\u00020;0\u001fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R&\u0010<\u001a\u001a\u0012\u0004\u0012\u00020\u0007\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u00060\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R&\u0010=\u001a\u001a\u0012\u0004\u0012\u00020\u0007\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u001e0\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010>\u001a\b\u0012\u0004\u0012\u00020\u00070?X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010@\u001a\u00020\"X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010A\u001a\u00020\"X\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010B\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u00060+X\u0082\u0004\u00a2\u0006\u0002\n\u0000R#\u0010C\u001a\u0014\u0012\u0004\u0012\u00020\u0007\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u001f0\u001e\u00a2\u0006\b\n\u0000\u001a\u0004\bD\u00103\u00a8\u0006\u0090\u0001"}, d2 = {"Lcom/example/mumbaitransit/engine/TransitEngine;", "", "data", "Lcom/example/mumbaitransit/data/DataLoader$LoadedData;", "(Lcom/example/mumbaitransit/data/DataLoader$LoadedData;)V", "CR_AC_FAST", "", "", "", "CR_FAST", "CR_REV", "CR_SLOW", "DIRECTION_CHANGE_PENALTY", "", "HL_DOWN", "HL_REV", "ML1_DOWN", "ML1_REV", "ML2A_DOWN", "ML2A_REV", "ML3_DOWN", "ML3_REV", "ML7_DOWN", "ML7_REV", "TH_DOWN", "TH_REV", "WR_FAST", "WR_REV", "WR_SLOW", "adj", "", "", "Lcom/example/mumbaitransit/engine/TransitEngine$AdjEdge;", "autoFares", "", "autoKm", "badEdges", "", "Lkotlin/Triple;", "canonicalLines", "canonicalMap", "canonicalMode", "canonicals", "", "getCanonicals", "()Ljava/util/List;", "downIsNorth", "fareLookup", "mriLookup", "Lcom/example/mumbaitransit/model/MriScore;", "getMriLookup", "()Ljava/util/Map;", "nodeInfo", "Lcom/example/mumbaitransit/model/GraphNode;", "scenarioLabels", "getScenarioLabels", "scenarioWeights", "stationAliases", "stationPoints", "Lcom/example/mumbaitransit/engine/TransitEngine$StationPoint;", "stopIndex", "stopNamesByLine", "stopTimeLines", "", "taxiFares", "taxiKm", "timetable", "twinMap", "getTwinMap", "autoFare", "km", "buildAllRoutes", "Lcom/example/mumbaitransit/model/RouteCard;", "origStations", "Lcom/example/mumbaitransit/model/NearestStation;", "destStations", "oLat", "oLon", "dLat", "dLon", "pinnedOrig", "pinnedDest", "origLabel", "destLabel", "buildStopIndex", "", "dijkstra", "Lcom/example/mumbaitransit/model/RouteResult;", "startNids", "endNids", "alpha", "beta", "gamma", "getDownOffsets", "line", "getNextTrains", "Lcom/example/mumbaitransit/model/TrainTiming;", "fromStop", "toStop", "arriveAtMins", "n", "windowMin", "getRevOffsets", "haversineKm", "lat1", "lon1", "lat2", "lon2", "interp", "x", "xs", "ys", "lookupFare", "from", "to", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Integer;", "minsToHhMm", "m", "modeLabel", "mode", "nearestStations", "lat", "lon", "k", "nearestStationsGrouped", "nextTrainsFromOffsets", "nextTrainsFromStopTimes", "normalizeStop", "name", "registerTwins", "group", "resolveStop", "stop", "servicesFor", "direction", "stationsOn", "taxiFare", "unwrapMidnight", "stops", "AdjEdge", "Companion", "HeapEntry", "State", "StationPoint", "app_debug"})
public final class TransitEngine {
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.util.List<com.example.mumbaitransit.engine.TransitEngine.AdjEdge>> adj = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, com.example.mumbaitransit.model.GraphNode> nodeInfo = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.util.List<java.lang.String>> canonicalMap = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, com.example.mumbaitransit.model.MriScore> mriLookup = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.mumbaitransit.engine.TransitEngine.StationPoint> stationPoints = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.util.List<java.lang.String>> canonicalLines = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.String> canonicalMode = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.util.List<java.lang.String>> twinMap = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.String>> fareLookup = null;
    @org.jetbrains.annotations.NotNull()
    private final double[] autoKm = null;
    @org.jetbrains.annotations.NotNull()
    private final double[] autoFares = null;
    @org.jetbrains.annotations.NotNull()
    private final double[] taxiKm = null;
    @org.jetbrains.annotations.NotNull()
    private final double[] taxiFares = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.util.Map<java.lang.String, java.lang.String>> timetable = null;
    
    /**
     * service_id -> (station -> minutes past the service's own start), with
     * midnight crossings unwrapped so times increase along the run.
     *
     * Built once from the stop-times matrix. A station absent from a service's
     * map means that service does not call there, which is how fast trains are
     * filtered out of a stop pair without any special-casing.
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>> stopIndex = null;
    
    /**
     * Lines that have real stop times. Everything else uses the offset tables.
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> stopTimeLines = null;
    
    /**
     * line -> (lowercased station -> the exact key used in that line's stop maps).
     *
     * The matrix spells the same physical station differently per line — Western
     * Railway writes BANDRA, the Harbour line writes Bandra — and the routing
     * graph already uses the matching spelling for each. This resolves a stop
     * name within its own line so those never cross over.
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.String>> stopNamesByLine = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<kotlin.Triple<java.lang.String, java.lang.String, java.lang.String>> badEdges = null;
    private final double DIRECTION_CHANGE_PENALTY = 40.0;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, kotlin.Triple<java.lang.Double, java.lang.Double, java.lang.Double>> scenarioWeights = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.String> scenarioLabels = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.String> stationAliases = null;
    
    /**
     * Beyond this a "next train" isn't useful — it's tomorrow's timetable.
     */
    public static final int MAX_WAIT_MIN = 240;
    
    /**
     * How far past the chosen departure time train options are listed.
     */
    public static final int SEARCH_WINDOW_MIN = 120;
    
    /**
     * Train options offered per rail leg.
     */
    public static final int TRAIN_OPTIONS = 5;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> downIsNorth = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> CR_SLOW = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> CR_FAST = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> CR_AC_FAST = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> CR_REV = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> WR_SLOW = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> WR_FAST = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> WR_REV = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> HL_DOWN = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> HL_REV = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> TH_DOWN = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> TH_REV = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> ML1_DOWN = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> ML1_REV = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> ML2A_DOWN = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> ML2A_REV = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> ML3_DOWN = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> ML3_REV = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> ML7_DOWN = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> ML7_REV = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.engine.TransitEngine.Companion Companion = null;
    
    public TransitEngine(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.data.DataLoader.LoadedData data) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getCanonicals() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, com.example.mumbaitransit.model.MriScore> getMriLookup() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, java.util.List<java.lang.String>> getTwinMap() {
        return null;
    }
    
    private final void registerTwins(java.util.List<java.lang.String> group) {
    }
    
    public final double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.mumbaitransit.model.NearestStation> nearestStations(double lat, double lon, int k) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, java.util.List<com.example.mumbaitransit.model.NearestStation>> nearestStationsGrouped(double lat, double lon) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.mumbaitransit.model.RouteResult dijkstra(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> startNids, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> endNids, double alpha, double beta, double gamma) {
        return null;
    }
    
    private final java.lang.Integer lookupFare(java.lang.String from, java.lang.String to) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, java.lang.String> getScenarioLabels() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.mumbaitransit.model.RouteCard> buildAllRoutes(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.mumbaitransit.model.NearestStation> origStations, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.mumbaitransit.model.NearestStation> destStations, double oLat, double oLon, double dLat, double dLon, @org.jetbrains.annotations.Nullable()
    java.lang.String pinnedOrig, @org.jetbrains.annotations.Nullable()
    java.lang.String pinnedDest, @org.jetbrains.annotations.NotNull()
    java.lang.String origLabel, @org.jetbrains.annotations.NotNull()
    java.lang.String destLabel) {
        return null;
    }
    
    private final java.lang.String modeLabel(java.lang.String mode) {
        return null;
    }
    
    private final double interp(double x, double[] xs, double[] ys) {
        return 0.0;
    }
    
    public final int autoFare(double km) {
        return 0;
    }
    
    public final int taxiFare(double km) {
        return 0;
    }
    
    private final java.lang.String normalizeStop(java.lang.String name) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String minsToHhMm(int m) {
        return null;
    }
    
    /**
     * Normalises the published stop-times matrix into [stopIndex].
     *
     * Two corrections are applied, both driven by what the source data actually
     * contains rather than by assumption:
     *
     * 1. **"00:00" is a null sentinel.** 16 services carry a 00:00 at a station
     *    they don't serve — CR_Up_95210 shows "Kasara 00:00" while genuinely
     *    running Badlapur 10:42 to CSMT 12:12. Left in, it stretches that
     *    service's span from 90 minutes to 732 and corrupts the ordering. It is
     *    dropped unless the service really does depart or arrive at midnight,
     *    which is true of exactly one service (CR_Up_97436, Thane 00:00).
     *
     * 2. **Midnight crossings are unwrapped.** A run that starts 23:38 and ends
     *    00:14 has times that appear to go backwards. Sorting the stop times
     *    and splitting at the largest gap recovers the true order; after this,
     *    no service spans more than 170 minutes and only 2 of 3,069 disagree
     *    with their own journey_min by more than 20 minutes.
     */
    private final void buildStopIndex(com.example.mumbaitransit.data.DataLoader.LoadedData data) {
    }
    
    /**
     * Returns the same stops with times made monotonic along the run.
     *
     * The stop order isn't given by the matrix (columns are alphabetical), so it
     * is recovered from the times themselves: sort them, find the largest gap,
     * and treat that gap as the midnight boundary. Everything before it belongs
     * to the earlier day and gets a day added.
     */
    private final java.util.Map<java.lang.String, java.lang.Integer> unwrapMidnight(java.util.Map<java.lang.String, java.lang.Integer> stops) {
        return null;
    }
    
    /**
     * Next departures on [line] from [fromStop] to [toStop], soonest first.
     *
     * Rail uses the real stop-times matrix, so a train appears only if it
     * actually calls at both stations — that is what keeps fast trains out of
     * results for the stations they skip. Metro has no stop times published, so
     * it still uses the interpolated offset tables.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.mumbaitransit.model.TrainTiming> getNextTrains(@org.jetbrains.annotations.NotNull()
    java.lang.String line, @org.jetbrains.annotations.NotNull()
    java.lang.String fromStop, @org.jetbrains.annotations.NotNull()
    java.lang.String toStop, int arriveAtMins, int n, int windowMin) {
        return null;
    }
    
    private final java.util.List<com.example.mumbaitransit.model.TrainTiming> nextTrainsFromStopTimes(java.lang.String line, java.lang.String fromStop, java.lang.String toStop, int arriveAtMins, int n, int windowMin) {
        return null;
    }
    
    /**
     * Matches a stop to the spelling this line's stop maps use, or null.
     */
    private final java.lang.String resolveStop(java.lang.String line, java.lang.String stop) {
        return null;
    }
    
    private final java.util.List<com.example.mumbaitransit.model.TrainTiming> nextTrainsFromOffsets(java.lang.String line, java.lang.String fromStop, java.lang.String toStop, int arriveAtMins, int n, int windowMin) {
        return null;
    }
    
    /**
     * Every service on one line running one direction, earliest first.
     *
     * The line chat pins reports to a specific train, and a room only ever
     * covers one line in one direction — offering a commuter on the Up platform
     * a list of Down trains would make the attachment worse than useless.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.util.Map<java.lang.String, java.lang.String>> servicesFor(@org.jetbrains.annotations.NotNull()
    java.lang.String line, @org.jetbrains.annotations.NotNull()
    java.lang.String direction) {
        return null;
    }
    
    /**
     * Stations on one line, in running order.
     *
     * Taken from the longest slow service on the line: a fast train's stop map
     * has gaps, so ordering by the most complete run is what produces a list a
     * commuter can scan down the way the line actually runs.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> stationsOn(@org.jetbrains.annotations.NotNull()
    java.lang.String line, @org.jetbrains.annotations.NotNull()
    java.lang.String direction) {
        return null;
    }
    
    private final java.util.Map<java.lang.String, java.lang.Integer> getRevOffsets(java.lang.String line) {
        return null;
    }
    
    private final java.util.Map<java.lang.String, java.lang.Integer> getDownOffsets(java.lang.String line) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010 \n\u0002\b%\b\u0086\b\u0018\u00002\u00020\u0001Bs\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0003\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\n\u0012\u0006\u0010\u000b\u001a\u00020\b\u0012\u0006\u0010\f\u001a\u00020\b\u0012\u0006\u0010\r\u001a\u00020\u0003\u0012\u0006\u0010\u000e\u001a\u00020\u0003\u0012\u0006\u0010\u000f\u001a\u00020\u0003\u0012\u0006\u0010\u0010\u001a\u00020\u0011\u0012\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00030\u0013\u00a2\u0006\u0002\u0010\u0014J\t\u0010&\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\'\u001a\u00020\u0003H\u00c6\u0003J\t\u0010(\u001a\u00020\u0003H\u00c6\u0003J\t\u0010)\u001a\u00020\u0011H\u00c6\u0003J\u000f\u0010*\u001a\b\u0012\u0004\u0012\u00020\u00030\u0013H\u00c6\u0003J\t\u0010+\u001a\u00020\u0003H\u00c6\u0003J\t\u0010,\u001a\u00020\u0003H\u00c6\u0003J\t\u0010-\u001a\u00020\u0003H\u00c6\u0003J\t\u0010.\u001a\u00020\bH\u00c6\u0003J\t\u0010/\u001a\u00020\nH\u00c6\u0003J\t\u00100\u001a\u00020\bH\u00c6\u0003J\t\u00101\u001a\u00020\bH\u00c6\u0003J\t\u00102\u001a\u00020\u0003H\u00c6\u0003J\u0091\u0001\u00103\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00032\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\b2\b\b\u0002\u0010\f\u001a\u00020\b2\b\b\u0002\u0010\r\u001a\u00020\u00032\b\b\u0002\u0010\u000e\u001a\u00020\u00032\b\b\u0002\u0010\u000f\u001a\u00020\u00032\b\b\u0002\u0010\u0010\u001a\u00020\u00112\u000e\b\u0002\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00030\u0013H\u00c6\u0001J\u0013\u00104\u001a\u00020\u00112\b\u00105\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00106\u001a\u00020\nH\u00d6\u0001J\t\u00107\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u000f\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0016R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0011\u0010\f\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001bR\u0011\u0010\r\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0016R\u0011\u0010\u0010\u001a\u00020\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u001dR\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0016R\u0011\u0010\u0006\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u0016R\u0011\u0010\u000b\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u001bR\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00030\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\"R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u0016R\u0011\u0010\u000e\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u0016R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u001b\u00a8\u00068"}, d2 = {"Lcom/example/mumbaitransit/engine/TransitEngine$AdjEdge;", "", "to", "", "edgeType", "line", "mode", "travelMin", "", "fareInr", "", "reliability", "freq", "fromStop", "toStop", "direction", "isFast", "", "skippedStops", "", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;DIDDLjava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/util/List;)V", "getDirection", "()Ljava/lang/String;", "getEdgeType", "getFareInr", "()I", "getFreq", "()D", "getFromStop", "()Z", "getLine", "getMode", "getReliability", "getSkippedStops", "()Ljava/util/List;", "getTo", "getToStop", "getTravelMin", "component1", "component10", "component11", "component12", "component13", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
    public static final class AdjEdge {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String to = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String edgeType = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String line = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String mode = null;
        private final double travelMin = 0.0;
        private final int fareInr = 0;
        private final double reliability = 0.0;
        private final double freq = 0.0;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String fromStop = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String toStop = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String direction = null;
        private final boolean isFast = false;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.String> skippedStops = null;
        
        public AdjEdge(@org.jetbrains.annotations.NotNull()
        java.lang.String to, @org.jetbrains.annotations.NotNull()
        java.lang.String edgeType, @org.jetbrains.annotations.NotNull()
        java.lang.String line, @org.jetbrains.annotations.NotNull()
        java.lang.String mode, double travelMin, int fareInr, double reliability, double freq, @org.jetbrains.annotations.NotNull()
        java.lang.String fromStop, @org.jetbrains.annotations.NotNull()
        java.lang.String toStop, @org.jetbrains.annotations.NotNull()
        java.lang.String direction, boolean isFast, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.String> skippedStops) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTo() {
            return null;
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
        
        public final double getTravelMin() {
            return 0.0;
        }
        
        public final int getFareInr() {
            return 0;
        }
        
        public final double getReliability() {
            return 0.0;
        }
        
        public final double getFreq() {
            return 0.0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getFromStop() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getToStop() {
            return null;
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
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component10() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component11() {
            return null;
        }
        
        public final boolean component12() {
            return false;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.String> component13() {
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
        public final java.lang.String component4() {
            return null;
        }
        
        public final double component5() {
            return 0.0;
        }
        
        public final int component6() {
            return 0;
        }
        
        public final double component7() {
            return 0.0;
        }
        
        public final double component8() {
            return 0.0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component9() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.engine.TransitEngine.AdjEdge copy(@org.jetbrains.annotations.NotNull()
        java.lang.String to, @org.jetbrains.annotations.NotNull()
        java.lang.String edgeType, @org.jetbrains.annotations.NotNull()
        java.lang.String line, @org.jetbrains.annotations.NotNull()
        java.lang.String mode, double travelMin, int fareInr, double reliability, double freq, @org.jetbrains.annotations.NotNull()
        java.lang.String fromStop, @org.jetbrains.annotations.NotNull()
        java.lang.String toStop, @org.jetbrains.annotations.NotNull()
        java.lang.String direction, boolean isFast, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.String> skippedStops) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/example/mumbaitransit/engine/TransitEngine$Companion;", "", "()V", "MAX_WAIT_MIN", "", "SEARCH_WINDOW_MIN", "TRAIN_OPTIONS", "clock12", "", "totalMins", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        /**
         * 1385 → "11:05 PM". Wraps past midnight, so 1500 → "1:00 AM".
         */
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String clock12(int totalMins) {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u000f\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0011\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u0000H\u0096\u0002J\t\u0010\u000e\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000f\u001a\u00020\u0005H\u00c6\u0003J\u001d\u0010\u0010\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u0011\u001a\u00020\u00122\b\u0010\r\u001a\u0004\u0018\u00010\u0013H\u00d6\u0003J\t\u0010\u0014\u001a\u00020\fH\u00d6\u0001J\t\u0010\u0015\u001a\u00020\u0016H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0017"}, d2 = {"Lcom/example/mumbaitransit/engine/TransitEngine$HeapEntry;", "", "cost", "", "state", "Lcom/example/mumbaitransit/engine/TransitEngine$State;", "(DLcom/example/mumbaitransit/engine/TransitEngine$State;)V", "getCost", "()D", "getState", "()Lcom/example/mumbaitransit/engine/TransitEngine$State;", "compareTo", "", "other", "component1", "component2", "copy", "equals", "", "", "hashCode", "toString", "", "app_debug"})
    public static final class HeapEntry implements java.lang.Comparable<com.example.mumbaitransit.engine.TransitEngine.HeapEntry> {
        private final double cost = 0.0;
        @org.jetbrains.annotations.NotNull()
        private final com.example.mumbaitransit.engine.TransitEngine.State state = null;
        
        public HeapEntry(double cost, @org.jetbrains.annotations.NotNull()
        com.example.mumbaitransit.engine.TransitEngine.State state) {
            super();
        }
        
        public final double getCost() {
            return 0.0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.engine.TransitEngine.State getState() {
            return null;
        }
        
        @java.lang.Override()
        public int compareTo(@org.jetbrains.annotations.NotNull()
        com.example.mumbaitransit.engine.TransitEngine.HeapEntry other) {
            return 0;
        }
        
        public final double component1() {
            return 0.0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.engine.TransitEngine.State component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.engine.TransitEngine.HeapEntry copy(double cost, @org.jetbrains.annotations.NotNull()
        com.example.mumbaitransit.engine.TransitEngine.State state) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0006J\t\u0010\u000b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\r\u001a\u00020\u0003H\u00c6\u0003J\'\u0010\u000e\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\u000f\u001a\u00020\u00102\b\u0010\u0011\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0012\u001a\u00020\u0013H\u00d6\u0001J\t\u0010\u0014\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\bR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\b\u00a8\u0006\u0015"}, d2 = {"Lcom/example/mumbaitransit/engine/TransitEngine$State;", "", "nodeId", "", "prevLine", "prevDir", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getNodeId", "()Ljava/lang/String;", "getPrevDir", "getPrevLine", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class State {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String nodeId = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String prevLine = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String prevDir = null;
        
        public State(@org.jetbrains.annotations.NotNull()
        java.lang.String nodeId, @org.jetbrains.annotations.NotNull()
        java.lang.String prevLine, @org.jetbrains.annotations.NotNull()
        java.lang.String prevDir) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getNodeId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getPrevLine() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getPrevDir() {
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
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.engine.TransitEngine.State copy(@org.jetbrains.annotations.NotNull()
        java.lang.String nodeId, @org.jetbrains.annotations.NotNull()
        java.lang.String prevLine, @org.jetbrains.annotations.NotNull()
        java.lang.String prevDir) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u000e\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B%\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\bJ\t\u0010\u000f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0010\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0011\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0006H\u00c6\u0003J1\u0010\u0013\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u0006H\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0017\u001a\u00020\u0018H\u00d6\u0001J\t\u0010\u0019\u001a\u00020\u0006H\u00d6\u0001R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\f\u00a8\u0006\u001a"}, d2 = {"Lcom/example/mumbaitransit/engine/TransitEngine$StationPoint;", "", "lat", "", "lon", "canonical", "", "line", "(DDLjava/lang/String;Ljava/lang/String;)V", "getCanonical", "()Ljava/lang/String;", "getLat", "()D", "getLine", "getLon", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class StationPoint {
        private final double lat = 0.0;
        private final double lon = 0.0;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String canonical = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String line = null;
        
        public StationPoint(double lat, double lon, @org.jetbrains.annotations.NotNull()
        java.lang.String canonical, @org.jetbrains.annotations.NotNull()
        java.lang.String line) {
            super();
        }
        
        public final double getLat() {
            return 0.0;
        }
        
        public final double getLon() {
            return 0.0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getCanonical() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getLine() {
            return null;
        }
        
        public final double component1() {
            return 0.0;
        }
        
        public final double component2() {
            return 0.0;
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
        public final com.example.mumbaitransit.engine.TransitEngine.StationPoint copy(double lat, double lon, @org.jetbrains.annotations.NotNull()
        java.lang.String canonical, @org.jetbrains.annotations.NotNull()
        java.lang.String line) {
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