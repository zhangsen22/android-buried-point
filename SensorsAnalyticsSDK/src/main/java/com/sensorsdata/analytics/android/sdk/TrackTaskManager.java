package com.sensorsdata.analytics.android.sdk;

import java.util.concurrent.LinkedBlockingQueue;

public class TrackTaskManager {
    private static TrackTaskManager trackTaskManager;
    /**
     * 请求线程队列
     */
    private final LinkedBlockingQueue<Runnable> mTrackEventTasksCache;

    private TrackTaskManager() {
        mTrackEventTasksCache = new LinkedBlockingQueue<>();
    }

    public static synchronized TrackTaskManager getInstance() {
        try {
            if (null == trackTaskManager) {
                trackTaskManager = new TrackTaskManager();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return trackTaskManager;
    }

    void addTrackEventTask(Runnable trackEvenTask) {
        try {
                mTrackEventTasksCache.put(trackEvenTask);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    Runnable takeTrackEventTask() {
        try {
                return mTrackEventTasksCache.take();

        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    Runnable pollTrackEventTask() {
        try {
                return mTrackEventTasksCache.poll();

        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }
}
