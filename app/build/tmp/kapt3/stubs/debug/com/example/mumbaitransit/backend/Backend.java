package com.example.mumbaitransit.backend;

/**
 * Whether the app is talking to Firebase or to the on-device fallback.
 *
 * The Firebase SDKs are always compiled in, but they only initialize when
 * app/google-services.json is present. Checking once here — rather than letting
 * every call site discover it by throwing — is what lets accounts and chat run
 * against the real database when it is configured and still run locally when it
 * is not, with no code changes in between. See FIREBASE_SETUP.md.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\b\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\nR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/example/mumbaitransit/backend/Backend;", "", "()V", "TAG", "", "checked", "", "cloud", "isCloud", "context", "Landroid/content/Context;", "app_debug"})
public final class Backend {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "AnchorBackend";
    @kotlin.jvm.Volatile()
    private static volatile boolean checked = false;
    @kotlin.jvm.Volatile()
    private static volatile boolean cloud = false;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.backend.Backend INSTANCE = null;
    
    private Backend() {
        super();
    }
    
    public final boolean isCloud(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
}