/*
 * Created by dengshiwei on 2021/04/07.
 * Copyright 2015－2022 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.sdk.data.adapter;

import android.net.Uri;

public class DbParams {
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
    private static DbParams instance;
    private final Uri mUri;

    private DbParams(String packageName) {
        mUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_EVENTS);
    }

    public static DbParams getInstance(String packageName) {
        if (instance == null) {
            instance = new DbParams(packageName);
        }
        return instance;
    }

    public static DbParams getInstance() {
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
