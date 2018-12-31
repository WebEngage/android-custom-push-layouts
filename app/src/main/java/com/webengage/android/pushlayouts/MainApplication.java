package com.webengage.android.pushlayouts;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.webengage.sdk.android.WebEngage;
import com.webengage.sdk.android.WebEngageActivityLifeCycleCallbacks;
import com.webengage.sdk.android.WebEngageConfig;

public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        initHttpCache();

        initWebEngage();
    }

    private void initHttpCache() {
        DownloadManager.createHttpCache(this);
        DownloadManager.cleanHttpCache(this, 3);
    }

    private void initWebEngage() {
        WebEngageConfig config = new WebEngageConfig.Builder()
                .setWebEngageKey("YOUR-WEBENGAGE-LICENSE-CODE")
                .setDebugMode(true)
                .build();

        registerActivityLifecycleCallbacks(new WebEngageActivityLifeCycleCallbacks(this, config));

        // Register for custom push render callbacks
        MyPushRenderer myPushRenderer = new MyPushRenderer();
        WebEngage.registerCustomPushRenderCallback(myPushRenderer);
        WebEngage.registerCustomPushRerenderCallback(myPushRenderer);

        try {
            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                @Override
                public void onSuccess(InstanceIdResult instanceIdResult) {
                    String token = instanceIdResult.getToken();
                    Log.d(TAG,  "FCM token: " + token);
                    WebEngage.get().setRegistrationID(token);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception while getting FCM token", e);
        }
    }
}
