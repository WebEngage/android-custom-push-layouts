package com.webengage.android.pushlayouts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.HttpResponseCache;
import android.util.Log;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

public class DownloadManager {
    private static final String TAG = DownloadManager.class.getSimpleName();
    private static final long ONE_DAY = 24 * 60 * 60 * 1000;
    
    /**
     * This input stream extension is required to decode png images to bitmap
     */
    private static class FlushedInputStream extends FilterInputStream {
        FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int b = read();
                    if (b < 0) {
                        break; // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }

    public static void createHttpCache(Context context) {
        try {
            File httpCacheDir = new File(context.getCacheDir(), "http");
            long httpCacheSize = 10 * 1024 * 1024;  // 10 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.e(TAG, "HTTP response cache installation failed", e);
        }
    }

    public static void cleanHttpCache(Context context, int olderThanDays) {
        try {
            File httpCacheDir = new File(context.getCacheDir(), "http");
            if (httpCacheDir.exists() && httpCacheDir.isDirectory()) {
                File[] files = httpCacheDir.listFiles();
                for (File file : files) {
                    if (file != null) {
                        long lastModified = file.lastModified();
                        if (lastModified > 0) {
                            Date lastMDate = new Date(lastModified);
                            Date today = new Date(System.currentTimeMillis());
                            long diff = today.getTime() - lastMDate.getTime();
                            long diffDays = diff / ONE_DAY;
                            if (olderThanDays < diffDays) {
                                file.delete();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "HTTP response cache installation failed", e);
        }
    }

    public static void flushHttpCache() {
        try {
            HttpResponseCache cache = HttpResponseCache.getInstalled();
            if (cache != null) {
                cache.flush();
            }
            Log.d(TAG, "Flushed Http Cache");
        } catch (Exception e) {
            Log.e(TAG, "HTTP response cache installation failed", e);
        }
    }

    public static Bitmap getBitmapFromURL(String src, boolean fromCacheOnly) {
        Log.d(TAG, "Image requested: " + src);
        InputStream input = null;
        FlushedInputStream flushedInputStream = null;
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(true);
            int maxStale = 60 * 60 * 24 * 3;  // 2 days
            connection.addRequestProperty("Cache-Control", "max-stale=" + maxStale);
            connection.setDoInput(true);
            if (fromCacheOnly) {
                connection.setRequestProperty("Cache-Control", "only-if-cached");
                HttpResponseCache responseCache = HttpResponseCache.getInstalled();
                if (responseCache != null) {
                    URI uri = new URI(src);
                    CacheResponse cacheResponse = responseCache.get(uri, "GET", connection.getRequestProperties());
                    if (cacheResponse != null) {
                        input = cacheResponse.getBody();
                    }
                } else {
                    Log.e(TAG, "Http cache not created");
                }
            } else {
                connection.connect();
                Log.d(TAG, "status response code: " + connection.getResponseCode());
                input = connection.getInputStream();
            }

            if (input == null) {
                return null;
            }

            flushedInputStream = new FlushedInputStream(input);

            BitmapFactory.Options bmOptions;
            bmOptions = new BitmapFactory.Options();
            bmOptions.inSampleSize = 1;

            Bitmap myBitmap = BitmapFactory.decodeStream(flushedInputStream, null, bmOptions);
            Log.d(TAG, "Downloaded image is null: " + (myBitmap == null));
            return myBitmap;
        } catch (IOException e) {
            Log.e(TAG, "Exception while loading image: " + src, e);
            return null;
        } catch (URISyntaxException e) {
            Log.e(TAG, "Exception while creating URI from: " + src, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception while downloading bitmap from: " + src, e);
            return null;
        }
        finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (flushedInputStream != null) {
                try {
                    flushedInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void downloadBitmap(String src) {
        Log.d(TAG, "Downloading image: " + src);
        getBitmapFromURL(src, false);
    }
}
