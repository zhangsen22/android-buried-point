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

package com.sensorsdata.analytics.android.sdk.data;

import android.content.Context;
import com.sensorsdata.analytics.android.sdk.SALog;

import org.json.JSONObject;

public class DbAdapter {
    private static DbAdapter instance;
    private final DbParams mDbParams;
    private DataOperation mTrackEventOperation;
    private DbAdapter(Context context, String packageName) {
        mDbParams = DbParams.getInstance(packageName);
        mTrackEventOperation = new EventDataOperation(context.getApplicationContext());
    }

    public static DbAdapter getInstance(Context context, String packageName) {
        if (instance == null) {
            instance = new DbAdapter(context, packageName);
        }
        return instance;
    }

    public static DbAdapter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("The static method getInstance(Context context, String packageName) should be called before calling getInstance()");
        }
        return instance;
    }

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     *
     * @param j the JSON to record
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    public int addJSON(JSONObject j) {
        int code = mTrackEventOperation.insertData(mDbParams.getEventUri(), j);
        if (code == 0) {
            return mTrackEventOperation.queryDataCount(mDbParams.getEventUri());
        }
        return code;
    }

    /**
     * Removes all events from table
     */
    public void deleteAllEvents() {
        mTrackEventOperation.deleteData(mDbParams.getEventUri(), DbParams.DB_DELETE_ALL);
    }

    /**
     * Removes events with an _id &lt;= last_id from table
     *
     * @param last_id the last id to delete
     * @return the number of rows in the table
     */
    public int cleanupEvents(String last_id) {
        mTrackEventOperation.deleteData(mDbParams.getEventUri(), last_id);
        return mTrackEventOperation.queryDataCount(mDbParams.getEventUri());
    }

    /**
     * 从 Event 表中读取上报数据
     *
     * @param tableName 表名
     * @param limit 条数限制
     * @return 数据
     */
    public String[] generateDataString(String tableName, int limit) {
        try {
            return mTrackEventOperation.queryData(mDbParams.getEventUri(), limit);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }
}