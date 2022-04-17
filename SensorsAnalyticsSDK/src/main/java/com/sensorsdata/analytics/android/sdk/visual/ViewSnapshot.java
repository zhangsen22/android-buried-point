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
package com.sensorsdata.analytics.android.sdk.visual;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.AppStateManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;
import com.sensorsdata.analytics.android.sdk.util.DeviceUtils;
import com.sensorsdata.analytics.android.sdk.util.ViewUtil;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;
import com.sensorsdata.analytics.android.sdk.visual.model.SnapInfo;
import com.sensorsdata.analytics.android.sdk.visual.snap.PropertyDescription;
import com.sensorsdata.analytics.android.sdk.visual.snap.SnapCache;
import com.sensorsdata.analytics.android.sdk.visual.snap.SoftWareCanvas;
import com.sensorsdata.analytics.android.sdk.visual.util.VisualUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ViewSnapshot {

    private static final int MAX_CLASS_NAME_CACHE_SIZE = 255;
    private static final int JS_NOT_INTEGRATED_ALERT_TIME_OUT = 5000;
    private static final String TAG = "SA.ViewSnapshot";
    private final RootViewFinder mRootViewFinder;
    private final Handler mMainThreadHandler;
    private SnapInfo mSnapInfo = new SnapInfo();

    public ViewSnapshot(List<PropertyDescription> properties, Handler mainThreadHandler) {
        mMainThreadHandler = mainThreadHandler;
        mRootViewFinder = new RootViewFinder();
    }

    public SnapInfo snapshots(OutputStream out, StringBuilder lastImageHash) throws IOException {
        final long startSnapshot = System.currentTimeMillis();
        final FutureTask<List<RootViewInfo>> infoFuture =
                new FutureTask<>(mRootViewFinder);
        mMainThreadHandler.post(infoFuture);

        final OutputStream writer = new BufferedOutputStream(out);
        List<RootViewInfo> infoList = Collections.emptyList();
        writer.write("[".getBytes());

        try {
            infoList = infoFuture.get(2, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            SALog.i(TAG, "Screenshot interrupted, no screenshot will be sent.", e);
        } catch (final TimeoutException e) {
            SALog.i(TAG, "Screenshot took more than 2 second to be scheduled and executed. No screenshot will be sent.", e);
        } catch (final ExecutionException e) {
            SALog.i(TAG, "Exception thrown during screenshot attempt", e);
        } catch (Throwable e) {
            SALog.i(TAG, "Throwable thrown during screenshot attempt", e);
        } finally {
            infoFuture.cancel(true);
            mMainThreadHandler.removeCallbacks(infoFuture);
        }

        String screenName = null, activityTitle = null;
        final int infoCount = infoList.size();
        SALog.i(TAG, "infoCount:" + infoCount + ",time:" + (System.currentTimeMillis() - startSnapshot));
        for (int i = 0; i < infoCount; i++) {
            final RootViewInfo info = infoList.get(i);
            if (i > 0) {
                writer.write(",".getBytes());
            }
            if (info != null && info.screenshot != null && (isSnapShotUpdated(info.screenshot.getImageHash(), lastImageHash) || i > 0)) {
                writer.write("{".getBytes());
                writer.write("\"activity\":".getBytes());
                screenName = info.screenName;
                activityTitle = info.activityTitle;
                writer.write(JSONObject.quote(info.screenName).getBytes());
                writer.write(",".getBytes());
                writer.write(("\"scale\":").getBytes());
                writer.write((String.format("%s", info.scale)).getBytes());
                writer.write(",".getBytes());
                writer.write(("\"serialized_objects\":").getBytes());

                try {
                    JSONObject jsonRootObject = new JSONObject();
                    jsonRootObject.put("rootObject", info.rootView.hashCode());
                    JSONArray jsonObjects = new JSONArray();
                    snapshotViewHierarchy(jsonObjects, info.rootView);
                    jsonRootObject.put("objects", jsonObjects);
                    writer.write(jsonRootObject.toString().getBytes());
                    SALog.i(TAG, "snapshotViewHierarchy:" + (System.currentTimeMillis() - startSnapshot));
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }

                writer.write(",".getBytes());
                writer.write(("\"image_hash\":").getBytes());
                writer.write((JSONObject.quote(info.screenshot.getImageHash())).getBytes());
                writer.write(",".getBytes());
                writer.write(("\"screenshot\":").getBytes());
                writer.flush();
                info.screenshot.writeBitmapJSON(Bitmap.CompressFormat.PNG, 70, out);
                writer.write("}".getBytes());
            } else {
                writer.write("{}".getBytes());
            }
        }
        writer.write("]".getBytes());
        writer.flush();
        mSnapInfo.screenName = screenName;
        mSnapInfo.activityTitle = activityTitle;
        return mSnapInfo;
    }

    private void getVisibleRect(View view, Rect rect, boolean fullscreen) {
        if (fullscreen) {
            view.getGlobalVisibleRect(rect);
            return;
        }
        int[] offset = new int[2];
        view.getLocationOnScreen(offset);
        boolean visibleRect = view.getLocalVisibleRect(rect);
        SnapCache.getInstance().setLocalVisibleRect(view, visibleRect);
        rect.offset(offset[0], offset[1]);
    }

    private void snapshotViewHierarchy(JSONArray j, View rootView)
            throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            reset();
        }
    }

    private void reset() {
        mSnapInfo = new SnapInfo();
    }

    /**
     * 页面 ImageHash / H5 页面元素内容 发生变化 / H5 出现错误提示时需要更新页面信息
     *
     * @param newImageHash
     * @param lastImageHash
     * @return 是否上报页面信息
     */
    private boolean isSnapShotUpdated(String newImageHash, StringBuilder lastImageHash) {
        boolean isUpdated = false;

        if (newImageHash != null && lastImageHash != null) {
            isUpdated = newImageHash.equals(lastImageHash.toString());
        }

        isUpdated = !isUpdated;
        if (lastImageHash != null) {
            lastImageHash.delete(0, lastImageHash.length()).append(newImageHash);
        }
        return isUpdated;
    }

    private static class RootViewFinder implements Callable<List<RootViewInfo>> {

        private final List<RootViewInfo> mRootViews;
        private final CachedBitmap mCachedBitmap;
        private final int mClientDensity = DisplayMetrics.DENSITY_DEFAULT;

        public RootViewFinder() {
            mRootViews = new ArrayList<RootViewInfo>();
            mCachedBitmap = new CachedBitmap();
        }

        @Override
        public List<RootViewInfo> call() throws Exception {
            mRootViews.clear();
            try {
                Activity activity = AppStateManager.getInstance().getForegroundActivity();
                if (activity != null) {
                    JSONObject object = AopUtil.buildTitleAndScreenName(activity);
                    VisualUtil.mergeRnScreenNameAndTitle(object);
                    String screenName = object.optString(AopConstants.SCREEN_NAME);
                    String activityTitle = object.optString(AopConstants.TITLE);
                    View rootView = null;
                    final Window window = activity.getWindow();
                    if (window != null && window.isActive()) {
                        rootView = window.getDecorView().getRootView();
                    }
                    if (rootView == null) {
                        return mRootViews;
                    }
                    final RootViewInfo info = new RootViewInfo(screenName, activityTitle, rootView);
                    final View[] views = WindowHelper.getSortedWindowViews();
                    Bitmap bitmap = null;
                    if (views != null && views.length > 0) {
                        bitmap = mergeViewLayers(views, info);
                        for (View view : views) {
                            if (view.getWindowVisibility() != View.VISIBLE || view.getVisibility() != View.VISIBLE
                                    || view.getWidth() == 0 || view.getHeight() == 0
                                    || TextUtils.equals(WindowHelper.getWindowPrefix(view), WindowHelper.getMainWindowPrefix()))
                                continue;
                            //解决自定义框：比如通过 Window.addView 加的悬浮框
                            if (!WindowHelper.isCustomWindow(view)) {
                                //自定义框图层只参与底图绘制 mergeViewLayers ，不参与页面数据信息处理
                                RootViewInfo subInfo = new RootViewInfo(screenName, activityTitle, view.getRootView());
                                scaleBitmap(subInfo, bitmap);
                                mRootViews.add(subInfo);
                            }
                        }
                    }
                    if (mRootViews.size() == 0) {
                        scaleBitmap(info, bitmap);
                        mRootViews.add(info);
                    }
                }
            } catch (Throwable e) {
                SALog.d(TAG, "" + e);
            }
            return mRootViews;
        }

        Bitmap mergeViewLayers(View[] views, RootViewInfo info) {
            int width = info.rootView.getWidth();
            int height = info.rootView.getHeight();
            if (width == 0 || height == 0) {
                int[] screenSize = DeviceUtils.getDeviceSize(SensorsDataAPI.sharedInstance().getContext());
                width = screenSize[0];
                height = screenSize[1];
                if (width == 0 || height == 0) return null;
            }
            Bitmap fullScreenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            SoftWareCanvas canvas = new SoftWareCanvas(fullScreenBitmap);
            int[] windowOffset = new int[2];
            boolean skipOther, isDrawBackground = false;
            if (ViewUtil.getMainWindowCount(views) > 1) {
                skipOther = true;
            } else {
                skipOther = false;
            }
            WindowHelper.init();
            ViewUtil.invalidateLayerTypeView(views);
            for (View view : views) {
                if (!(view.getVisibility() != View.VISIBLE || view.getWidth() == 0 || view.getHeight() == 0 || !ViewUtil.isWindowNeedTraverse(view, WindowHelper.getWindowPrefix(view), skipOther))) {
                    canvas.save();
                    if (!WindowHelper.isMainWindow(view)) {
                        view.getLocationOnScreen(windowOffset);
                        canvas.translate((float) windowOffset[0], (float) windowOffset[1]);
                        if (WindowHelper.isDialogOrPopupWindow(view) && !isDrawBackground) {
                            isDrawBackground = true;
                            Paint paint = new Paint();
                            paint.setColor(0xA0000000);
                            canvas.drawRect(-(float) windowOffset[0], -(float) windowOffset[1], canvas.getWidth(), canvas.getHeight(), paint);
                        }
                    }
                    view.draw(canvas);
                    canvas.restoreToCount(1);
                }
            }
            canvas.destroy();
            return fullScreenBitmap;
        }

        private void scaleBitmap(final RootViewInfo info, Bitmap rawBitmap) {
            float scale = 1.0f;
            if (null != rawBitmap) {
                final int rawDensity = rawBitmap.getDensity();
                if (rawDensity != Bitmap.DENSITY_NONE) {
                    scale = ((float) mClientDensity) / rawDensity;
                }
                final int rawWidth = rawBitmap.getWidth();
                final int rawHeight = rawBitmap.getHeight();
                final int destWidth = (int) ((rawBitmap.getWidth() * scale) + 0.5);
                final int destHeight = (int) ((rawBitmap.getHeight() * scale) + 0.5);
                if (rawWidth > 0 && rawHeight > 0 && destWidth > 0 && destHeight > 0) {
                    mCachedBitmap.recreate(destWidth, destHeight, mClientDensity, rawBitmap);
                }
            }
            info.scale = scale;
            info.screenshot = mCachedBitmap;
        }
    }

    private static class CachedBitmap {

        private final Paint mPaint;
        private Bitmap mCached;
        // 含截图和 WebView 页面元素数据
        private String mImageHash = "";

        public CachedBitmap() {
            mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
            mCached = null;
        }

        public synchronized void recreate(int width, int height, int destDensity, Bitmap source) {
            if (null == mCached || mCached.getWidth() != width || mCached.getHeight() != height) {
                try {
                    mCached = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                } catch (final Throwable e) {
                    mCached = null;
                }

                if (null != mCached) {
                    mCached.setDensity(destDensity);
                }
            }

            if (null != mCached) {
                final Canvas scaledCanvas = new Canvas(mCached);
                scaledCanvas.drawBitmap(source, 0, 0, mPaint);

                try {
                    final ByteArrayOutputStream imageByte = new ByteArrayOutputStream();
                    mCached.compress(Bitmap.CompressFormat.PNG, 100, imageByte);
                    byte[] array = imageByte.toByteArray();

                    final String debugInfo = VisualizedAutoTrackService.getInstance().getLastDebugInfo();
                    if (!TextUtils.isEmpty(debugInfo)) {
                        byte[] debugInfoBytes = debugInfo.getBytes();
                        if (debugInfoBytes != null && debugInfoBytes.length > 0) {
                            array = concat(array, debugInfoBytes);
                        }
                    }

                    byte[] md5 = MessageDigest.getInstance("MD5").digest(array);
                    mImageHash = toHex(md5);
                } catch (Exception e) {
                    SALog.i(TAG, "CachedBitmap.recreate;Create image_hash error=" + e);
                }
            }
        }

        private static byte[] concat(byte[] first, byte[] second) {
            byte[] result = new byte[first.length + second.length];
            System.arraycopy(first, 0, result, 0, first.length);
            System.arraycopy(second, 0, result, first.length, second.length);
            return result;
        }

        // Writes a QUOTED base64 string (or the string null) to the output stream
        public synchronized void writeBitmapJSON(Bitmap.CompressFormat format, int quality,
                                                 OutputStream out)
                throws IOException {
            if (null == mCached || mCached.getWidth() == 0 || mCached.getHeight() == 0) {
                out.write("null".getBytes());
            } else {
                out.write('"');
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mCached.compress(Bitmap.CompressFormat.PNG, 100, stream);
                stream.flush();
                String bitmapStr = new String(Base64Coder.encode(stream.toByteArray()));
                out.write(bitmapStr.getBytes());
                out.write('"');
            }
        }

        private String getImageHash() {
            return mImageHash;
        }

        private String toHex(byte[] ary) {
            final String hex = "0123456789ABCDEF";
            String ret = "";
            for (int i = 0; i < ary.length; i++) {
                ret += hex.charAt((ary[i] >> 4) & 0xf);
                ret += hex.charAt(ary[i] & 0xf);
            }
            return ret;
        }
    }

    private static class RootViewInfo {
        final String screenName;
        final String activityTitle;
        final View rootView;
        CachedBitmap screenshot;
        float scale;

        RootViewInfo(String screenName, String activityTitle, View rootView) {
            this.screenName = screenName;
            this.activityTitle = activityTitle;
            this.rootView = rootView;
            this.screenshot = null;
            this.scale = 1.0f;
        }
    }
}