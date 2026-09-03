package com.example.mumbaitransit.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000V\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\u0018\u0000 $2\u00020\u00012\u00020\u0002:\u0001$B\u0005\u00a2\u0006\u0002\u0010\u0003J&\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00120\u00112\u0006\u0010\u0013\u001a\u00020\u00122\u0006\u0010\u0014\u001a\u00020\u00122\u0006\u0010\u0015\u001a\u00020\u0016H\u0002J(\u0010\u0017\u001a\u00020\t2\u0006\u0010\u0018\u001a\u00020\t2\u0006\u0010\u0019\u001a\u00020\t2\u0006\u0010\u001a\u001a\u00020\t2\u0006\u0010\u001b\u001a\u00020\tH\u0002J\u0012\u0010\u001c\u001a\u00020\u001d2\b\u0010\u001e\u001a\u0004\u0018\u00010\u001fH\u0014J\u0010\u0010 \u001a\u00020\u001d2\u0006\u0010!\u001a\u00020\fH\u0016J\b\u0010\"\u001a\u00020#H\u0016R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006%"}, d2 = {"Lcom/example/mumbaitransit/ui/RoutePreviewActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "Lcom/google/android/gms/maps/OnMapReadyCallback;", "()V", "binding", "Lcom/example/mumbaitransit/databinding/ActivityRoutePreviewBinding;", "destLabel", "", "destLat", "", "destLon", "googleMap", "Lcom/google/android/gms/maps/GoogleMap;", "originLabel", "originLat", "originLon", "buildCurvedPath", "", "Lcom/google/android/gms/maps/model/LatLng;", "start", "end", "steps", "", "haversineKm", "lat1", "lon1", "lat2", "lon2", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onMapReady", "map", "onSupportNavigateUp", "", "Companion", "app_debug"})
public final class RoutePreviewActivity extends androidx.appcompat.app.AppCompatActivity implements com.google.android.gms.maps.OnMapReadyCallback {
    private com.example.mumbaitransit.databinding.ActivityRoutePreviewBinding binding;
    private com.google.android.gms.maps.GoogleMap googleMap;
    private double originLat = 0.0;
    private double originLon = 0.0;
    private double destLat = 0.0;
    private double destLon = 0.0;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String originLabel = "";
    @org.jetbrains.annotations.NotNull()
    private java.lang.String destLabel = "";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_ORIGIN_LAT = "origin_lat";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_ORIGIN_LON = "origin_lon";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_DEST_LAT = "dest_lat";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_DEST_LON = "dest_lon";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_ORIGIN_LABEL = "origin_label";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_DEST_LABEL = "dest_label";
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.ui.RoutePreviewActivity.Companion Companion = null;
    
    public RoutePreviewActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    public void onMapReady(@org.jetbrains.annotations.NotNull()
    com.google.android.gms.maps.GoogleMap map) {
    }
    
    /**
     * Generates a smooth arc path between two LatLng points.
     * Adds a slight perpendicular offset midway to make it look like a road curve.
     */
    private final java.util.List<com.google.android.gms.maps.model.LatLng> buildCurvedPath(com.google.android.gms.maps.model.LatLng start, com.google.android.gms.maps.model.LatLng end, int steps) {
        return null;
    }
    
    private final double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        return 0.0;
    }
    
    @java.lang.Override()
    public boolean onSupportNavigateUp() {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/example/mumbaitransit/ui/RoutePreviewActivity$Companion;", "", "()V", "EXTRA_DEST_LABEL", "", "EXTRA_DEST_LAT", "EXTRA_DEST_LON", "EXTRA_ORIGIN_LABEL", "EXTRA_ORIGIN_LAT", "EXTRA_ORIGIN_LON", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}