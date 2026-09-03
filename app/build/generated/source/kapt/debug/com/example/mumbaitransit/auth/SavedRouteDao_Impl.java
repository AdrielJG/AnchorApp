package com.example.mumbaitransit.auth;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
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

@SuppressWarnings({"unchecked", "deprecation"})
public final class SavedRouteDao_Impl implements SavedRouteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SavedRouteEntity> __insertionAdapterOfSavedRouteEntity;

  private final EntityDeletionOrUpdateAdapter<SavedRouteEntity> __deletionAdapterOfSavedRouteEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllForUser;

  public SavedRouteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSavedRouteEntity = new EntityInsertionAdapter<SavedRouteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `saved_routes` (`id`,`user_id`,`origin_label`,`dest_label`,`scenario`,`scenario_label`,`mode_str`,`total_min`,`total_fare`,`transfers`,`lines_used`,`route_type`,`saved_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavedRouteEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getUserId());
        if (entity.getOriginLabel() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getOriginLabel());
        }
        if (entity.getDestLabel() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDestLabel());
        }
        if (entity.getScenario() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getScenario());
        }
        if (entity.getScenarioLabel() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getScenarioLabel());
        }
        if (entity.getModeStr() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getModeStr());
        }
        statement.bindDouble(8, entity.getTotalMin());
        statement.bindLong(9, entity.getTotalFare());
        statement.bindLong(10, entity.getTransfers());
        if (entity.getLinesUsed() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getLinesUsed());
        }
        if (entity.getRouteType() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getRouteType());
        }
        statement.bindLong(13, entity.getSavedAt());
      }
    };
    this.__deletionAdapterOfSavedRouteEntity = new EntityDeletionOrUpdateAdapter<SavedRouteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `saved_routes` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavedRouteEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAllForUser = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM saved_routes WHERE user_id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final SavedRouteEntity route, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSavedRouteEntity.insertAndReturnId(route);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final SavedRouteEntity route, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSavedRouteEntity.handle(route);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllForUser(final long userId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllForUser.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
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
          __preparedStmtOfDeleteAllForUser.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getForUser(final long userId,
      final Continuation<? super List<SavedRouteEntity>> $completion) {
    final String _sql = "SELECT * FROM saved_routes WHERE user_id = ? ORDER BY saved_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SavedRouteEntity>>() {
      @Override
      @NonNull
      public List<SavedRouteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "user_id");
          final int _cursorIndexOfOriginLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "origin_label");
          final int _cursorIndexOfDestLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "dest_label");
          final int _cursorIndexOfScenario = CursorUtil.getColumnIndexOrThrow(_cursor, "scenario");
          final int _cursorIndexOfScenarioLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "scenario_label");
          final int _cursorIndexOfModeStr = CursorUtil.getColumnIndexOrThrow(_cursor, "mode_str");
          final int _cursorIndexOfTotalMin = CursorUtil.getColumnIndexOrThrow(_cursor, "total_min");
          final int _cursorIndexOfTotalFare = CursorUtil.getColumnIndexOrThrow(_cursor, "total_fare");
          final int _cursorIndexOfTransfers = CursorUtil.getColumnIndexOrThrow(_cursor, "transfers");
          final int _cursorIndexOfLinesUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "lines_used");
          final int _cursorIndexOfRouteType = CursorUtil.getColumnIndexOrThrow(_cursor, "route_type");
          final int _cursorIndexOfSavedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "saved_at");
          final List<SavedRouteEntity> _result = new ArrayList<SavedRouteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SavedRouteEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final String _tmpOriginLabel;
            if (_cursor.isNull(_cursorIndexOfOriginLabel)) {
              _tmpOriginLabel = null;
            } else {
              _tmpOriginLabel = _cursor.getString(_cursorIndexOfOriginLabel);
            }
            final String _tmpDestLabel;
            if (_cursor.isNull(_cursorIndexOfDestLabel)) {
              _tmpDestLabel = null;
            } else {
              _tmpDestLabel = _cursor.getString(_cursorIndexOfDestLabel);
            }
            final String _tmpScenario;
            if (_cursor.isNull(_cursorIndexOfScenario)) {
              _tmpScenario = null;
            } else {
              _tmpScenario = _cursor.getString(_cursorIndexOfScenario);
            }
            final String _tmpScenarioLabel;
            if (_cursor.isNull(_cursorIndexOfScenarioLabel)) {
              _tmpScenarioLabel = null;
            } else {
              _tmpScenarioLabel = _cursor.getString(_cursorIndexOfScenarioLabel);
            }
            final String _tmpModeStr;
            if (_cursor.isNull(_cursorIndexOfModeStr)) {
              _tmpModeStr = null;
            } else {
              _tmpModeStr = _cursor.getString(_cursorIndexOfModeStr);
            }
            final double _tmpTotalMin;
            _tmpTotalMin = _cursor.getDouble(_cursorIndexOfTotalMin);
            final int _tmpTotalFare;
            _tmpTotalFare = _cursor.getInt(_cursorIndexOfTotalFare);
            final int _tmpTransfers;
            _tmpTransfers = _cursor.getInt(_cursorIndexOfTransfers);
            final String _tmpLinesUsed;
            if (_cursor.isNull(_cursorIndexOfLinesUsed)) {
              _tmpLinesUsed = null;
            } else {
              _tmpLinesUsed = _cursor.getString(_cursorIndexOfLinesUsed);
            }
            final String _tmpRouteType;
            if (_cursor.isNull(_cursorIndexOfRouteType)) {
              _tmpRouteType = null;
            } else {
              _tmpRouteType = _cursor.getString(_cursorIndexOfRouteType);
            }
            final long _tmpSavedAt;
            _tmpSavedAt = _cursor.getLong(_cursorIndexOfSavedAt);
            _item = new SavedRouteEntity(_tmpId,_tmpUserId,_tmpOriginLabel,_tmpDestLabel,_tmpScenario,_tmpScenarioLabel,_tmpModeStr,_tmpTotalMin,_tmpTotalFare,_tmpTransfers,_tmpLinesUsed,_tmpRouteType,_tmpSavedAt);
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
  public Object findExact(final long userId, final String origin, final String dest,
      final String scenario, final Continuation<? super SavedRouteEntity> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM saved_routes\n"
            + "        WHERE user_id = ?\n"
            + "          AND origin_label = ?\n"
            + "          AND dest_label   = ?\n"
            + "          AND scenario     = ?\n"
            + "        LIMIT 1\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 4);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    _argIndex = 2;
    if (origin == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, origin);
    }
    _argIndex = 3;
    if (dest == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, dest);
    }
    _argIndex = 4;
    if (scenario == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, scenario);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SavedRouteEntity>() {
      @Override
      @Nullable
      public SavedRouteEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "user_id");
          final int _cursorIndexOfOriginLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "origin_label");
          final int _cursorIndexOfDestLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "dest_label");
          final int _cursorIndexOfScenario = CursorUtil.getColumnIndexOrThrow(_cursor, "scenario");
          final int _cursorIndexOfScenarioLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "scenario_label");
          final int _cursorIndexOfModeStr = CursorUtil.getColumnIndexOrThrow(_cursor, "mode_str");
          final int _cursorIndexOfTotalMin = CursorUtil.getColumnIndexOrThrow(_cursor, "total_min");
          final int _cursorIndexOfTotalFare = CursorUtil.getColumnIndexOrThrow(_cursor, "total_fare");
          final int _cursorIndexOfTransfers = CursorUtil.getColumnIndexOrThrow(_cursor, "transfers");
          final int _cursorIndexOfLinesUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "lines_used");
          final int _cursorIndexOfRouteType = CursorUtil.getColumnIndexOrThrow(_cursor, "route_type");
          final int _cursorIndexOfSavedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "saved_at");
          final SavedRouteEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final String _tmpOriginLabel;
            if (_cursor.isNull(_cursorIndexOfOriginLabel)) {
              _tmpOriginLabel = null;
            } else {
              _tmpOriginLabel = _cursor.getString(_cursorIndexOfOriginLabel);
            }
            final String _tmpDestLabel;
            if (_cursor.isNull(_cursorIndexOfDestLabel)) {
              _tmpDestLabel = null;
            } else {
              _tmpDestLabel = _cursor.getString(_cursorIndexOfDestLabel);
            }
            final String _tmpScenario;
            if (_cursor.isNull(_cursorIndexOfScenario)) {
              _tmpScenario = null;
            } else {
              _tmpScenario = _cursor.getString(_cursorIndexOfScenario);
            }
            final String _tmpScenarioLabel;
            if (_cursor.isNull(_cursorIndexOfScenarioLabel)) {
              _tmpScenarioLabel = null;
            } else {
              _tmpScenarioLabel = _cursor.getString(_cursorIndexOfScenarioLabel);
            }
            final String _tmpModeStr;
            if (_cursor.isNull(_cursorIndexOfModeStr)) {
              _tmpModeStr = null;
            } else {
              _tmpModeStr = _cursor.getString(_cursorIndexOfModeStr);
            }
            final double _tmpTotalMin;
            _tmpTotalMin = _cursor.getDouble(_cursorIndexOfTotalMin);
            final int _tmpTotalFare;
            _tmpTotalFare = _cursor.getInt(_cursorIndexOfTotalFare);
            final int _tmpTransfers;
            _tmpTransfers = _cursor.getInt(_cursorIndexOfTransfers);
            final String _tmpLinesUsed;
            if (_cursor.isNull(_cursorIndexOfLinesUsed)) {
              _tmpLinesUsed = null;
            } else {
              _tmpLinesUsed = _cursor.getString(_cursorIndexOfLinesUsed);
            }
            final String _tmpRouteType;
            if (_cursor.isNull(_cursorIndexOfRouteType)) {
              _tmpRouteType = null;
            } else {
              _tmpRouteType = _cursor.getString(_cursorIndexOfRouteType);
            }
            final long _tmpSavedAt;
            _tmpSavedAt = _cursor.getLong(_cursorIndexOfSavedAt);
            _result = new SavedRouteEntity(_tmpId,_tmpUserId,_tmpOriginLabel,_tmpDestLabel,_tmpScenario,_tmpScenarioLabel,_tmpModeStr,_tmpTotalMin,_tmpTotalFare,_tmpTransfers,_tmpLinesUsed,_tmpRouteType,_tmpSavedAt);
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
