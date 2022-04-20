package com.sensorsdata.analytics.android.sdk.data;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import com.sensorsdata.analytics.android.sdk.SALog;

class PFProviderHelper {
    private SQLiteOpenHelper mDbHelper;
    private Context mContext;
    private boolean isDbWritable = true;

    public PFProviderHelper(Context context, SQLiteOpenHelper dbHelper) {
        try {
            this.mDbHelper = dbHelper;
            this.mContext = context;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 构建 Uri 类型
     *
     * @param uriMatcher UriMatcher
     * @param authority authority
     */
    public void appendUri(UriMatcher uriMatcher, String authority) {
        try {
            uriMatcher.addURI(authority, PFDbParams.TABLE_EVENTS, URI_CODE.EVENTS);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 插入 Event 埋点数据
     *
     * @param uri Uri
     * @param values 数据
     * @return Uri
     */
    public Uri insertEvent(Uri uri, ContentValues values) {
        try {
            SQLiteDatabase database = getWritableDatabase();
            if (database == null || !values.containsKey(PFDbParams.KEY_DATA)
                    || !values.containsKey(PFDbParams.KEY_CREATED_AT)) {
                return uri;
            }
            long d = database.insert(PFDbParams.TABLE_EVENTS, "_id", values);
            return ContentUris.withAppendedId(uri, d);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return uri;
    }

    /**
     * 删除埋点数据
     *
     * @param selection 条件
     * @param selectionArgs 参数
     * @return 受影响数
     */
    public int deleteEvents(String selection, String[] selectionArgs) {
        if (!isDbWritable) {
            return 0;
        }
        try {
            SQLiteDatabase database = getWritableDatabase();
            if (database != null) {
                return database.delete(PFDbParams.TABLE_EVENTS, selection, selectionArgs);
            }
        } catch (SQLiteException e) {
            isDbWritable = false;
            SALog.printStackTrace(e);
        }
        return 0;
    }

    /**
     * 查询数据
     *
     * @param tableName 表名
     * @param projection 列明
     * @param selection 筛选条件
     * @param selectionArgs 筛选参数
     * @param sortOrder 排序
     * @return Cursor
     */
    public Cursor queryByTable(String tableName, String[] projection, String selection, String[]
            selectionArgs, String sortOrder) {
        if (!isDbWritable) {
            return null;
        }
        Cursor cursor = null;
        try {
            SQLiteDatabase liteDatabase = getWritableDatabase();
            if (liteDatabase != null) {
                cursor = liteDatabase.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
            }
        } catch (SQLiteException e) {
            isDbWritable = false;
            SALog.printStackTrace(e);
        }
        return cursor;
    }

    /**
     * 获取数据库
     *
     * @return SQLiteDatabase
     */
    private SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase database = null;
        try {
            if (!isDBExist()) {
                mDbHelper.close();
                isDbWritable = true;
            }
            database = mDbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            SALog.printStackTrace(e);
            isDbWritable = false;
        }
        return database;
    }

    private boolean isDBExist() {
        return mContext.getDatabasePath(PFDbParams.DATABASE_NAME).exists();
    }

    /**
     * URI 对应的 Code
     */
    public interface URI_CODE {
        int EVENTS = 1;
    }
}
