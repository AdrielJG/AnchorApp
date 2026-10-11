# Keep data model classes
-keep class com.example.mumbaitransit.model.** { *; }
-keep class com.example.mumbaitransit.engine.StationCoords { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# OpenCSV
-dontwarn com.opencsv.**
