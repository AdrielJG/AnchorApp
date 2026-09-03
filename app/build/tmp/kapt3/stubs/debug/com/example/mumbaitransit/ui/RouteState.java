package com.example.mumbaitransit.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0004\u0003\u0004\u0005\u0006B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0004\u0007\b\t\n\u00a8\u0006\u000b"}, d2 = {"Lcom/example/mumbaitransit/ui/RouteState;", "", "()V", "Error", "Idle", "Results", "Searching", "Lcom/example/mumbaitransit/ui/RouteState$Error;", "Lcom/example/mumbaitransit/ui/RouteState$Idle;", "Lcom/example/mumbaitransit/ui/RouteState$Results;", "Lcom/example/mumbaitransit/ui/RouteState$Searching;", "app_debug"})
public abstract class RouteState {
    
    private RouteState() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/example/mumbaitransit/ui/RouteState$Error;", "Lcom/example/mumbaitransit/ui/RouteState;", "msg", "", "(Ljava/lang/String;)V", "getMsg", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class Error extends com.example.mumbaitransit.ui.RouteState {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String msg = null;
        
        public Error(@org.jetbrains.annotations.NotNull()
        java.lang.String msg) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getMsg() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.mumbaitransit.ui.RouteState.Error copy(@org.jetbrains.annotations.NotNull()
        java.lang.String msg) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/mumbaitransit/ui/RouteState$Idle;", "Lcom/example/mumbaitransit/ui/RouteState;", "()V", "app_debug"})
    public static final class Idle extends com.example.mumbaitransit.ui.RouteState {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.mumbaitransit.ui.RouteState.Idle INSTANCE = null;
        
        private Idle() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\f\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B#\u0012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\bJ\u000f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\t\u0010\u000f\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u0010\u001a\u00020\u0006H\u00c6\u0003J-\u0010\u0011\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u0006H\u00c6\u0001J\u0013\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0015H\u00d6\u0003J\t\u0010\u0016\u001a\u00020\u0017H\u00d6\u0001J\t\u0010\u0018\u001a\u00020\u0006H\u00d6\u0001R\u0011\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\nR\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0019"}, d2 = {"Lcom/example/mumbaitransit/ui/RouteState$Results;", "Lcom/example/mumbaitransit/ui/RouteState;", "routes", "", "Lcom/example/mumbaitransit/model/RouteCard;", "origLabel", "", "destLabel", "(Ljava/util/List;Ljava/lang/String;Ljava/lang/String;)V", "getDestLabel", "()Ljava/lang/String;", "getOrigLabel", "getRoutes", "()Ljava/util/List;", "component1", "component2", "component3", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class Results extends com.example.mumbaitransit.ui.RouteState {
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<com.example.mumbaitransit.model.RouteCard> routes = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String origLabel = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String destLabel = null;
        
        public Results(@org.jetbrains.annotations.NotNull()
        java.util.List<com.example.mumbaitransit.model.RouteCard> routes, @org.jetbrains.annotations.NotNull()
        java.lang.String origLabel, @org.jetbrains.annotations.NotNull()
        java.lang.String destLabel) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.example.mumbaitransit.model.RouteCard> getRoutes() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getOrigLabel() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getDestLabel() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.example.mumbaitransit.model.RouteCard> component1() {
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
        public final com.example.mumbaitransit.ui.RouteState.Results copy(@org.jetbrains.annotations.NotNull()
        java.util.List<com.example.mumbaitransit.model.RouteCard> routes, @org.jetbrains.annotations.NotNull()
        java.lang.String origLabel, @org.jetbrains.annotations.NotNull()
        java.lang.String destLabel) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/mumbaitransit/ui/RouteState$Searching;", "Lcom/example/mumbaitransit/ui/RouteState;", "()V", "app_debug"})
    public static final class Searching extends com.example.mumbaitransit.ui.RouteState {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.mumbaitransit.ui.RouteState.Searching INSTANCE = null;
        
        private Searching() {
        }
    }
}