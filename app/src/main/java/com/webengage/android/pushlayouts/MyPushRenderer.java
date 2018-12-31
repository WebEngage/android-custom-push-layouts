package com.webengage.android.pushlayouts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.webengage.sdk.android.PendingIntentFactory;
import com.webengage.sdk.android.actions.render.CallToAction;
import com.webengage.sdk.android.actions.render.CarouselV1CallToAction;
import com.webengage.sdk.android.actions.render.PushNotificationData;
import com.webengage.sdk.android.callbacks.CustomPushRender;
import com.webengage.sdk.android.callbacks.CustomPushRerender;
import com.webengage.sdk.android.utils.WebEngageConstant;

import java.util.List;

public class MyPushRenderer implements CustomPushRender, CustomPushRerender {
    private static final String TAG = MyPushRenderer.class.getSimpleName();

    private static final String MY_CHANNEL_ID = "test-channel-id";
    private static final String MY_CHANNEL_NAME = "test-channel";

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(Context context, String channelId, String channelName, int importance) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(notificationChannel);
            Log.d(TAG, "channel created");
        } else {
            Log.d(TAG, "channel already exists");
        }
    }

    @Override
    public boolean onRender(Context context, PushNotificationData pushNotificationData) {
        if (pushNotificationData == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context, MY_CHANNEL_ID, MY_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        }

        Bundle customData = pushNotificationData.getCustomData();
        Log.d(TAG, "custom data: " + String.valueOf(customData));

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

        // Big picture
        else if (pushNotificationData.getStyle() == WebEngageConstant.STYLE.BIG_PICTURE) {
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
            Log.d(TAG, "Rendered push notification from application: big picture");
            return true;
        }

        // Carousel
        else if (pushNotificationData.getStyle() == WebEngageConstant.STYLE.CAROUSEL_V1) {
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
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setDeleteIntent(deletePendingIntent)
                    .build();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(pushNotificationData.getVariationId().hashCode(), notification);
            Log.d(TAG, "Rendered push notification from application: carousel");
            return true;
        }

        // Rating
        else if (pushNotificationData.getStyle() == WebEngageConstant.STYLE.RATING_V1) {
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

    @Override
    public boolean onRerender(Context context, PushNotificationData pushNotificationData, Bundle bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context, MY_CHANNEL_ID, MY_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        }

        Bundle customData = pushNotificationData.getCustomData();
        Log.d(TAG, "custom data: " + String.valueOf(customData) + ", extra data: " + String.valueOf(bundle));

        // Carousel
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

        // Rating
        else if (pushNotificationData.getStyle() == WebEngageConstant.STYLE.RATING_V1) {
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

                // Here you can use any resource for selected and unselected ratings
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
}
