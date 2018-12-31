## Custom Push Notification Layouts

WebEngage Android SDK has included two new functional interfaces in v3.10.1 which allows you to modify the in-build push notification layouts.

**1. CustomPushRender**: Allows you to show push notification for the first time.

**2. CustomPushRerender**: Allows you to update/rerender push notification.

Custom push render callbacks are only triggered when you provide `we_custom_render: true` in the custom key-values from our dashboard and custom push render implementations are registered in the Android application.


### Push Notification Data

The instance of PushNotificationData available to you in the onRender and onRerender callbacks contains the details required to construct and show the push notification.


### Tracking the Push Campaign

In order to correctly track the push notification clicks, dismissed, etc. it is necessary to use the PendingIntents constructed through the APIs provided in the WebEngage Android SDK.

PendingIntentFactory includes the following APIs for constructing PendingIntents which must be set in the Notification for tracking all the push campaign statistics.

**1. constructPushClickPendingIntent**: This method returns PendingIntent which can be set as click listener for any action button on your push notification.

**2. constructPushDeletePendingIntent**: This PendingIntent must be set as deleteIntent in your notification, which will help the WebEngage SDK to track push notification dismisses.

**3. constructRerenderPendingIntent**: Returns PendingIntent which will trigger the onRerender callback on click. This PendingIntent can be used to update/rerender your notification.

**4. constructCarouselBrowsePendingIntent**: Returns PendingIntent which will automatically track the carousel browse event on click for the push carousel campaign and also triggers the onRerender callback which can be used to update the current carousel item.

**5. constructPushRatingSubmitPendingIntent**: Method takes an integer for rating as parameter which will track rating submit event on click for the push rating campaign.

**Note**: Any PendingIntents used in the push notification which is not provided by the above specified APIs, will not be tracked by the WebEngage Android SDK and hence will not be seen on the campaign stats page in your WebEngage dashboard.


### Sample Custom Layouts

Prerequisites:

1. Add the following dependencies in your app-level build.gradle file.

```gradle
    // Support library
    implementation 'com.android.support:appcompat-v7:28.0.0'

    // Firebase
    implementation 'com.google.firebase:firebase-core:16.0.6'
    implementation 'com.google.firebase:firebase-messaging:17.3.4'

    // WebEngage Beta SDK
    // This SDK is only for testing purpose, do NOT use this in production applications
    implementation 'com.webengage:android-sdk-beta:3.10.1'
```

