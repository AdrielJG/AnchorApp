package com.example.mumbaitransit.share;

/**
 * Shares one route as a picture plus a readable caption.
 *
 * A picture is the only form of "shareable route" that works for every
 * recipient: WhatsApp, Telegram, email and Drive all render it, and the person
 * on the other end needs nothing installed. The caption carries the same
 * information as plain text, so it still reads sensibly if the image is
 * stripped or the recipient is on a screen reader.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH\u0002J(\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00042\u0006\u0010\u0011\u001a\u00020\u0004H\u0002J\u001e\u0010\u0012\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tJ \u0010\u0013\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH\u0002J\u0010\u0010\u0014\u001a\u00020\u00042\u0006\u0010\u0015\u001a\u00020\u0004H\u0002J\u0010\u0010\u0011\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u0007H\u0002J\"\u0010\u0016\u001a\u0004\u0018\u00010\u000f2\u0006\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u0006\u001a\u00020\u0007H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001b"}, d2 = {"Lcom/example/mumbaitransit/share/RouteSharer;", "", "()V", "CACHE_DIR", "", "caption", "card", "Lcom/example/mumbaitransit/model/RouteCard;", "ctx", "Lcom/example/mumbaitransit/share/ShareContext;", "launchChooser", "", "activity", "Landroidx/appcompat/app/AppCompatActivity;", "uri", "Landroid/net/Uri;", "text", "subject", "share", "shareTextOnly", "slug", "value", "writePng", "context", "Landroid/content/Context;", "bitmap", "Landroid/graphics/Bitmap;", "app_debug"})
public final class RouteSharer {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CACHE_DIR = "shared_routes";
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.share.RouteSharer INSTANCE = null;
    
    private RouteSharer() {
        super();
    }
    
    public final void share(@org.jetbrains.annotations.NotNull()
    androidx.appcompat.app.AppCompatActivity activity, @org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.model.RouteCard card, @org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.share.ShareContext ctx) {
    }
    
    private final android.net.Uri writePng(android.content.Context context, android.graphics.Bitmap bitmap, com.example.mumbaitransit.model.RouteCard card) {
        return null;
    }
    
    private final void launchChooser(androidx.appcompat.app.AppCompatActivity activity, android.net.Uri uri, java.lang.String text, java.lang.String subject) {
    }
    
    private final void shareTextOnly(androidx.appcompat.app.AppCompatActivity activity, com.example.mumbaitransit.model.RouteCard card, com.example.mumbaitransit.share.ShareContext ctx) {
    }
    
    private final java.lang.String subject(com.example.mumbaitransit.model.RouteCard card) {
        return null;
    }
    
    /**
     * The same route in plain text, for previews and image-less recipients.
     */
    private final java.lang.String caption(com.example.mumbaitransit.model.RouteCard card, com.example.mumbaitransit.share.ShareContext ctx) {
        return null;
    }
    
    private final java.lang.String slug(java.lang.String value) {
        return null;
    }
}