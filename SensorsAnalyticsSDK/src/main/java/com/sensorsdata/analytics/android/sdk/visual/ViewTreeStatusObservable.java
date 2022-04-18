/*
 * Created by zhangxiangwei on 2021/05/25.
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

package com.sensorsdata.analytics.android.sdk.visual;

import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver.OnGlobalFocusChangeListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.ViewUtil;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.util.Dispatcher;
import java.util.HashMap;


public class ViewTreeStatusObservable implements OnGlobalLayoutListener, OnScrollChangedListener, OnGlobalFocusChangeListener {
    private static final String TAG = "SA.ViewTreeStatusObservable";
    public static volatile ViewTreeStatusObservable viewTreeStatusObservable;
    private final Runnable mTraverseRunnable = new TraverseRunnable();
    private SparseArray<ViewNode> mViewNodesWithHashCode = new SparseArray<>();
    private HashMap<String, ViewNode> mViewNodesHashMap = new HashMap<>();
    private HashMap<String, ViewNode> mWebViewHashMap = new HashMap<>();

    public static ViewTreeStatusObservable getInstance() {
        if (viewTreeStatusObservable == null) {
            synchronized (ViewTreeStatusObservable.class) {
                if (viewTreeStatusObservable == null) {
                    viewTreeStatusObservable = new ViewTreeStatusObservable();
                }
            }
        }
        return viewTreeStatusObservable;
    }

    class TraverseRunnable implements Runnable {
        TraverseRunnable() {
        }

        public void run() {
            long startTime = System.currentTimeMillis();
            SALog.i(TAG, "start traverse...");
            traverseNode();
            SALog.i(TAG, "stop traverse...:" + (System.currentTimeMillis() - startTime));
        }
    }

    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        SALog.i(TAG, "onGlobalFocusChanged");
        traverse();
    }

    public void onGlobalLayout() {
        SALog.i(TAG, "onGlobalLayout");
        traverse();
    }

    public void onScrollChanged() {
        SALog.i(TAG, "onScrollChanged");
        traverse();
    }

    public void traverse() {
        try {
            Dispatcher.getInstance().postDelayed(mTraverseRunnable, 100);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public ViewNode getViewNode(View view) {
        ViewNode viewNode = null;
        try {
            viewNode = mViewNodesWithHashCode.get(view.hashCode());
            if (viewNode == null) {
                viewNode = getViewPathAndPosition(view);
                if (viewNode != null) {
                    mViewNodesWithHashCode.put(view.hashCode(), viewNode);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return viewNode;
    }

    private void traverseNode() {
        mViewNodesHashMap.clear();
        mViewNodesWithHashCode.clear();
        mWebViewHashMap.clear();
        traverseNode(null);
    }

    private void traverseNode(View rootView) {
        try {
            SparseArray<ViewNode> tempSparseArray = new SparseArray<>();
            HashMap<String, ViewNode> tempHashMap = new HashMap<>();
            HashMap<String, ViewNode> tempWebViewHashMap = new HashMap<>();
            // 主动遍历
            if (rootView != null) {
                traverseNode(rootView, tempSparseArray, tempHashMap, tempWebViewHashMap);
            }
            mViewNodesHashMap = tempHashMap;
            mViewNodesWithHashCode = tempSparseArray;
            mWebViewHashMap = tempWebViewHashMap;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void traverseNode(final View view, final SparseArray<ViewNode> sparseArray, final HashMap<String, ViewNode> hashMap, final HashMap<String, ViewNode> webViewHashMap) {
        try {
            if (view == null) {
                return;
            }
            ViewNode viewNode = getCacheViewPathAndPosition(view, true);
            if (viewNode != null) {
                // 缓存 ViewNode,用于获取 $element_path
                sparseArray.put(view.hashCode(), viewNode);
            }
            if (view instanceof ViewGroup) {
                final ViewGroup group = (ViewGroup) view;
                final int childCount = group.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    final View child = group.getChildAt(i);
                    if (child != null) {
                        traverseNode(child, sparseArray, hashMap, webViewHashMap);
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }


    /**
     * 获取 view 控件的 ViewNode 信息
     *
     * @param clickView 需要获取 ViewNode 信息的 View 对象
     * @return 返回 View 对象的 ViewNode 信息
     */
    public ViewNode getViewPathAndPosition(View clickView) {
        return getCacheViewPathAndPosition(clickView, false);
    }

    /**
     * 对 view 进行遍历的过程中进行父控件的缓存读取
     *
     * @param clickView 需要获取 ViewNode 信息的 View 对象
     * @param fromVisual 是否主动获取
     * @return 当前 clickView 的 ViewNode 节点信息
     */
    private ViewNode getCacheViewPathAndPosition(View clickView, boolean fromVisual) {
        ViewNode currentNode = mViewNodesWithHashCode.get(clickView.hashCode());
        if (currentNode != null) {
            return currentNode;
        }
        ViewParent viewParent;
        View parent_view = null;
        viewParent = clickView.getParent();
        if (viewParent instanceof ViewGroup) {
            parent_view = (View) viewParent;
        }
        if (parent_view == null) {
            currentNode = ViewUtil.getViewPathAndPosition(clickView, fromVisual);
        } else {
            StringBuilder opx = new StringBuilder();
            StringBuilder px = new StringBuilder();
            ViewNode parentNode = mViewNodesWithHashCode.get(parent_view.hashCode());
            if (parentNode == null) {
                parentNode = ViewUtil.getViewPathAndPosition(parent_view, fromVisual);
                mViewNodesWithHashCode.put(parent_view.hashCode(), parentNode);
            }
            opx.append(parentNode.getViewOriginalPath());
            px.append(parentNode.getViewPath());
            final int viewPosition = ((ViewGroup) parent_view).indexOfChild(clickView);
            currentNode = ViewUtil.getViewNode(clickView, viewPosition, fromVisual);
            String listPosition = parentNode.getViewPosition();
            // 这个地方由于 viewPosition 当前控件是列表的子控件的时候，表示当前控件位于父控件的位置；当前控件是非列表的子控件的时候，表示上一个列表的位置。因此通过上一个 View 的 listPosition 进行替换[-]没有什么大的问题
            if (!TextUtils.isEmpty(currentNode.getViewPath()) && currentNode.getViewPath().contains("-") && !TextUtils.isEmpty(listPosition)) {
                int replacePosition = px.lastIndexOf("-");
                if (replacePosition != -1) {
                    px.replace(replacePosition, replacePosition + 1, String.valueOf(listPosition));
                }
            }
            opx.append(currentNode.getViewOriginalPath());
            px.append(currentNode.getViewPath());
            currentNode = new ViewNode(clickView, currentNode.getViewPosition(), opx.toString(), px.toString(), currentNode.getViewContent());
        }
        mViewNodesWithHashCode.put(clickView.hashCode(), currentNode);
        return currentNode;
    }
}