/*
 * Created by wangzhuozhou on 2015/08/01.
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
package com.sensorsdata.analytics.android.sdk;


import android.app.Activity;
import android.view.View;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SensorsDataAPIEmptyImplementation extends SensorsDataAPI {
    SensorsDataAPIEmptyImplementation() {

    }

    @Override
    public String getServerUrl() {
        return null;
    }

    @Override
    public void setServerUrl(String serverUrl) {

    }

    @Override
    public void enableLog(boolean enable) {

    }

    @Override
    public boolean isDebugMode() {
        return false;
    }

    @Override
    public long getMaxCacheSize() {
        // 返回默认值
        return 32 * 1024 * 1024;
    }

    @Override
    public void setMaxCacheSize(long maxCacheSize) {

    }

    @Override
    public void setFlushNetworkPolicy(int networkType) {

    }

    @Override
    public int getFlushBulkSize() {
        return 100;
    }

    @Override
    public void setFlushBulkSize(int flushBulkSize) {

    }

    @Override
    public void track(String eventName, JSONObject properties) {

    }

    @Override
    public void track(String eventName) {

    }

    public void trackInternal(final String eventName, final JSONObject properties) {

    }

    @Override
    public void trackViewScreen(String url, JSONObject properties) {

    }

    @Override
    public void trackViewScreen(Activity activity) {

    }

    @Override
    public void trackViewScreen(Object fragment) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void flushSync() {

    }


    @Override
    public void setTrackEventCallBack(SensorsDataTrackEventCallBack trackEventCallBack) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public void setDebugMode(DebugMode debugMode) {

    }

    @Override
    public void enableNetworkRequest(boolean isRequest) {

    }

    @Override
    int getFlushNetworkPolicy() {
        return 0;
    }

    @Override
    public boolean isNetworkRequestEnable() {
        return false;
    }

}