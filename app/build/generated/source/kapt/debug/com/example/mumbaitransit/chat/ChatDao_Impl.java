package com.example.mumbaitransit.chat;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@SuppressWarnings({"unchecked", "deprecation"})
public final class ChatDao_Impl implements ChatDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChatMessage> __insertionAdapterOfChatMessage;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  public ChatDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChatMessage = new EntityInsertionAdapter<ChatMessage>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `chat_messages` (`id`,`roomId`,`uid`,`username`,`text`,`reportType`,`trainNo`,`trainLabel`,`station`,`platform`,`sentAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChatMessage entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getRoomId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getRoomId());
        }
        if (entity.getUid() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getUid());
        }
        if (entity.getUsername() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getUsername());
        }
        if (entity.getText() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getText());
        }
        if (entity.getReportType() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getReportType());
        }
        if (entity.getTrainNo() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getTrainNo());
        }
        if (entity.getTrainLabel() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getTrainLabel());
        }
        if (entity.getStation() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getStation());
        }
        if (entity.getPlatform() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getPlatform());
        }
        statement.bindLong(11, entity.getSentAt());
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM chat_messages WHERE sentAt < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final ChatMessage message, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfChatMessage.insertAndReturnId(message);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final long before, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, before);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ChatMessage>> observeRoom(final String roomId) {
    final String _sql = "SELECT * FROM chat_messages WHERE roomId = ? ORDER BY sentAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (roomId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, roomId);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"chat_messages"}, new Callable<List<ChatMessage>>() {
      @Override
      @NonNull
      public List<ChatMessage> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRoomId = CursorUtil.getColumnIndexOrThrow(_cursor, "roomId");
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfReportType = CursorUtil.getColumnIndexOrThrow(_cursor, "reportType");
          final int _cursorIndexOfTrainNo = CursorUtil.getColumnIndexOrThrow(_cursor, "trainNo");
          final int _cursorIndexOfTrainLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "trainLabel");
          final int _cursorIndexOfStation = CursorUtil.getColumnIndexOrThrow(_cursor, "station");
          final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
          final int _cursorIndexOfSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "sentAt");
          final List<ChatMessage> _result = new ArrayList<ChatMessage>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChatMessage _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpRoomId;
            if (_cursor.isNull(_cursorIndexOfRoomId)) {
              _tmpRoomId = null;
            } else {
              _tmpRoomId = _cursor.getString(_cursorIndexOfRoomId);
            }
            final String _tmpUid;
            if (_cursor.isNull(_cursorIndexOfUid)) {
              _tmpUid = null;
            } else {
              _tmpUid = _cursor.getString(_cursorIndexOfUid);
            }
            final String _tmpUsername;
            if (_cursor.isNull(_cursorIndexOfUsername)) {
              _tmpUsername = null;
            } else {
              _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            }
            final String _tmpText;
            if (_cursor.isNull(_cursorIndexOfText)) {
              _tmpText = null;
            } else {
              _tmpText = _cursor.getString(_cursorIndexOfText);
            }
            final String _tmpReportType;
            if (_cursor.isNull(_cursorIndexOfReportType)) {
              _tmpReportType = null;
            } else {
              _tmpReportType = _cursor.getString(_cursorIndexOfReportType);
            }
            final String _tmpTrainNo;
            if (_cursor.isNull(_cursorIndexOfTrainNo)) {
              _tmpTrainNo = null;
            } else {
              _tmpTrainNo = _cursor.getString(_cursorIndexOfTrainNo);
            }
            final String _tmpTrainLabel;
            if (_cursor.isNull(_cursorIndexOfTrainLabel)) {
              _tmpTrainLabel = null;
            } else {
              _tmpTrainLabel = _cursor.getString(_cursorIndexOfTrainLabel);
            }
            final String _tmpStation;
            if (_cursor.isNull(_cursorIndexOfStation)) {
              _tmpStation = null;
            } else {
              _tmpStation = _cursor.getString(_cursorIndexOfStation);
            }
            final String _tmpPlatform;
            if (_cursor.isNull(_cursorIndexOfPlatform)) {
              _tmpPlatform = null;
            } else {
              _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
            }
            final long _tmpSentAt;
            _tmpSentAt = _cursor.getLong(_cursorIndexOfSentAt);
            _item = new ChatMessage(_tmpId,_tmpRoomId,_tmpUid,_tmpUsername,_tmpText,_tmpReportType,_tmpTrainNo,_tmpTrainLabel,_tmpStation,_tmpPlatform,_tmpSentAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object latestPerRoom(final Continuation<? super List<ChatMessage>> $completion) {
    final String _sql = "SELECT * FROM chat_messages WHERE id IN (SELECT MAX(id) FROM chat_messages GROUP BY roomId)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ChatMessage>>() {
      @Override
      @NonNull
      public List<ChatMessage> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRoomId = CursorUtil.getColumnIndexOrThrow(_cursor, "roomId");
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfReportType = CursorUtil.getColumnIndexOrThrow(_cursor, "reportType");
          final int _cursorIndexOfTrainNo = CursorUtil.getColumnIndexOrThrow(_cursor, "trainNo");
          final int _cursorIndexOfTrainLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "trainLabel");
          final int _cursorIndexOfStation = CursorUtil.getColumnIndexOrThrow(_cursor, "station");
          final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
          final int _cursorIndexOfSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "sentAt");
          final List<ChatMessage> _result = new ArrayList<ChatMessage>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChatMessage _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpRoomId;
            if (_cursor.isNull(_cursorIndexOfRoomId)) {
              _tmpRoomId = null;
            } else {
              _tmpRoomId = _cursor.getString(_cursorIndexOfRoomId);
            }
            final String _tmpUid;
            if (_cursor.isNull(_cursorIndexOfUid)) {
              _tmpUid = null;
            } else {
              _tmpUid = _cursor.getString(_cursorIndexOfUid);
            }
            final String _tmpUsername;
            if (_cursor.isNull(_cursorIndexOfUsername)) {
              _tmpUsername = null;
            } else {
              _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            }
            final String _tmpText;
            if (_cursor.isNull(_cursorIndexOfText)) {
              _tmpText = null;
            } else {
              _tmpText = _cursor.getString(_cursorIndexOfText);
            }
            final String _tmpReportType;
            if (_cursor.isNull(_cursorIndexOfReportType)) {
              _tmpReportType = null;
            } else {
              _tmpReportType = _cursor.getString(_cursorIndexOfReportType);
            }
            final String _tmpTrainNo;
            if (_cursor.isNull(_cursorIndexOfTrainNo)) {
              _tmpTrainNo = null;
            } else {
              _tmpTrainNo = _cursor.getString(_cursorIndexOfTrainNo);
            }
            final String _tmpTrainLabel;
            if (_cursor.isNull(_cursorIndexOfTrainLabel)) {
              _tmpTrainLabel = null;
            } else {
              _tmpTrainLabel = _cursor.getString(_cursorIndexOfTrainLabel);
            }
            final String _tmpStation;
            if (_cursor.isNull(_cursorIndexOfStation)) {
              _tmpStation = null;
            } else {
              _tmpStation = _cursor.getString(_cursorIndexOfStation);
            }
            final String _tmpPlatform;
            if (_cursor.isNull(_cursorIndexOfPlatform)) {
              _tmpPlatform = null;
            } else {
              _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
            }
            final long _tmpSentAt;
            _tmpSentAt = _cursor.getLong(_cursorIndexOfSentAt);
            _item = new ChatMessage(_tmpId,_tmpRoomId,_tmpUid,_tmpUsername,_tmpText,_tmpReportType,_tmpTrainNo,_tmpTrainLabel,_tmpStation,_tmpPlatform,_tmpSentAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object countSince(final String roomId, final long since,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM chat_messages WHERE roomId = ? AND sentAt > ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (roomId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, roomId);
    }
    _argIndex = 2;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
