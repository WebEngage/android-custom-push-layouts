## Custom Push Notification Layouts

WebEngage Android SDK has included two new functional interfaces in v3.10.1 which allows you to modify the default push notification layouts.

**Note**: Make sure you follow the [Android notification layout guidelines](https://developer.android.com/training/notify-user/custom-notification) before creating your own custom layouts. Usually, collapsed view layouts are limited to 64 dp, and expanded view layouts are limited to 256 dp.


**1. CustomPushRender**: Called for rendering push notifications. This is where you can set your custom layouts and notify push notifications.

```java
public interface CustomPushRender {
    /**
     * @return: Return true if notification is rendered successfully, else return false.
     */
    boolean onRender(Context context, PushNotificationData pushNotificationData);
}
```


**2. CustomPushRerender**: Called for re-rendering/updating push notifications. This is where you can update your push notifications.

```java
public interface CustomPushRerender {
    /**
     * @return: Return true if notification is re-rendered successfully, else return false.
     */
    boolean onRerender(Context context, PushNotificationData pushNotificationData, Bundle extras);
}
```


Custom push render callbacks are only called when you provide `we_custom_render: true` in the custom key-values while creating push campaign from WebEngage dashboard and CustomPushRender and CustomPushRerender implementations are registered using `WebEngage.registerCustomPushRenderCallback` and `WebEngage.registerCustomPushRerenderCallback` APIs.

If `onRender` or `onRerender` method returns false, then notification will not be displayed.


### Push Notification Data

The instance of PushNotificationData available to you in the onRender and onRerender callbacks contains the details required to construct and show the push notification.


### Tracking Push Notification Actions

In order to correctly track the push notification actions such as clicks, updates, dismissed, etc. it is mandatory to use the PendingIntents constructed through the APIs provided in the WebEngage's PendingIntentFactory class.

PendingIntentFactory includes the APIs for constructing the following PendingIntents which must be set in the Notification for tracking different push campaign actions.


####1. Click PendingIntent

`PendingIntent constructPushClickPendingIntent(Context context, PushNotificationData pushNotificationData, CallToAction cta, boolean autoCancel)` method returns a PendingIntent which can be set as PendingIntent for content intent or for any action button in your custom push notification.

```java
    boolean autoCancel = true;  // true if notification should be dismissed on click, else false
    PendingIntent contentPendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, pushNotificationData.getPrimeCallToAction(), autoCancel);
    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MY_CHANNEL_ID)
        ...
        .setContentIntent(contentPendingIntent)
        ...
```


####2. Delete PendingIntent

`PendingIntent constructPushDeletePendingIntent(Context context, PushNotificationData pushNotificationData)` method returns a PendingIntent that must be set as delete intent in your notification, which will help WebEngage SDK to track push notification dismisses.

```java
    PendingIntent deletePendingIntent = PendingIntentFactory.constructPushDeletePendingIntent(context, pushNotificationData);
    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MY_CHANNEL_ID)
        ...
        .setDeleteIntent(deletePendingIntent)
        ...
```


####3. Rerender PendingIntent

`PendingIntent constructRerenderPendingIntent(Context context, PushNotificationData pushNotificationData, String requestCodePrefix, Bundle extraData)` method returns a PendingIntent which will trigger the `onRerender` callback on click. This callback can be used to update/rerender your notification.

*param* **String requestCodePrefix**: The request code of the returned PendingIntent is generated using the following code:

```java
    ...
    int requestCode = (requestCodePrefix + pushNotificationData.getVariationId()).hashCode();
    PendingIntent pendingIntent = PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    return pendingIntent;
```

*param* **Bundle extraData**: This bundle must include additional data required for rerendering the notification. This bundle will be passed in `boolean onRerender(Context context, PushNotificationData pushNotificationData, Bundle extraData)` method as extraData for updating the notification.

For E.g. You might want to update the push notification content when user selects a star in rating push notification. In order to mark the star(s) as selected, you might need an additional information that tells you which star was clicked. Such additional data can be passed in the extraData bundle.

```java
    Bundle rateClickExtraData = new Bundle();
    rateClickExtraData.putInt("current", i);
    ...

    PendingIntent rateClickPendingIntent = PendingIntentFactory.constructRerenderPendingIntent(context, pushNotificationData, "rate_click_" + i, rateClickExtraData);
    ratingView.setOnClickPendingIntent(R.id.selected_star, rateClickPendingIntent);
```


####4. Carousel Browse PendingIntent

`PendingIntent constructCarouselBrowsePendingIntent(Context context, PushNotificationData pushNotificationData, int newIndex, String navigation, String requestCodePrefix, Bundle extraData)` method returns a PendingIntent which will automatically track the carousel browse event on click of left/right arraows in the carousel push notification. It also triggers the `onRerender` callback which can be used to update the current carousel item.

*param* **int newIndex**: This is the newly calculated index of the carousel item to be shown.

*param* **String navigation**: This indicates the direction in which the carousel is browsed.

```java
    int currIndex = 0;
    PendingIntent leftPendingIntent = PendingIntentFactory.constructCarouselBrowsePendingIntent(context, pushNotificationData, currIndex, "left", "carousel_left", browseExtraData);
    PendingIntent rightPendingIntent = PendingIntentFactory.constructCarouselBrowsePendingIntent(context, pushNotificationData, currIndex, "right", "carousel_right", browseExtraData);
    ...
    carouselView.setOnClickPendingIntent(R.id.left_arraow, leftPendingIntent);
    carouselView.setOnClickPendingIntent(R.id.right_arrow, rightPendingIntent);
    ...
```


####5. Rating Submit PendingIntent

`PendingIntent constructPushRatingSubmitPendingIntent()` method takes an integer for rating as parameter which will track rating submit event on click of submit button in rating notification.

```java
    int currIndex = extraData.getInt("current");  // current index can be obtained from extraData bundle provided while setting Click PendingIntent
    PendingIntent rateSubmitPendingIntent = PendingIntentFactory.constructPushRatingSubmitPendingIntent(context, pushNotificationData, currIndex);
    ratingView.setOnClickPendingIntent(R.id.rating_submit_button, rateSubmitPendingIntent);
```

**Note**: Any PendingIntents used in the push notification which is not provided by the above specified APIs, will not be tracked by the WebEngage Android SDK and hence will not be seen on the campaign stats page in your WebEngage dashboard.

Sample usage of these PendingIntents can be found in the code snippets of sample custom layouts given below.


### Sample Custom Layouts

**Prerequisites**:

1. Add the following dependencies in your app-level build.gradle file.

```gradle
    // Firebase
    implementation 'com.google.firebase:firebase-core:16.0.6'
    implementation 'com.google.firebase:firebase-messaging:17.3.4'

    // WebEngage Beta SDK
    // This SDK is only for testing purpose, do NOT use this in production applications
    implementation 'com.webengage:android-sdk-beta:3.10.1'
```

Also integrate FCM with your app as shown [here](https://docs.webengage.com/docs/android-fcm-integration)


2. Create implementations of CustomPushRender and CustomPushRerender interfaces as shown below.

```java
public class MyPushRenderer implements CustomPushRender, CustomPushRerender {
    @Override
    public boolean onRender(Context context, PushNotificationData pushNotificationData) {
        // render your notification here
        return true;
    }

    @Override
    public boolean onRerender(Context context, PushNotificationData pushNotificationData, Bundle bundle) {
        // re-render your notification here
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

Here are some sample code snippets which might help you to build your own custom push notification layouts.


### 1. Big Text Layout

The following code snippet shows how to show Big Text notification within WebEngage CustomPushRender implementation.

`MyPushRenderer.java`

```java
public class MyPushRenderer implements CustomPushRender, CustomPushRerender {
    ...

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

    ...

}
```


### 2. Big Picture Layout

The below code sample shows how to show multi-line text in a Big Picture notification style.

[push_collapsed.xml](https://github.com/WebEngage/android-custom-push-layouts/blob/master/app/src/main/res/layout/push_collapsed.xml)

[push_big_picture.xml](https://github.com/WebEngage/android-custom-push-layouts/blob/master/app/src/main/res/layout/push_big_picture.xml)

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
                    .setDeleteIntent(deletePendingIntent);

            // Add custom actions
            // Note: If you use NotificationCompat.DecoratedCustomViewStyle(), you can directly add actions to NotificationCompat.Builder without using custom layout (push_actions.xml) for actions.
            List<CallToAction> actionsList = pushNotificationData.getActions();
            if (actionsList != null && actionsList.size() > 0) {
                bigPictureView.setViewVisibility(R.id.push_actions, View.VISIBLE);
                for (int i = 0; i < actionsList.size(); i++) {
                    CallToAction callToAction = actionsList.get(i);
                    PendingIntent ctaPendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, callToAction, true);
                    int actionId = -1;
                    switch (i) {
                        case 0:
                            actionId = R.id.action1;
                            break;
                        case 1:
                            actionId = R.id.action2;
                            break;
                        case 2:
                            actionId = R.id.action3;
                            break;
                    }
                    if (actionId != -1) {
                        bigPictureView.setViewVisibility(actionId, View.VISIBLE);
                        bigPictureView.setTextViewText(actionId, callToAction.getText());
                        bigPictureView.setOnClickPendingIntent(actionId, ctaPendingIntent);
                    }
                }
            } else {
                Log.d(TAG, "no actions received");
                bigPictureView.setViewVisibility(R.id.push_actions, View.GONE);
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


### 3. Carousel Landscape Layout

In order to modify the carousel layout, it is important to implement both onRender and onRerender methods.

The following code shows how to display carousel notification without the item label over the image.

Initially render the carousel notification from the onRender callback as shown below.

[push_carousel_landscape.xml](https://github.com/WebEngage/android-custom-push-layouts/blob/master/app/src/main/res/layout/push_carousel_landscape.xml)

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
            if ("landscape".equals(pushNotificationData.getCarouselV1Data().getMODE())) {
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

                Bitmap img = DownloadManager.getBitmapFromURL(cta.getImageURL(), true);
                if (img == null) {
                    img = DownloadManager.getBitmapFromURL(cta.getImageURL(), false);
                    if (img == null) {
                        // Use a placeholder image
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
                        .setDeleteIntent(deletePendingIntent)
                        .build();

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(pushNotificationData.getVariationId().hashCode(), notification);
                Log.d(TAG, "Rendered push notification from application: carousel");
                return true;
            }
        }

        return false;
    }
```


Now re-render the carousel notification in onRerender callback every time the user browses the carousel by clicking on left/right arrow as shown below.

```java
	@Override
	public boolean onRerender(Context context, PushNotificationData pushNotificationData, Bundle bundle) {
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
	        // create notification channel id if not yet created
	    }

	    Bundle customData = pushNotificationData.getCustomData();
	    Log.d(TAG, "custom data: " + String.valueOf(customData) + ", extra data: " + String.valueOf(bundle));

	    if (pushNotificationData.getStyle() == WebEngageConstant.STYLE.CAROUSEL_V1) {
	        if ("landscape".equals(pushNotificationData.getCarouselV1Data().getMODE())) {
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
                        .setWhen(when)
                        .build();

                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(pushNotificationData.getVariationId().hashCode(), notification);
                Log.d(TAG, "Re-rendered push notification: carousel");
                return true;
            }
	    }

	    return false;
	}
```


### 4. Carousel Portrait Layout

The following sample shows how to display carousel portrait notification using onRender and onRerender callbacks.

[push_carousel_portrait.xml](https://github.com/WebEngage/android-custom-push-layouts/blob/master/app/src/main/res/layout/push_carousel_portrait.xml)

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
        	if ("portrait".equals(pushNotificationData.getCarouselV1Data().getMODE())) {
                PendingIntent deletePendingIntent = PendingIntentFactory.constructPushDeletePendingIntent(context, pushNotificationData);
                PendingIntent contentPendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, pushNotificationData.getPrimeCallToAction(), true);

                long when = System.currentTimeMillis();

                Bundle browseExtraData = new Bundle();
                browseExtraData.putLong("when", when);
                PendingIntent leftPendingIntent = PendingIntentFactory.constructCarouselBrowsePendingIntent(context, pushNotificationData, 0, "left", "carousel_left", browseExtraData);
                PendingIntent rightPendingIntent = PendingIntentFactory.constructCarouselBrowsePendingIntent(context, pushNotificationData, 0, "right", "carousel_right", browseExtraData);

                // Download all images and cache
                List<CarouselV1CallToAction> ctaList = pushNotificationData.getCarouselV1Data().getCallToActions();
                for (CarouselV1CallToAction cta : ctaList) {
                    DownloadManager.downloadBitmap(cta.getImageURL());
                }

                RemoteViews collapsedView = new RemoteViews(context.getPackageName(), R.layout.push_collapsed);
                collapsedView.setTextViewText(R.id.notificationTitle, pushNotificationData.getTitle());
                collapsedView.setTextViewText(R.id.notificationText, pushNotificationData.getContentText());

                int size = ctaList.size();
                int curr = 0;
                int right = (curr + 1) % size;
                int left = (curr - 1 + size) % size;

                CarouselV1CallToAction currCta = ctaList.get(curr);
                CarouselV1CallToAction leftCta = ctaList.get(left);
                CarouselV1CallToAction rightCta = ctaList.get(right);

                Bitmap leftImg = DownloadManager.getBitmapFromURL(leftCta.getImageURL(), true);
                if (leftImg == null) {
                    leftImg = DownloadManager.getBitmapFromURL(leftCta.getImageURL(), false);
                    if (leftImg == null) {
                        // Image could not be downloaded. Set a placeholder image
                        leftImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.banner_android);
                    }
                }

                Bitmap rightImg = DownloadManager.getBitmapFromURL(rightCta.getImageURL(), true);
                if (rightImg == null) {
                    rightImg = DownloadManager.getBitmapFromURL(rightCta.getImageURL(), false);
                    if (rightImg == null) {
                        // Image could not be downloaded. Set a placeholder image
                        rightImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.banner_android);
                    }
                }

                Bitmap currImg = DownloadManager.getBitmapFromURL(currCta.getImageURL(), true);
                if (currImg == null) {
                    currImg = DownloadManager.getBitmapFromURL(currCta.getImageURL(), false);
                    if (currImg == null) {
                        // Image could not be downloaded. Set a placeholder image
                        currImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.banner_android);
                    }
                }

                PendingIntent currImagePendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, currCta, false);

                RemoteViews carouselView = new RemoteViews(context.getPackageName(), R.layout.push_carousel_portrait);
                carouselView.setTextViewText(R.id.notificationTitle, pushNotificationData.getCarouselV1Data().getBigContentTitle());
                carouselView.setTextViewText(R.id.notificationText, pushNotificationData.getCarouselV1Data().getSummary());
                carouselView.setImageViewBitmap(R.id.carousel_curr_image, currImg);
                carouselView.setOnClickPendingIntent(R.id.carousel_curr_image, currImagePendingIntent);
                carouselView.setImageViewBitmap(R.id.carousel_left_image, leftImg);
                carouselView.setImageViewBitmap(R.id.carousel_right_image, rightImg);
                carouselView.setOnClickPendingIntent(R.id.left, leftPendingIntent);
                carouselView.setOnClickPendingIntent(R.id.right, rightPendingIntent);

                Notification notification = new NotificationCompat.Builder(context, MY_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setCustomContentView(collapsedView)
                        .setCustomBigContentView(carouselView)
                        .setContentIntent(contentPendingIntent)
                        .setDeleteIntent(deletePendingIntent)
                        .build();

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(pushNotificationData.getVariationId().hashCode(), notification);
                Log.d(TAG, "Rendered push notification from application: portrait carousel");
                return true;
            }
        }

        return false;
    }
