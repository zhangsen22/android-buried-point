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
    public static final String TABLE_EVENTS = "events";
    public static final String TABLE_CHANNEL_PERSISTENT = "t_channel";
    public static final int DB_OUT_OF_MEMORY_ERROR = -2;
    public static final String KEY_CHANNEL_EVENT_NAME = "event_name";
    public static final String KEY_CHANNEL_RESULT = "result";
    /* 数据库名称 */
    public static final String DATABASE_NAME = "sensorsdata";
    /* 数据库版本号 */
    public static final int DATABASE_VERSION = 5;
    public static final String TABLE_ACTIVITY_START_COUNT = "activity_started_count";
    public static final String TABLE_FIRST_PROCESS_START = "first_process_start";
    public static final String TABLE_DATA_COLLECT = "data_collect";
    public static final String TABLE_DATA_ENABLE_SDK = "enable_SDK";
    public static final String TABLE_DATA_DISABLE_SDK = "disable_SDK";
    public static final String PUSH_ID_KEY = "push_key";
    public static final String PUSH_ID_VALUE = "push_value";
    public static final String REMOVE_SP_KEY = "remove_key";
    /* Event 表字段 */
    public static final String KEY_DATA = "data";
    public static final String KEY_CREATED_AT = "created_at";
    /* 数据库状态 */
    static final int DB_UPDATE_ERROR = -1;
    static final String VALUE = "value";
    public static final String GZIP_DATA_EVENT = "1";
    public static final String GZIP_DATA_ENCRYPT = "9";
    /* 删除所有数据 */
    static final String DB_DELETE_ALL = "DB_DELETE_ALL";
    private static DbParams instance;
    private final Uri mUri, mActivityStartCountUri, mDataCollectUri, mChannelPersistentUri, mSubProcessUri,
            mEnableSDKUri, mDisableSDKUri, mRemoteConfigUri, mUserIdentities, mLoginIdKeyUri, mPushIdUri;

    public interface PersistentName {
        String APP_END_DATA = "app_end_data";
        String SUB_PROCESS_FLUSH_DATA = "sub_process_flush_data";
        String DISTINCT_ID = "events_distinct_id";
        String FIRST_DAY = "first_day";
        String FIRST_START = "first_start";
        String FIRST_INSTALL = "first_track_installation";
        String REQUEST_DEFERRER_DEEPLINK = "request_deferrer_deeplink";
        String FIRST_INSTALL_CALLBACK = "first_track_installation_with_callback";
        String REMOTE_CONFIG = "sensorsdata_sdk_configuration";
        String SUPER_PROPERTIES = "super_properties";
        String VISUAL_PROPERTIES = "visual_properties";
        String PERSISTENT_USER_ID = "user_ids";
        String PERSISTENT_LOGIN_ID_KEY = "login_id_key";
    }

    private DbParams(String packageName) {
        mUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_EVENTS);
        mActivityStartCountUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_ACTIVITY_START_COUNT);
        mLoginIdKeyUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + PersistentName.PERSISTENT_LOGIN_ID_KEY);
        mChannelPersistentUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_CHANNEL_PERSISTENT);
        mSubProcessUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + PersistentName.SUB_PROCESS_FLUSH_DATA);
        mDataCollectUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_DATA_COLLECT);
        mEnableSDKUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_DATA_ENABLE_SDK);
        mDisableSDKUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_DATA_DISABLE_SDK);
        mRemoteConfigUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + PersistentName.REMOTE_CONFIG);
        mUserIdentities = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + PersistentName.PERSISTENT_USER_ID);
        mPushIdUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + PUSH_ID_KEY);
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

    /**
     * 获取 AppStart Uri
     *
     * @return Uri
     */
    public Uri getActivityStartCountUri() {
        return mActivityStartCountUri;
    }

    /**
     * 获取 Channel 持久化 Uri
     *
     * @return Uri
     */
    public Uri getChannelPersistentUri() {
        return mChannelPersistentUri;
    }

    /**
     * 多进程上报数据标记位 Uri
     *
     * @return Uri
     */
    Uri getSubProcessUri() {
        return mSubProcessUri;
    }

    /**
     * 开启数据采集 Uri
     *
     * @return Uri
     */
    public Uri getDataCollectUri() {
        return mDataCollectUri;
    }

    public Uri getDisableSDKUri() {
        return mDisableSDKUri;
    }

    public Uri getEnableSDKUri() {
        return mEnableSDKUri;
    }

    public Uri getRemoteConfigUri() {
        return mRemoteConfigUri;
    }

    /**
     * 用户标识 Uri
     *
     * @return Uri
     */
    public Uri getUserIdentities() {
        return mUserIdentities;
    }

    /**
     * 获取 ID-Mapping 3.0 自定义的 LoginIdKey 的 Uri
     *
     * @return Uri
     */
    public Uri getLoginIdKeyUri() {
        return mLoginIdKeyUri;
    }

    public Uri getPushIdUri() {
        return mPushIdUri;
    }
}
