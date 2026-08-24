package com.example.mumbaitransit.auth;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fH\u0086@\u00a2\u0006\u0002\u0010\u000eJ\u0016\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H\u0086@\u00a2\u0006\u0002\u0010\u0013J\u0016\u0010\u0014\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H\u0086@\u00a2\u0006\u0002\u0010\u0013J\u0016\u0010\u0015\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H\u0086@\u00a2\u0006\u0002\u0010\u0013J\u0006\u0010\u0016\u001a\u00020\u0006R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0017"}, d2 = {"Lcom/example/mumbaitransit/auth/SavedRouteRepository;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "dao", "Lcom/example/mumbaitransit/auth/SavedRouteDao;", "session", "Lcom/example/mumbaitransit/auth/SessionManager;", "getSession", "()Lcom/example/mumbaitransit/auth/SessionManager;", "getSavedRoutes", "", "Lcom/example/mumbaitransit/auth/SavedRouteEntity;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "isSaved", "", "card", "Lcom/example/mumbaitransit/model/RouteCard;", "(Lcom/example/mumbaitransit/model/RouteCard;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "removeRoute", "saveRoute", "savedRouteDao", "app_debug"})
public final class SavedRouteRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.example.mumbaitransit.auth.SavedRouteDao dao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.mumbaitransit.auth.SessionManager session = null;
    
    public SavedRouteRepository(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.auth.SessionManager getSession() {
        return null;
    }
    
    /**
     * Expose DAO for direct operations (e.g. delete by entity in SavedRoutesActivity)
     */
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.auth.SavedRouteDao savedRouteDao() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveRoute(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.model.RouteCard card, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object removeRoute(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.model.RouteCard card, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object isSaved(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.model.RouteCard card, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getSavedRoutes(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.example.mumbaitransit.auth.SavedRouteEntity>> $completion) {
        return null;
    }
}