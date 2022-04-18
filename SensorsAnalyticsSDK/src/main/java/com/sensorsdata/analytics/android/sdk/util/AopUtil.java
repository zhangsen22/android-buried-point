/*
 * Created by wangzhuozhou on 2016/12/2.
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

package com.sensorsdata.analytics.android.sdk.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.visual.ViewTreeStatusObservable;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.snap.SnapCache;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class AopUtil {
    private static final String TAG = "SA.AopUtil";

    // 采集 viewType 忽略以下包内 view 直接返回对应的基础控件 viewType
    private static List<String> sOSViewPackage = new LinkedList<String>() {{
        add("android##widget");
        add("android##support##v7##widget");
        add("android##support##design##widget");
        add("android##support##text##emoji##widget");
        add("androidx##appcompat##widget");
        add("androidx##emoji##widget");
        add("androidx##cardview##widget");
        add("com##google##android##material");
    }};

    public static String traverseView(StringBuilder stringBuilder, ViewGroup root) {
        try {
            if (stringBuilder == null) {
                stringBuilder = new StringBuilder();
            }

            if (root == null) {
                return stringBuilder.toString();
            }

            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);
                if (child == null) {
                    continue;
                }
                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }

                if (child instanceof ViewGroup) {
                    traverseView(stringBuilder, (ViewGroup) child);
                } else {

                    String viewText = getViewText(child);
                    if (!TextUtils.isEmpty(viewText)) {
                        stringBuilder.append(viewText);
                        stringBuilder.append("-");
                    }
                }
            }
            return stringBuilder.toString();
        } catch (Throwable e) {
            SALog.d(TAG, e.getMessage());
            return stringBuilder != null ? stringBuilder.toString() : "";
        }
    }

    public static String getViewText(View child) {
        if (child == null || child instanceof EditText) {
            return "";
        }

        String text = SnapCache.getInstance().getViewText(child);
        if (text != null) {
            return text;
        }

        try {
            Class<?> switchCompatClass = null;
            try {
                switchCompatClass = ReflectUtil.getClassByName("android.support.v7.widget.SwitchCompat");
            } catch (Exception e) {
                //ignored
            }

            if (switchCompatClass == null) {
                try {
                    switchCompatClass = ReflectUtil.getClassByName("androidx.appcompat.widget.SwitchCompat");
                } catch (Exception e) {
                    //ignored
                }
            }

            CharSequence viewText = null;

            if (child instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) child;
                viewText = checkBox.getText();
            } else if (switchCompatClass != null && switchCompatClass.isInstance(child)) {
                CompoundButton switchCompat = (CompoundButton) child;
                Method method;
                if (switchCompat.isChecked()) {
                    method = child.getClass().getMethod("getTextOn");
                } else {
                    method = child.getClass().getMethod("getTextOff");
                }
                viewText = (String) method.invoke(child);
            } else if (child instanceof RadioButton) {
                RadioButton radioButton = (RadioButton) child;
                viewText = radioButton.getText();
            } else if (child instanceof ToggleButton) {
                ToggleButton toggleButton = (ToggleButton) child;
                boolean isChecked = toggleButton.isChecked();
                if (isChecked) {
                    viewText = toggleButton.getTextOn();
                } else {
                    viewText = toggleButton.getTextOff();
                }
            } else if (child instanceof Button) {
                Button button = (Button) child;
                viewText = button.getText();
            } else if (child instanceof CheckedTextView) {
                CheckedTextView textView = (CheckedTextView) child;
                viewText = textView.getText();
            } else if (child instanceof TextView) {
                TextView textView = (TextView) child;
                Object object = ReflectUtil.findField(new String[]{"androidx.appcompat.widget.AppCompatTextView"}, textView, "mPrecomputedTextFuture");
                if (object == null) {
                    viewText = textView.getText();
                }
            } else if (child instanceof ImageView) {
                ImageView imageView = (ImageView) child;
                if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                    viewText = imageView.getContentDescription().toString();
                }
            } else {
                viewText = child.getContentDescription();
            }


            if ((viewText == null || viewText.equals("")) && child instanceof TextView) {
                viewText = ((TextView) child).getHint();
            }
            if (viewText != null) {
                text = viewText.toString();
                SnapCache.getInstance().setViewText(child, text);
                return text;
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return "";
    }

    public static Activity getActivityFromContext(Context context, View view) {
        Activity activity = null;
        try {
            if (context != null) {
                if (context instanceof Activity) {
                    activity = (Activity) context;
                } else if (context instanceof ContextWrapper) {
                    while (!(context instanceof Activity) && context instanceof ContextWrapper) {
                        context = ((ContextWrapper) context).getBaseContext();
                    }
                    if (context instanceof Activity) {
                        activity = (Activity) context;
                    }
                }

            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return activity;
    }

    /**
     * 尝试读取页面 title
     *
     * @param properties JSONObject
     * @param fragment Fragment
     * @param activity Activity
     */
    public static void getScreenNameAndTitleFromFragment(JSONObject properties, Object fragment, Activity activity) {
        try {
            String screenName = null;
            String title = null;
            boolean isTitleNull = TextUtils.isEmpty(title);
            boolean isScreenNameNull = TextUtils.isEmpty(screenName);
            isTitleNull = TextUtils.isEmpty(title);
            if (isTitleNull || isScreenNameNull) {
                if (activity == null) {
                    activity = getActivityFromFragment(fragment);
                }
                if (activity != null) {
                    if (isTitleNull) {
                        title = SensorsDataUtils.getActivityTitle(activity);
                    }

                    if (isScreenNameNull) {
                        screenName = fragment.getClass().getCanonicalName();
                        screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), screenName);
                    }
                }
            }

            if (!TextUtils.isEmpty(title)) {
                properties.put(AopConstants.TITLE, title);
            }

            if (TextUtils.isEmpty(screenName)) {
                screenName = fragment.getClass().getCanonicalName();
            }
            properties.put("$screen_name", screenName);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 根据 Fragment 获取对应的 Activity
     *
     * @param fragment，Fragment
     * @return Activity or null
     */
    public static Activity getActivityFromFragment(Object fragment) {
        Activity activity = null;
        if (Build.VERSION.SDK_INT >= 11) {
            try {
                Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                if (getActivityMethod != null) {
                    activity = (Activity) getActivityMethod.invoke(fragment);
                }
            } catch (Exception e) {
                //ignored
            }
        }
        return activity;
    }

    /**
     * 构建 Title 和 Screen 的名称
     *
     * @param activity 页面
     * @return JSONObject
     */
    public static JSONObject buildTitleAndScreenName(Activity activity) {
        JSONObject propertyJSON = new JSONObject();
        try {
            propertyJSON.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
            String activityTitle = AopUtil.getActivityTitle(activity);
            if (!TextUtils.isEmpty(activityTitle)) {
                propertyJSON.put(AopConstants.TITLE, activityTitle);
            }
        } catch (Exception ex) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(ex);
            return new JSONObject();
        }
        return propertyJSON;
    }

    /**
     * 构建 Title 和 Screen 的名称
     *
     * @param activity 页面
     * @return JSONObject
     */
    public static JSONObject buildTitleNoAutoTrackerProperties(Activity activity) {
        JSONObject propertyJSON = new JSONObject();
        try {
            propertyJSON.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
            String activityTitle = AopUtil.getActivityTitle(activity);
            if (!TextUtils.isEmpty(activityTitle)) {
                propertyJSON.put(AopConstants.TITLE, activityTitle);
            }
        } catch (Exception ex) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(ex);
            return new JSONObject();
        }
        return propertyJSON;
    }

    /**
     * 获取 CompoundButton text
     *
     * @param view view
     * @return CompoundButton 显示的内容
     */
    public static String getCompoundButtonText(View view) {
        try {
            CompoundButton switchButton = (CompoundButton) view;
            Method method;
            if (switchButton.isChecked()) {
                method = view.getClass().getMethod("getTextOn");
            } else {
                method = view.getClass().getMethod("getTextOff");
            }
            return (String) method.invoke(view);
        } catch (Exception ex) {
            return "UNKNOWN";
        }
    }

    /**
     * 采集 View 的 $element_type 主要区分继承系统 View 和继承系统 View 的自定义 View
     *
     * @param viewName View.getCanonicalName（）返回的 name
     * @param defaultTypeName 默认的 typeName
     * @return typeName
     */
    public static String getViewType(String viewName, String defaultTypeName) {
        if (TextUtils.isEmpty(viewName)) {
            return defaultTypeName;
        }
        if (TextUtils.isEmpty(defaultTypeName)) {
            return viewName;
        }

        if (isOSViewByPackage(viewName)) {
            return defaultTypeName;
        }

        return viewName;
    }

    /**
     * 通过反射判断类的类型
     *
     * @param view 判断类型的 viewGroup
     * @return viewType
     */
    public static String getViewGroupTypeByReflect(View view) {
        Class<?> compatClass;
        String viewType = SnapCache.getInstance().getCanonicalName(view.getClass());
        compatClass = ReflectUtil.getClassByName("android.support.v7.widget.CardView");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "CardView");
        }
        compatClass = ReflectUtil.getClassByName("androidx.cardview.widget.CardView");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "CardView");
        }
        compatClass = ReflectUtil.getClassByName("android.support.design.widget.NavigationView");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "NavigationView");
        }
        compatClass = ReflectUtil.getClassByName("com.google.android.material.navigation.NavigationView");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "NavigationView");
        }
        return viewType;
    }

    /**
     * 通过反射判断类的类型
     *
     * @param view 判断类型的 view
     * @return viewType
     */
    public static String getViewTypeByReflect(View view) {
        Class<?> compatClass;
        String viewType = SnapCache.getInstance().getCanonicalName(view.getClass());
        compatClass = ReflectUtil.getClassByName("android.widget.Switch");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "Switch");
        }
        compatClass = ReflectUtil.getClassByName("android.support.v7.widget.SwitchCompat");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "SwitchCompat");
        }
        compatClass = ReflectUtil.getClassByName("androidx.appcompat.widget.SwitchCompat");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "SwitchCompat");
        }
        return viewType;
    }

    /**
     * 获取 Activity 的 title
     *
     * @param activity Activity
     * @return Activity 的 title
     */
    private static String getActivityTitle(Activity activity) {
        try {
            if (activity != null) {
                try {
                    String activityTitle = null;
                    if (!TextUtils.isEmpty(activity.getTitle())) {
                        activityTitle = activity.getTitle().toString();
                    }

                    if (Build.VERSION.SDK_INT >= 11) {
                        String toolbarTitle = SensorsDataUtils.getToolbarTitle(activity);
                        if (!TextUtils.isEmpty(toolbarTitle)) {
                            activityTitle = toolbarTitle;
                        }
                    }

                    if (TextUtils.isEmpty(activityTitle)) {
                        PackageManager packageManager = activity.getPackageManager();
                        if (packageManager != null) {
                            ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                            if (!TextUtils.isEmpty(activityInfo.loadLabel(packageManager))) {
                                activityTitle = activityInfo.loadLabel(packageManager).toString();
                            }
                        }
                    }

                    return activityTitle;
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            return null;
        }
    }

    /**
     * 合并 JSONObject
     *
     * @param source JSONObject
     * @param dest JSONObject
     */
    public static void mergeJSONObject(final JSONObject source, JSONObject dest) {
        try {
            Iterator<String> superPropertiesIterator = source.keys();
            while (superPropertiesIterator.hasNext()) {
                String key = superPropertiesIterator.next();
                Object value = source.get(key);
                if (value instanceof Date) {
                    dest.put(key, TimeUtils.formatDate((Date) value));
                } else {
                    dest.put(key, value);
                }
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    /**
     * 判断是否是系统 view
     *
     * @param viewName view 的名称（包含包名）
     * @return 是否是系统 view  true:是  false: 否
     */
    private static boolean isOSViewByPackage(String viewName) {
        if (TextUtils.isEmpty(viewName)) {
            return false;
        }
        String viewNameTemp = viewName.replace(".", "##");
        for (String OSViewPackage : sOSViewPackage) {
            if (viewNameTemp.startsWith(OSViewPackage)) {
                return true;
            }
        }
        return false;
    }

    /**
     * properties 注入点击事件信息
     * {@link SensorsDataAPI#trackViewAppClick(View, JSONObject)}
     *
     * @param view 点击的 view
     * @param properties 事件属性
     * @param isFromUser 是否由用户触发
     * @return isTrackEvent 是否发送事件
     */
    public static boolean injectClickInfo(View view, JSONObject properties, boolean isFromUser) {
        if (view == null || properties == null) {
            return false;
        }
        try {
            if (!ViewUtil.isTrackEvent(view, isFromUser)) {
                return false;
            }
            Context context = view.getContext();
            JSONObject eventJson = new JSONObject();
            Activity activity = AopUtil.getActivityFromContext(context, view);

            ViewNode viewNode = ViewUtil.getViewContentAndType(view);
            String viewText = viewNode.getViewContent();
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                eventJson.put(AopConstants.ELEMENT_CONTENT, viewText);
            }
            //$element_type
            eventJson.put(AopConstants.ELEMENT_TYPE, viewNode.getViewType());

            //2.获取 Activity 页面信息及 ScreenAutoTracker 定义的属性
            if (activity != null) {
                SensorsDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), eventJson);
            }

            //fragmentName
            Object fragment = AopUtil.getFragmentFromView(view, activity);
            if (fragment != null) {
                AopUtil.getScreenNameAndTitleFromFragment(eventJson, fragment, activity);
            }
            //4.事件传入的自定义属性
            JSONUtils.mergeDistinctProperty(eventJson, properties);
            return true;
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * 获取点击 view 的 fragment 对象
     *
     * @param view 点击的 view
     * @return object 这里是 fragment 实例对象
     */
    public static Object getFragmentFromView(View view) {
        return getFragmentFromView(view, null);
    }

    /**
     * 获取点击 view 的 fragment 对象
     *
     * @param view 点击的 view
     * @param activity Activity
     * @return object 这里是 fragment 实例对象
     */
    public static Object getFragmentFromView(View view, Activity activity) {
        try {
            if (view != null) {
                String fragmentName = (String) view.getTag(R.id.sensors_analytics_tag_view_fragment_name);
                if (TextUtils.isEmpty(fragmentName)) {
                    if (activity == null) {
                        //获取所在的 Context
                        Context context = view.getContext();
                        //将 Context 转成 Activity
                        activity = AopUtil.getActivityFromContext(context, view);
                    }
                    if (activity != null) {
                        Window window = activity.getWindow();
                        if (window != null && window.isActive()) {
                            Object tag = window.getDecorView().getRootView().getTag(R.id.sensors_analytics_tag_view_fragment_name);
                            if (tag != null) {
                                fragmentName = traverseParentViewTag(view);
                            }
                        }
                    }
                }
                return FragmentCacheUtil.getFragmentFromCache(fragmentName);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    public static ViewNode addViewPathProperties(Activity activity, View view, JSONObject properties) {
        try {
            if (view == null) {
                return null;
            }
            if (activity == null) {
                return null;
            }
            if (properties == null) {
                properties = new JSONObject();
            }

            ViewNode viewNode = ViewTreeStatusObservable.getInstance().getViewNode(view);
            if (viewNode != null) {
                if (!TextUtils.isEmpty(viewNode.getViewPosition())) {
                    properties.put(AopConstants.ELEMENT_POSITION, viewNode.getViewPosition());
                }
                return viewNode;
            }
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    private static String traverseParentViewTag(View view) {
        try {
            ViewParent parentView = view.getParent();
            String fragmentName = null;
            while (TextUtils.isEmpty(fragmentName) && parentView instanceof View) {
                fragmentName = (String) ((View) parentView).getTag(R.id.sensors_analytics_tag_view_fragment_name);
                parentView = parentView.getParent();
            }
            return fragmentName;
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return "";
    }
}