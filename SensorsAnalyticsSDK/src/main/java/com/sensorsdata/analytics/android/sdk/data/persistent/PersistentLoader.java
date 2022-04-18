/*
 * Created by wangzhuozhou on 2019/02/01.
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

package com.sensorsdata.analytics.android.sdk.data.persistent;

import android.content.Context;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

public class PersistentLoader {

    private static volatile PersistentLoader instance;
    private static Context context;

    private PersistentLoader(Context context) {
        PersistentLoader.context = context.getApplicationContext();
    }

    public static PersistentLoader initLoader(Context context) {
        if (instance == null) {
            instance = new PersistentLoader(context);
        }
        return instance;
    }

    public static PersistentIdentity<?> loadPersistent(String persistentKey) {
        if (instance == null) {
            throw new RuntimeException("you should call 'PersistentLoader.initLoader(Context)' first");
        }
        if (TextUtils.isEmpty(persistentKey)) {
            return null;
        }
        switch (persistentKey) {
            case DbParams.PersistentName.FIRST_DAY:
                return new PersistentFirstDay();
            case DbParams.PersistentName.FIRST_INSTALL_CALLBACK:
                return new PersistentFirstTrackInstallationWithCallback();
            case DbParams.PersistentName.VISUAL_PROPERTIES:
                return new PersistentVisualConfig();
            default:
                return null;
        }
    }
}
