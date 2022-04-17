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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;

import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.KeyboardViewUtil;
import com.sensorsdata.analytics.android.sdk.util.ReflectUtil;
import com.sensorsdata.analytics.android.sdk.util.SAFragmentUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.ThreadUtils;
import com.sensorsdata.analytics.android.sdk.util.ViewUtil;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.util.VisualUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

@SuppressWarnings("unused")
public class SensorsDataAutoTrackHelper {
    private static final String TAG = "SensorsDataAutoTrackHelper";
    private static HashMap<Integer, Long> eventTimestamp = new HashMap<>();

    private static boolean isDeBounceTrack(Object object) {
        long currentOnClickTimestamp = SystemClock.elapsedRealtime();
        Object targetObject = eventTimestamp.get(object.hashCode());
        if (targetObject != null) {
            long lastOnClickTimestamp = (long) targetObject;
            if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                return true;
            }
        }
        eventTimestamp.put(object.hashCode(), currentOnClickTimestamp);
        return false;
    }

    public static void trackViewOnClick(View view) {
        if (view == null) {
            return;
        }
        trackViewOnClick(view, view.isPressed());
    }

    public static void trackViewOnClick(View view, boolean isFromUser) {
        try {
            if (view == null) {
                return;
            }
            //关闭 AutoTrack
            if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }
            //$AppClick 被过滤
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // 获取 view 所在的 fragment
            Object fragment = AopUtil.getFragmentFromView(view, activity);

            // fragment 忽略
            if (fragment != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            if (SensorsDataUtils.isDoubleClick(view)) {
                return;
            }

            if (KeyboardViewUtil.isKeyboardView(view)) {
                return;
            }

            JSONObject properties = new JSONObject();

            if (AopUtil.injectClickInfo(view, properties, isFromUser)) {
                SensorsDataAPI.sharedInstance().trackAutoEvent(AopConstants.APP_CLICK_EVENT_NAME, properties, AopUtil.addViewPathProperties(activity, view, properties));
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void track(String eventName, String properties) {
        try {
            if (TextUtils.isEmpty(eventName)) {
                return;
            }
            JSONObject pro = null;
            if (!TextUtils.isEmpty(properties)) {
                try {
                    pro = new JSONObject(properties);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
            SensorsDataAPI.sharedInstance().trackInternal(eventName, pro);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private static boolean isSupportJellyBean() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 && !SensorsDataAPI.getConfigOptions().isWebViewSupportJellyBean) {
            SALog.d(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
            return false;
        }
        return true;
    }

}