package com.example.mumbaitransit.backend

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Whether the app is talking to Firebase or to the on-device fallback.
 *
 * The Firebase SDKs are always compiled in, but they only initialize when
 * app/google-services.json is present. Checking once here — rather than letting
 * every call site discover it by throwing — is what lets accounts and chat run
 * against the real database when it is configured and still run locally when it
 * is not, with no code changes in between. See FIREBASE_SETUP.md.
 */
object Backend {

    private const val TAG = "AnchorBackend"

    @Volatile private var checked = false
    @Volatile private var cloud = false

    fun isCloud(context: Context): Boolean {
        if (checked) return cloud
        synchronized(this) {
            if (checked) return cloud
            cloud = try {
                // Returns null when google-services.json was never added, and
                // throws on some OEM builds — both mean "no backend".
                FirebaseApp.initializeApp(context.applicationContext) != null
            } catch (t: Throwable) {
                Log.w(TAG, "Firebase not configured, using on-device storage", t)
                false
            }
            checked = true
            if (!cloud) {
                Log.w(TAG, "Running on-device: accounts and chat stay on this phone.")
            }
            return cloud
        }
    }
}
