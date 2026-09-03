package com.example.mumbaitransit.auth;

/**
 * Sign-up and sign-in.
 *
 * Routes to Firebase Auth when the backend is configured, so an account works
 * on any device; falls back to the on-device Room table otherwise. Call sites
 * see the same three methods either way.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u001e\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u0013H\u0086@\u00a2\u0006\u0002\u0010\u0015J\u0006\u0010\u0016\u001a\u00020\u0017J\u0014\u0010\u0018\u001a\u00020\u00132\n\u0010\u0019\u001a\u00060\u001aj\u0002`\u001bH\u0002J\u0006\u0010\u001c\u001a\u00020\u0017J&\u0010\u001d\u001a\u00020\u00112\u0006\u0010\u001e\u001a\u00020\u00132\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u0013H\u0086@\u00a2\u0006\u0002\u0010\u001fR\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\t\u001a\u00020\n8F\u00a2\u0006\u0006\u001a\u0004\b\t\u0010\u000bR\u0011\u0010\f\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006 "}, d2 = {"Lcom/example/mumbaitransit/auth/AuthRepository;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "cloud", "Lcom/example/mumbaitransit/auth/CloudAuth;", "dao", "Lcom/example/mumbaitransit/auth/UserDao;", "isCloud", "", "()Z", "session", "Lcom/example/mumbaitransit/auth/SessionManager;", "getSession", "()Lcom/example/mumbaitransit/auth/SessionManager;", "logIn", "Lcom/example/mumbaitransit/auth/AuthResult;", "email", "", "password", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "logOut", "", "readable", "e", "Ljava/lang/Exception;", "Lkotlin/Exception;", "restoreSession", "signUp", "username", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class AuthRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.example.mumbaitransit.auth.UserDao dao = null;
    @org.jetbrains.annotations.Nullable()
    private final com.example.mumbaitransit.auth.CloudAuth cloud = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.mumbaitransit.auth.SessionManager session = null;
    
    public AuthRepository(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.mumbaitransit.auth.SessionManager getSession() {
        return null;
    }
    
    public final boolean isCloud() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object signUp(@org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.mumbaitransit.auth.AuthResult> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object logIn(@org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.mumbaitransit.auth.AuthResult> $completion) {
        return null;
    }
    
    public final void logOut() {
    }
    
    /**
     * Restores a cloud session the SDK is still holding.
     *
     * Firebase keeps the user signed in across restarts, but SharedPreferences
     * is cleared on reinstall — without this the SDK would think you are signed
     * in while the app thinks you are not.
     */
    public final void restoreSession() {
    }
    
    /**
     * Firebase exception text is written for developers, not commuters.
     */
    private final java.lang.String readable(java.lang.Exception e) {
        return null;
    }
}