```


Re-render on left/right arrow clicks.

```java
	@Override
	public boolean onRerender(Context context, PushNotificationData pushNotificationData, Bundle bundle) {
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
	        // create notification channel id if not yet created
	    }

	    Bundle customData = pushNotificationData.getCustomData();
	    Log.d(TAG, "custom data: " + String.valueOf(customData) + ", extra data: " + String.valueOf(bundle));

	    if (pushNotificationData.getStyle() == WebEngageConstant.STYLE.CAROUSEL_V1) {
	        if ("portrait".equals(pushNotificationData.getCarouselV1Data().getMODE())) {
                PendingIntent deletePendingIntent = PendingIntentFactory.constructPushDeletePendingIntent(context, pushNotificationData);
                PendingIntent contentPendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, pushNotificationData.getPrimeCallToAction(), true);

                // Download all images and cache
                List<CarouselV1CallToAction> ctaList = pushNotificationData.getCarouselV1Data().getCallToActions();
                for (CarouselV1CallToAction cta : ctaList) {
                    DownloadManager.downloadBitmap(cta.getImageURL());
                }

                long when = bundle.getLong("when");
                int prevIndex = bundle.getInt("current");
                String navigation = bundle.getString("navigation", "right");
                int size = ctaList.size();
                int curr = 0;
                if (navigation.equals("right")) {
                    curr = (prevIndex + 1) % size;
                } else {
                    curr = (prevIndex - 1 + size) % size;
                }

                int right = (curr + 1) % size;
                int left = (curr - 1 + size) % size;

                Bundle browseExtraData = new Bundle();
                browseExtraData.putLong("when", when);

                PendingIntent leftPendingIntent = PendingIntentFactory.constructCarouselBrowsePendingIntent(context, pushNotificationData, curr, "left", "carousel_left", browseExtraData);
                PendingIntent rightPendingIntent = PendingIntentFactory.constructCarouselBrowsePendingIntent(context, pushNotificationData, curr, "right", "carousel_right", browseExtraData);

                RemoteViews collapsedView = new RemoteViews(context.getPackageName(), R.layout.push_collapsed);
                collapsedView.setTextViewText(R.id.notificationTitle, pushNotificationData.getTitle());
                collapsedView.setTextViewText(R.id.notificationText, pushNotificationData.getContentText());

                CarouselV1CallToAction currCta = ctaList.get(curr);
                CarouselV1CallToAction leftCta = ctaList.get(left);
                CarouselV1CallToAction rightCta = ctaList.get(right);

                Bitmap leftImg = DownloadManager.getBitmapFromURL(leftCta.getImageURL(), true);
                if (leftImg == null) {
                    leftImg = DownloadManager.getBitmapFromURL(leftCta.getImageURL(), false);
                    if (leftImg == null) {
                        // Image could not be downloaded. Set a placeholder image
                        leftImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.banner_android);
                    }
                }

                Bitmap rightImg = DownloadManager.getBitmapFromURL(rightCta.getImageURL(), true);
                if (rightImg == null) {
                    rightImg = DownloadManager.getBitmapFromURL(rightCta.getImageURL(), false);
                    if (rightImg == null) {
                        // Image could not be downloaded. Set a placeholder image
                        rightImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.banner_android);
                    }
                }

                Bitmap currImg = DownloadManager.getBitmapFromURL(currCta.getImageURL(), true);
                if (currImg == null) {
                    currImg = DownloadManager.getBitmapFromURL(currCta.getImageURL(), false);
                    if (currImg == null) {
                        // Image could not be downloaded. Set a placeholder image
                        currImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.banner_android);
                    }
                }

                PendingIntent currImagePendingIntent = PendingIntentFactory.constructPushClickPendingIntent(context, pushNotificationData, currCta, false);

                RemoteViews carouselView = new RemoteViews(context.getPackageName(), R.layout.push_carousel_portrait);
                carouselView.setTextViewText(R.id.notificationTitle, pushNotificationData.getCarouselV1Data().getBigContentTitle());
                carouselView.setTextViewText(R.id.notificationText, pushNotificationData.getCarouselV1Data().getSummary());
                carouselView.setImageViewBitmap(R.id.carousel_curr_image, currImg);
                carouselView.setOnClickPendingIntent(R.id.carousel_curr_image, currImagePendingIntent);
                carouselView.setImageViewBitmap(R.id.carousel_left_image, leftImg);
                carouselView.setImageViewBitmap(R.id.carousel_right_image, rightImg);
                carouselView.setOnClickPendingIntent(R.id.left, leftPendingIntent);
                carouselView.setOnClickPendingIntent(R.id.right, rightPendingIntent);

                Notification notification = new NotificationCompat.Builder(context, MY_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setCustomContentView(collapsedView)
                        .setCustomBigContentView(carouselView)
                        .setContentIntent(contentPendingIntent)
                        .setDeleteIntent(deletePendingIntent)
                        .build();

                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(pushNotificationData.getVariationId().hashCode(), notification);
                Log.d(TAG, "Re-rendered push notification from application: portrait carousel");
                return true;
            }
	    }

	    return false;
	}
```


### 5. Rating Layout

This code snippet shows how to display the Rating notification layout using onRender and onRerender callbacks.

[push_rating.xml](https://github.com/WebEngage/android-custom-push-layouts/blob/master/app/src/main/res/layout/push_rating.xml)

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

Re-render the rating notification in onRerender callback whenever the user selects/changes the rating as shown below.

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
        Log.d(TAG, "Re-rendered push notification: rating");
        return true;
    }

    return false;
}
```

These code samples are simple examples for modifying and rendering custom push notification layouts through WebEngage. You can render even more complex notification layouts as per your requirements.
