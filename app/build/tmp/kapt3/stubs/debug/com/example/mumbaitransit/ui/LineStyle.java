package com.example.mumbaitransit.ui;

/**
 * Line colours and short names, in one place.
 *
 * RoutesResultActivity and the share card both paint the same lines, so keeping
 * the mapping here stops a shared card from showing Harbour Line in a different
 * colour than the route list the sender was looking at.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\u0006\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\u0007\u001a\u00020\u00062\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\u0006J\u000e\u0010\n\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\fJ\u000e\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u0006J\u000e\u0010\u000f\u001a\u00020\u00062\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/example/mumbaitransit/ui/LineStyle;", "", "()V", "color", "", "line", "", "colorHex", "modeColor", "type", "mriColor", "pct", "", "scenarioColor", "scenario", "shorten", "app_debug"})
public final class LineStyle {
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.ui.LineStyle INSTANCE = null;
    
    private LineStyle() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String colorHex(@org.jetbrains.annotations.NotNull()
    java.lang.String line) {
        return null;
    }
    
    public final int color(@org.jetbrains.annotations.NotNull()
    java.lang.String line) {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String shorten(@org.jetbrains.annotations.NotNull()
    java.lang.String line) {
        return null;
    }
    
    /**
     * Accent colour for a route card, by scenario.
     */
    public final int scenarioColor(@org.jetbrains.annotations.NotNull()
    java.lang.String scenario) {
        return 0;
    }
    
    /**
     * Accent colour for the non-transit cards (bus / auto / cab).
     */
    public final int modeColor(@org.jetbrains.annotations.NotNull()
    java.lang.String type) {
        return 0;
    }
    
    public final int mriColor(double pct) {
        return 0;
    }
}