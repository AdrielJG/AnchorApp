package com.example.mumbaitransit.auth;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\b\u0002\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ0\u0010\u000b\u001a\u0004\u0018\u00010\u00052\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u0010J\u001c\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00050\u00122\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\u0013\u001a\u00020\t2\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0014"}, d2 = {"Lcom/example/mumbaitransit/auth/SavedRouteDao;", "", "delete", "", "route", "Lcom/example/mumbaitransit/auth/SavedRouteEntity;", "(Lcom/example/mumbaitransit/auth/SavedRouteEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteAllForUser", "userId", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "findExact", "origin", "", "dest", "scenario", "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getForUser", "", "insert", "app_debug"})
@androidx.room.Dao()
public abstract interface SavedRouteDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.auth.SavedRouteEntity route, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object delete(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.auth.SavedRouteEntity route, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM saved_routes WHERE user_id = :userId ORDER BY saved_at DESC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getForUser(long userId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.example.mumbaitransit.auth.SavedRouteEntity>> $completion);
    
    @androidx.room.Query(value = "\n        SELECT * FROM saved_routes\n        WHERE user_id = :userId\n          AND origin_label = :origin\n          AND dest_label   = :dest\n          AND scenario     = :scenario\n        LIMIT 1\n    ")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object findExact(long userId, @org.jetbrains.annotations.NotNull()
    java.lang.String origin, @org.jetbrains.annotations.NotNull()
    java.lang.String dest, @org.jetbrains.annotations.NotNull()
    java.lang.String scenario, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.mumbaitransit.auth.SavedRouteEntity> $completion);
    
    @androidx.room.Query(value = "DELETE FROM saved_routes WHERE user_id = :userId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteAllForUser(long userId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}