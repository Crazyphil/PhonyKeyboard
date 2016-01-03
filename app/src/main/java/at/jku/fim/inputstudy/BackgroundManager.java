package at.jku.fim.inputstudy;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

// Source: https://gist.github.com/xadh00m/1584a58ddb3d724cdd24
// based on https://gist.github.com/steveliles/11116937
public class BackgroundManager implements Application.ActivityLifecycleCallbacks {
    public static final long BACKGROUND_DELAY = 500;

    private static final String TAG = "BackgroundManager";

    private static BackgroundManager sInstance;

    public interface Listener {
        void onBecameForeground();
        void onBecameBackground();
    }

    private boolean mInBackground = true;
    private final List<Listener> listeners = new ArrayList<>();
    private final Handler mBackgroundDelayHandler = new Handler();
    private Runnable mBackgroundTransition;

    public static void init(Application application) {
        if (sInstance != null) {
            throw new IllegalStateException("BackgroundManager is already initialized");
        }
        getInstance(application);
    }

    public static BackgroundManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("BackgroundManager is not initialised - invoke at least once with parameterised init/getInstance");
        }
        return sInstance;
    }

    public static BackgroundManager getInstance(Application application) {
        if (sInstance == null) {
            sInstance = new BackgroundManager(application);
        }
        return sInstance;
    }

    private BackgroundManager(Application application) {
        application.registerActivityLifecycleCallbacks(this);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public boolean isInBackground() {
        return mInBackground;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (mBackgroundTransition != null) {
            mBackgroundDelayHandler.removeCallbacks(mBackgroundTransition);
            mBackgroundTransition = null;
        }

        if (mInBackground) {
            mInBackground = false;
            notifyOnBecameForeground();
            Log.v(TAG, "Application went to foreground");
        }
    }

    private void notifyOnBecameForeground() {
        for (Listener listener : listeners) {
            try {
                listener.onBecameForeground();
            } catch (Exception e) {
                Log.e(TAG, "Listener threw exception onBecameForeground!", e);
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (!mInBackground && mBackgroundTransition == null) {
            mBackgroundTransition = new Runnable() {
                @Override
                public void run() {
                    mInBackground = true;
                    mBackgroundTransition = null;
                    notifyOnBecameBackground();
                    Log.v(TAG, "Application went to background");
                }
            };
            mBackgroundDelayHandler.postDelayed(mBackgroundTransition, BACKGROUND_DELAY);
        }
    }

    private void notifyOnBecameBackground() {
        for (Listener listener : listeners) {
            try {
                listener.onBecameBackground();
            } catch (Exception e) {
                Log.e(TAG, "Listener threw exception onBecameBackground!", e);
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
}