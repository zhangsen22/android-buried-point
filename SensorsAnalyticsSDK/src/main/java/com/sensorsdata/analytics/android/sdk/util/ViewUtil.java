/*
 * Created by zhangxiangwei on 2019/12/09.
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

import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.sensorsdata.analytics.android.sdk.visual.snap.SnapCache;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;

public class ViewUtil {

    private static boolean sHaveCustomRecyclerView = false;
    private static boolean sHaveRecyclerView = haveRecyclerView();
    private static Method sRecyclerViewGetChildAdapterPositionMethod;
    private static Class<?> sRecyclerViewClass;
    private static SparseArray<String> sViewCache;

    private static boolean instanceOfSupportSwipeRefreshLayout(Object view) {
        return ReflectUtil.isInstance(view, "android.support.v4.widget.SwipeRefreshLayout", "androidx.swiperefreshlayout.widget.SwipeRefreshLayout");
    }

    static boolean instanceOfBottomNavigationItemView(Object view) {
        return ReflectUtil.isInstance(view, "com.google.android.material.bottomnavigation.BottomNavigationItemView", "android.support.design.internal.NavigationMenuItemView");
    }

    private static boolean instanceOfNavigationView(Object view) {
        return ReflectUtil.isInstance(view, "android.support.design.widget.NavigationView", "com.google.android.material.navigation.NavigationView");
    }

    private static boolean instanceOfSupportViewPager(Object view) {
        return ReflectUtil.isInstance(view, "android.support.v4.view.ViewPager");
    }

    private static boolean instanceOfAndroidXViewPager(Object view) {
        return ReflectUtil.isInstance(view, "androidx.viewpager.widget.ViewPager");
    }

    public static boolean instanceOfRecyclerView(Object view) {
        boolean result = ReflectUtil.isInstance(view, "android.support.v7.widget.RecyclerView", "androidx.recyclerview.widget.RecyclerView");
        if (!result) {
            result = sHaveCustomRecyclerView && view != null && sRecyclerViewClass != null && sRecyclerViewClass.isAssignableFrom(view.getClass());
        }
        return result;
    }

    private static Object instanceOfTabView(View tabView) {
        try {
            Class<?> currentTabViewClass = ReflectUtil.getCurrentClass(new String[]{"android.support.design.widget.TabLayout$TabView", "com.google.android.material.tabs.TabLayout$TabView"});
            if (currentTabViewClass != null && currentTabViewClass.isAssignableFrom(tabView.getClass())) {
                return ReflectUtil.findField(currentTabViewClass, tabView, "mTab", "tab");
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * 获取 class name 和 刷新自定义 RecyclerView 状态
     *
     * @param clazz 需要获取名字的类
     * @return 获取的类名字
     */
    private static String getCanonicalAndCheckCustomView(Class<?> clazz) {
        String name = SnapCache.getInstance().getCanonicalName(clazz);
        if (name != null) {
            checkCustomRecyclerView(clazz, name);
        }
        return name;
    }

    /**
     * view 是否为 Fragment 中的顶层 View
     */
    private static Object instanceOfFragmentRootView(View parentView, View childView) {
        Object parentFragment = AopUtil.getFragmentFromView(parentView);
        Object childFragment = AopUtil.getFragmentFromView(childView);
        if (parentFragment == null && childFragment != null) {
            return childFragment;
        }
        return null;
    }

    /**
     * position RecyclerView item
     */
    private static int getChildAdapterPositionInRecyclerView(View childView, ViewGroup parentView) {
        if (instanceOfRecyclerView(parentView)) {
            try {
                sRecyclerViewGetChildAdapterPositionMethod = parentView.getClass().getMethod("getChildAdapterPosition", new Class[]{View.class});
            } catch (NoSuchMethodException e) {
                //ignored
            }
            if (sRecyclerViewGetChildAdapterPositionMethod == null) {
                try {
                    sRecyclerViewGetChildAdapterPositionMethod = parentView.getClass().getMethod("getChildPosition", new Class[]{View.class});
                } catch (NoSuchMethodException e2) {
                    //ignored
                }
            }
            try {
                if (sRecyclerViewGetChildAdapterPositionMethod != null) {
                    Object object = sRecyclerViewGetChildAdapterPositionMethod.invoke(parentView, childView);
                    if (object != null) {
                        return (Integer) object;
                    }
                }
            } catch (IllegalAccessException e) {
                //ignored
            } catch (InvocationTargetException e) {
                //ignored
            }
        } else if (sHaveCustomRecyclerView) {
            return invokeCRVGetChildAdapterPositionMethod(parentView, childView);
        }
        return -1;
    }

    private static int getCurrentItem(View view) {
        try {
            Method method = view.getClass().getMethod("getCurrentItem");
            Object object = method.invoke(view);
            if (object != null) {
                return (Integer) object;
            }
        } catch (IllegalAccessException e) {
            //ignored
        } catch (InvocationTargetException e2) {
            //ignored
        } catch (NoSuchMethodException e) {
            //ignored
        }
        return -1;
    }

    static Object getItemData(View view) {
        try {
            Method method = view.getClass().getMethod("getItemData");
            return method.invoke(view);
        } catch (IllegalAccessException e) {
            //ignored
        } catch (InvocationTargetException e2) {
            //ignored
        } catch (NoSuchMethodException e) {
            //ignored
        }
        return null;
    }

    private static boolean haveRecyclerView() {
        try {
            Class.forName("android.support.v7.widget.RecyclerView");
            return true;
        } catch (ClassNotFoundException th) {
            try {
                Class.forName("androidx.recyclerview.widget.RecyclerView");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    private static void checkCustomRecyclerView(Class<?> viewClass, String viewName) {
        if (!sHaveRecyclerView && !sHaveCustomRecyclerView && viewName != null && viewName.contains("RecyclerView")) {
            try {
                if (findRecyclerInSuper(viewClass) != null && sRecyclerViewGetChildAdapterPositionMethod != null) {
                    sRecyclerViewClass = viewClass;
                    sHaveCustomRecyclerView = true;
                }
            } catch (Exception e) {
                //ignored
            }
        }
    }

    private static Class<?> findRecyclerInSuper(Class<?> viewClass) {
        while (viewClass != null && !viewClass.equals(ViewGroup.class)) {
            try {
                sRecyclerViewGetChildAdapterPositionMethod = viewClass.getMethod("getChildAdapterPosition", new Class[]{View.class});
            } catch (NoSuchMethodException e) {
                //ignored
            }
            if (sRecyclerViewGetChildAdapterPositionMethod == null) {
                try {
                    sRecyclerViewGetChildAdapterPositionMethod = viewClass.getMethod("getChildPosition", new Class[]{View.class});
                } catch (NoSuchMethodException e2) {
                    //ignored
                }
            }
            if (sRecyclerViewGetChildAdapterPositionMethod != null) {
                return viewClass;
            }
            viewClass = viewClass.getSuperclass();
        }
        return null;
    }

    private static int invokeCRVGetChildAdapterPositionMethod(View customRecyclerView, View childView) {
        try {
            if (customRecyclerView.getClass() == sRecyclerViewClass) {
                return (Integer) sRecyclerViewGetChildAdapterPositionMethod.invoke(customRecyclerView, new Object[]{childView});
            }
        } catch (IllegalAccessException e) {
            //ignored
        } catch (InvocationTargetException e2) {
            //ignored
        }
        return -1;
    }

    static boolean isTrackEvent(View view, boolean isFromUser) {
        if (view instanceof CheckBox) {
            if (!isFromUser) {
                return false;
            }
        } else if (view instanceof RadioButton) {
            if (!isFromUser) {
                return false;
            }
        } else if (view instanceof ToggleButton) {
            if (!isFromUser) {
                return false;
            }
        } else if (view instanceof CompoundButton) {
            if (!isFromUser) {
                return false;
            }
        }
        if (view instanceof RatingBar) {
            if (!isFromUser) {
                return false;
            }
        }
        return true;
    }
}