Also integrate FCM with your app as shown [here](https://docs.webengage.com/docs/android-fcm-integration)


2. Create an implementation of CustomPushRender and CustomPushRerender interfaces as shown below.

```java
public class MyPushRenderer implements CustomPushRender, CustomPushRerender {
    @Override
    public boolean onRender(Context context, PushNotificationData pushNotificationData) {
        // render your notification here
        return true;
    }

    @Override
    public boolean onRerender(Context context, PushNotificationData pushNotificationData, Bundle bundle) {
        // rerender your notification here
        return true;
    }
}
```


3. Make sure to register for custom push render callbacks in your Application class as shown below.

```java
public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        ...

        initWebEngage();
    }

    private void initWebEngage() {
        WebEngageConfig config = new WebEngageConfig.Builder()
                    .setWebEngageKey(YOUR-WEBENGAGE-LICENSE-CODE)
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
```

Here are some sample code which might help you to build your own custom push notification layouts.


### 1. Big Text Layout

The following code snippet shows how to show Big Text notification within WebEngage CustomPushRender implementation.

`MyPushRenderer.java`

```java
    @Override
    public boolean onRender(Context context, PushNotificationData pushNotificationData) {
        if (pushNotificationData == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // create notification channel
        }

        // Big text
        if (pushNotificationData.getStyle() == WebEngageConstant.STYLE.BIG_TEXT) {
            PendingIntent deletePendingIntent = PendingIntentFactory.constructPushDeletePendingIntent(context, pushNotificationData);
            PendingIntent contentPendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, pushNotificationData.getPrimeCallToAction(), true);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MY_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(pushNotificationData.getTitle())
                    .setContentText(pushNotificationData.getContentText())
                    .setContentIntent(contentPendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .setBigContentTitle(pushNotificationData.getBigTextStyleData().getBigContentTitle())
                            .bigText(pushNotificationData.getBigTextStyleData().getBigText()))
                    .setDeleteIntent(deletePendingIntent);

            // actions
            List<CallToAction> actionsList = pushNotificationData.getActions();
            if (actionsList != null) {
                for (CallToAction callToAction : actionsList) {
                    PendingIntent ctaPendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, callToAction, true);
                    builder.addAction(0, callToAction.getText(), ctaPendingIntent);
                }
            }

            Notification notification = builder.build();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(pushNotificationData.getVariationId().hashCode(), notification);
            Log.d(TAG, "Rendered push notification from application: big text");
            return true;
        }

        return false;
    }
```


### 2. Big Picture Layout

The below code sample shows how to show multi-line text in a Big Picture notification style.

`MyPushRenderer.java`

```java
    @Override
    public boolean onRender(Context context, PushNotificationData pushNotificationData) {
        if (pushNotificationData == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // create notification channel
        }

        if (pushNotificationData.getStyle() == WebEngageConstant.STYLE.BIG_PICTURE) {
            PendingIntent deletePendingIntent = PendingIntentFactory.constructPushDeletePendingIntent(context, pushNotificationData);
            PendingIntent contentPendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, pushNotificationData.getPrimeCallToAction(), true);

            RemoteViews collapsedView = new RemoteViews(context.getPackageName(), R.layout.push_collapsed);
            collapsedView.setTextViewText(R.id.notificationTitle, pushNotificationData.getTitle());
            collapsedView.setTextViewText(R.id.notificationText, pushNotificationData.getContentText());

            // Download the big picture image
            Bitmap bigPicture = DownloadManager.getBitmapFromURL(pushNotificationData.getBigPictureStyleData().getBigPictureUrl(), false);

            RemoteViews bigPictureView = new RemoteViews(context.getPackageName(), R.layout.push_big_picture);
            bigPictureView.setTextViewText(R.id.notificationTitle, pushNotificationData.getBigPictureStyleData().getBigContentTitle());
            bigPictureView.setTextViewText(R.id.notificationText, pushNotificationData.getBigPictureStyleData().getSummary());
            bigPictureView.setInt(R.id.notificationText, "setMaxLines", 4);

            if (bigPicture != null) {
                bigPictureView.setViewVisibility(R.id.big_picture_imageview, View.VISIBLE);
                bigPictureView.setImageViewBitmap(R.id.big_picture_imageview, bigPicture);
            } else {
                bigPictureView.setViewVisibility(R.id.big_picture_imageview, View.GONE);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MY_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setCustomContentView(collapsedView)
                    .setCustomBigContentView(bigPictureView)
                    .setContentIntent(contentPendingIntent)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setDeleteIntent(deletePendingIntent);

            // Add actions
            List<CallToAction> actionsList = pushNotificationData.getActions();
            if (actionsList != null) {
                for (CallToAction callToAction : actionsList) {
                    PendingIntent ctaPendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, callToAction, true);
                    builder.addAction(0, callToAction.getText(), ctaPendingIntent);
                }
            }

            Notification notification = builder.build();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(pushNotificationData.getVariationId().hashCode(), notification);
            Log.d(TAG, "Rendered push notification from application: big picture");
            return true;
        }

        return false;
    }
```


### 3. Carousel Layout

In order to modify the carousel layout, it is important to implement both onRender and onRerender methods.

The following code shows how to display carousel notification without the item label over the image.

Initially render the carousel notification from the onRender callback as shown below.

```java
    @Override
    public boolean onRender(Context context, PushNotificationData pushNotificationData) {
        if (pushNotificationData == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // create notification channel
        }

        if (pushNotificationData.getStyle() == WebEngageConstant.STYLE.CAROUSEL_V1) {
            PendingIntent deletePendingIntent = PendingIntentFactory.constructPushDeletePendingIntent(context, pushNotificationData);
            PendingIntent contentPendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, pushNotificationData.getPrimeCallToAction(), true);

            long when = System.currentTimeMillis();

            Bundle browseExtraData = new Bundle();
            browseExtraData.putLong("when", when);
            PendingIntent leftPendingIntent = PendingIntentFactory.constructCarouselBrowsePendingIntent(context, pushNotificationData, 0, "left", "carousel_left", browseExtraData);
            PendingIntent rightPendingIntent = PendingIntentFactory.constructCarouselBrowsePendingIntent(context, pushNotificationData, 0, "right", "carousel_right", browseExtraData);

            // Download all images and cache
            List<CarouselV1CallToAction> ctas = pushNotificationData.getCarouselV1Data().getCallToActions();
            for (CarouselV1CallToAction cta : ctas) {
                DownloadManager.downloadBitmap(cta.getImageURL());
            }

            RemoteViews collapsedView = new RemoteViews(context.getPackageName(), R.layout.push_collapsed);
            collapsedView.setTextViewText(R.id.notificationTitle, pushNotificationData.getTitle());
            collapsedView.setTextViewText(R.id.notificationText, pushNotificationData.getContentText());

            CarouselV1CallToAction cta = ctas.get(0);
            PendingIntent imagePendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, cta, false);

            // First try to get the image from cache
            Bitmap img = DownloadManager.getBitmapFromURL(cta.getImageURL(), true);
            if (img == null) {
                // Try to download and cache the image again
                img = DownloadManager.getBitmapFromURL(cta.getImageURL(), false);
                if (img == null) {
                    // Use a placeholder/default image
                    img = BitmapFactory.decodeResource(context.getResources(), R.drawable.banner_android);
                }
            }

            RemoteViews carouselView = new RemoteViews(context.getPackageName(), R.layout.push_carousel_landscape);
            carouselView.setTextViewText(R.id.notificationTitle, pushNotificationData.getCarouselV1Data().getBigContentTitle());
            carouselView.setTextViewText(R.id.notificationText, pushNotificationData.getCarouselV1Data().getSummary());
            carouselView.setImageViewBitmap(R.id.carousel_landscape_image, img);
            carouselView.setOnClickPendingIntent(R.id.carousel_landscape_image, imagePendingIntent);
            carouselView.setOnClickPendingIntent(R.id.left, leftPendingIntent);
            carouselView.setOnClickPendingIntent(R.id.right, rightPendingIntent);

            Notification notification = new NotificationCompat.Builder(context, MY_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setCustomContentView(collapsedView)
                    .setCustomBigContentView(carouselView)
                    .setContentIntent(contentPendingIntent)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setDeleteIntent(deletePendingIntent)
                    .build();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(pushNotificationData.getVariationId().hashCode(), notification);
            Log.d(TAG, "Rendered push notification from application: carousel");
            return true;
        }

        return false;
    }
```

Now rerender the carousel notification in onRerender callback every time the user browses the carousel by clicking on left or right arrows as shown below.

```java
@Override
public boolean onRerender(Context context, PushNotificationData pushNotificationData, Bundle bundle) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // create notification channel id if not yet created
    }

    Bundle customData = pushNotificationData.getCustomData();
    Log.d(TAG, "custom data: " + String.valueOf(customData) + ", extra data: " + String.valueOf(bundle));

    if (pushNotificationData.getStyle() == WebEngageConstant.STYLE.CAROUSEL_V1) {
        List<CarouselV1CallToAction> callToActionList = pushNotificationData.getCarouselV1Data().getCallToActions();
        int size = callToActionList.size();
        String navigation = bundle.getString("navigation", "right");
        int prevIndex = bundle.getInt("current");
        long when = bundle.getLong("when");
        int newIndex = 0;
        if (navigation.equals("right")) {
            newIndex = (prevIndex + 1) % size;
        } else {
            newIndex = (prevIndex - 1 + size) % size;
        }

        PendingIntent deletePendingIntent = PendingIntentFactory.constructPushDeletePendingIntent(context, pushNotificationData);
        PendingIntent contentPendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, pushNotificationData.getPrimeCallToAction(), true);

        Bundle browseExtraData = new Bundle();
        browseExtraData.putLong("when", when);
        PendingIntent leftPendingIntent = PendingIntentFactory.constructCarouselBrowsePendingIntent(context, pushNotificationData, newIndex, "left", "carousel_left", browseExtraData);
        PendingIntent rightPendingIntent = PendingIntentFactory.constructCarouselBrowsePendingIntent(context, pushNotificationData, newIndex, "right", "carousel_right", browseExtraData);

        CarouselV1CallToAction cta = callToActionList.get(newIndex);
        PendingIntent imagePendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, cta, false);

        Bitmap img = DownloadManager.getBitmapFromURL(cta.getImageURL(), true);
        if (img == null) {
            img = DownloadManager.getBitmapFromURL(cta.getImageURL(), false);
            if (img == null) {
                // Use a default/placeholder image
                img = BitmapFactory.decodeResource(context.getResources(), R.drawable.banner_android);
            }
        }

        RemoteViews collapsedView = new RemoteViews(context.getPackageName(), R.layout.push_collapsed);
        collapsedView.setTextViewText(R.id.notificationTitle, pushNotificationData.getTitle());
        collapsedView.setTextViewText(R.id.notificationText, pushNotificationData.getContentText());

        RemoteViews carouselView = new RemoteViews(context.getPackageName(), R.layout.push_carousel_landscape);
        carouselView.setTextViewText(R.id.notificationTitle, pushNotificationData.getCarouselV1Data().getBigContentTitle());
        carouselView.setTextViewText(R.id.notificationText, pushNotificationData.getCarouselV1Data().getSummary());
        carouselView.setImageViewBitmap(R.id.carousel_landscape_image, img);
        carouselView.setOnClickPendingIntent(R.id.carousel_landscape_image, imagePendingIntent);
        carouselView.setOnClickPendingIntent(R.id.left, leftPendingIntent);
        carouselView.setOnClickPendingIntent(R.id.right, rightPendingIntent);

        Notification notification = new NotificationCompat.Builder(context, MY_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(carouselView)
                .setContentIntent(contentPendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setWhen(when)
                .build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(pushNotificationData.getVariationId().hashCode(), notification);
        Log.d(TAG, "Rerendered push notification: carousel");
        return true;
    }

    return false;
}
```


### 4. Rating Layout

This code snippet shows how to display the Rating notification layout using onRender and onRerender callbacks.

```java
    @Override
    public boolean onRender(Context context, PushNotificationData pushNotificationData) {
        if (pushNotificationData == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // create notification channel
        }

        if (pushNotificationData.getStyle() == WebEngageConstant.STYLE.RATING_V1) {
            PendingIntent deletePendingIntent = PendingIntentFactory.constructPushDeletePendingIntent(context, pushNotificationData);
            PendingIntent contentPendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, pushNotificationData.getPrimeCallToAction(), true);

            long when = System.currentTimeMillis();

            RemoteViews collapsedView = new RemoteViews(context.getPackageName(), R.layout.push_collapsed);
            collapsedView.setTextViewText(R.id.notificationTitle, pushNotificationData.getTitle());
            collapsedView.setTextViewText(R.id.notificationText, pushNotificationData.getContentText());

            RemoteViews npsView = new RemoteViews(context.getPackageName(), R.layout.push_rating);
            npsView.setTextViewText(R.id.notificationTitle, pushNotificationData.getRatingV1().getBigContentTitle());
            npsView.setTextViewText(R.id.notificationText, pushNotificationData.getRatingV1().getSummary());

            if (pushNotificationData.getRatingV1().getImageUrl() != null) {
                Bitmap img = DownloadManager.getBitmapFromURL(pushNotificationData.getRatingV1().getImageUrl(), false);
                npsView.setViewVisibility(R.id.rate_frame, View.VISIBLE);
                if (img != null) {
                    npsView.setViewVisibility(R.id.rate_image, View.VISIBLE);
                    npsView.setImageViewBitmap(R.id.rate_image, img);
                } else {
                    npsView.setInt(R.id.rate_frame, "setBackgroundColor", pushNotificationData.getRatingV1().getContentBackgroundColor());
                }
            }

            if (pushNotificationData.getRatingV1().getContentTitle() != null) {
                npsView.setViewVisibility(R.id.rate_frame, View.VISIBLE);
                npsView.setViewVisibility(R.id.rate_title, View.VISIBLE);
                npsView.setTextViewText(R.id.rate_title, pushNotificationData.getRatingV1().getContentTitle());
            }

            if (pushNotificationData.getRatingV1().getContentMessage() != null) {
                npsView.setViewVisibility(R.id.rate_frame, View.VISIBLE);
                npsView.setViewVisibility(R.id.rate_message, View.VISIBLE);
                npsView.setTextViewText(R.id.rate_message, pushNotificationData.getRatingV1().getContentMessage());
            }

            for (int i = 1; i <= 5; i++) {
                Bundle rateClickExtraData = new Bundle();
                rateClickExtraData.putInt("current", i);
                rateClickExtraData.putLong("when", when);
                final PendingIntent rateClickPendingIntent = PendingIntentFactory.constructRerenderPendingIntent(context, pushNotificationData, "rate_click_" + i, rateClickExtraData);

                int id = context.getResources().getIdentifier("rate_" + i, "id", context.getPackageName());
                npsView.setOnClickPendingIntent(id, rateClickPendingIntent);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MY_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setCustomContentView(collapsedView)
                    .setCustomBigContentView(npsView)
                    .setContentIntent(contentPendingIntent)
                    .setDeleteIntent(deletePendingIntent)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

            Notification notification = builder.build();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(pushNotificationData.getVariationId().hashCode(), notification);
            Log.d(TAG, "Rendered push notification from application: rating");
            return true;
        }

        return false;
    }
```

Rerender the rating notification in onRerender callback whenever the user selects/changes the rating as shown below.

```java
@Override
public boolean onRerender(Context context, PushNotificationData pushNotificationData, Bundle bundle) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // create notification channel id if not yet created
    }

    if (pushNotificationData.getStyle() == WebEngageConstant.STYLE.RATING_V1) {
        int currIndex = bundle.getInt("current");
        long when = bundle.getLong("when");

        RemoteViews collapsedView = new RemoteViews(context.getPackageName(), R.layout.push_collapsed);
        collapsedView.setTextViewText(R.id.notificationTitle, pushNotificationData.getTitle());
        collapsedView.setTextViewText(R.id.notificationText, pushNotificationData.getContentText());

        RemoteViews npsView = new RemoteViews(context.getPackageName(), R.layout.push_rating);
        npsView.setTextViewText(R.id.notificationTitle, pushNotificationData.getRatingV1().getBigContentTitle());
        npsView.setTextViewText(R.id.notificationText, pushNotificationData.getRatingV1().getSummary());

        if (pushNotificationData.getRatingV1().getImageUrl() != null) {
            Bitmap img = DownloadManager.getBitmapFromURL(pushNotificationData.getRatingV1().getImageUrl(), false);
            npsView.setViewVisibility(R.id.rate_frame, View.VISIBLE);
            if (img != null) {
                npsView.setViewVisibility(R.id.rate_image, View.VISIBLE);
                npsView.setImageViewBitmap(R.id.rate_image, img);
            } else {
                npsView.setInt(R.id.rate_frame, "setBackgroundColor", pushNotificationData.getRatingV1().getContentBackgroundColor());
            }
        }

        if (pushNotificationData.getRatingV1().getContentTitle() != null) {
            npsView.setViewVisibility(R.id.rate_frame, View.VISIBLE);
            npsView.setViewVisibility(R.id.rate_title, View.VISIBLE);
            npsView.setTextViewText(R.id.rate_title, pushNotificationData.getRatingV1().getContentTitle());
        }

        if (pushNotificationData.getRatingV1().getContentMessage() != null) {
            npsView.setViewVisibility(R.id.rate_frame, View.VISIBLE);
            npsView.setViewVisibility(R.id.rate_message, View.VISIBLE);
            npsView.setTextViewText(R.id.rate_message, pushNotificationData.getRatingV1().getContentMessage());
        }

        for (int i = 1; i <= 5; i++) {
            Bundle rateClickExtraData = new Bundle();
            rateClickExtraData.putInt("current", i);
            rateClickExtraData.putLong("when", when);
            PendingIntent rateClickPendingIntent = PendingIntentFactory.constructRerenderPendingIntent(context, pushNotificationData, "rate_click_" + i, rateClickExtraData);

            int id = context.getResources().getIdentifier("rate_" + i, "id", context.getPackageName());
            npsView.setOnClickPendingIntent(id, rateClickPendingIntent);

            // Here you can use any resource for selected and unselected rating icon
            if (i <= currIndex) {
                npsView.setImageViewResource(id, R.drawable.star_selected);
            } else {
                npsView.setImageViewResource(id, R.drawable.star_unselected);
            }
        }

        PendingIntent rateSubmitPendingIntent = PendingIntentFactory.constructPushRatingSubmitPendingIntent(context, pushNotificationData, currIndex);
        npsView.setOnClickPendingIntent(R.id.rate_submit, rateSubmitPendingIntent);

        PendingIntent deletePendingIntent = PendingIntentFactory.constructPushDeletePendingIntent(context, pushNotificationData);
        PendingIntent contentPendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, pushNotificationData.getPrimeCallToAction(), true);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MY_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(npsView)
                .setContentIntent(contentPendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(pushNotificationData.getVariationId().hashCode(), notification);
        Log.d(TAG, "Rerendered push notification: rating");
        return true;
    }

    return false;
}
```

These code samples are simple examples to modify and render push notifications on your own. You can make more complex changes as per your requirements.

