package com.sensorsdata.analytics.android.sdk.data;

import android.net.Uri;

public class PFDbParams {
    /* 数据库中的表名 */
    public static final String TABLE_EVENTS = "analytics_events";
    public static final int DB_OUT_OF_MEMORY_ERROR = -2;
    /* 数据库名称 */
    public static final String DATABASE_NAME = "pinefiele_db";
    /* 数据库版本号 */
    public static final int DATABASE_VERSION = 1;
    /* Event 表字段 */
    public static final String KEY_DATA = "data";
    public static final String KEY_CREATED_AT = "created_at";
    /* 数据库状态 */
    public static final String GZIP_DATA_EVENT = "1";
    /* 删除所有数据 */
    static final String DB_DELETE_ALL = "DB_DELETE_ALL";
    private static PFDbParams instance;
    private final Uri mUri;

    private PFDbParams(String packageName) {
        mUri = Uri.parse("content://" + packageName + ".PFDataContentProvider/" + TABLE_EVENTS);
    }

    public static PFDbParams getInstance(String packageName) {
        if (instance == null) {
            instance = new PFDbParams(packageName);
        }
        return instance;
    }

    public static PFDbParams getInstance() {
        if (instance == null) {
            throw new IllegalStateException("The static method getInstance(String packageName) should be called before calling getInstance()");
        }
        return instance;
    }

    /**
     * 获取 Event Uri
     *
     * @return Uri
     */
    Uri getEventUri() {
        return mUri;
    }
}
