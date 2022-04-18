/*
 * Created by yuejiangzhong on 2022/01/20.
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

package com.sensorsdata.analytics.android.sdk.monitor;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;

import org.json.JSONException;
import org.json.JSONObject;


public class TrackMonitor {


    private TrackMonitor() {

    }

    private static class SingletonHolder {
        private static final TrackMonitor INSTANCE = new TrackMonitor();
    }

    private void call(String function, JSONObject jsonObject) {
        if (TextUtils.isEmpty(function) ) {
            return;
        }
    }

    public static TrackMonitor getInstance() {
        return SingletonHolder.INSTANCE;
    }


    public void callTrack(JSONObject eventObject) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("eventJSON", eventObject);
            call("trackEvent",jsonObject);
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    public void callEnableDataCollect() {
        call("enableDataCollect", null);
    }
}
