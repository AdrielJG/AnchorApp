package com.example.mumbaitransit.chat;

/**
 * Trains and stations for one chat room.
 *
 * Everything here is filtered by the room's own line and direction, so a
 * report posted in Central Up can only ever name a Central Up train.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u001c\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eJ\u001c\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00100\u00062\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eJ\u001c\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00100\u00062\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\t\u00a8\u0006\u0012"}, d2 = {"Lcom/example/mumbaitransit/chat/RoomTimetable;", "", "()V", "WINDOW_MIN", "", "platforms", "", "", "getPlatforms", "()Ljava/util/List;", "stationsFor", "engine", "Lcom/example/mumbaitransit/engine/TransitEngine;", "room", "Lcom/example/mumbaitransit/chat/ChatRoom;", "trainsFor", "Lcom/example/mumbaitransit/chat/TrainOption;", "trainsFromNow", "app_debug"})
public final class RoomTimetable {
    
    /**
     * How far ahead of now the picker opens. Earlier services stay reachable via search.
     */
    private static final int WINDOW_MIN = 180;
    
    /**
     * Platform numbers offered for a platform-change report.
     */
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> platforms = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.mumbaitransit.chat.RoomTimetable INSTANCE = null;
    
    private RoomTimetable() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.mumbaitransit.chat.TrainOption> trainsFor(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.engine.TransitEngine engine, @org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.chat.ChatRoom room) {
        return null;
    }
    
    /**
     * The same list rotated so the next departure sits at the top.
     *
     * A commuter reporting a delay is almost always talking about the train in
     * front of them, so the one leaving in ten minutes should not be 400 rows
     * down a list that starts at 04:35.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.mumbaitransit.chat.TrainOption> trainsFromNow(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.engine.TransitEngine engine, @org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.chat.ChatRoom room) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> stationsFor(@org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.engine.TransitEngine engine, @org.jetbrains.annotations.NotNull()
    com.example.mumbaitransit.chat.ChatRoom room) {
        return null;
    }
    
    /**
     * Platform numbers offered for a platform-change report.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getPlatforms() {
        return null;
    }
}