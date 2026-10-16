// Copyright 2026 The Cobalt Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package dev.cobalt.coat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

/**
 * A foreground service that keeps the app process alive while media is playing in the background,
 * and shows a Now Playing (media-style) notification with the current title, artist, artwork and
 * play/pause control.
 *
 * <p>Started only when background playback is enabled by the web app (see
 * StarboardBridge.setBackgroundPlaybackEnabled). The notification content is pushed in by
 * CobaltMediaSession via {@link #update}.
 */
public class BackgroundPlaybackService extends Service {
  private static final String TAG = "BackgroundPlayback";
  private static final String CHANNEL_ID = "cobalt_background_playback";
  private static final int NOTIFICATION_ID = 4242;

  // Current media state, mirrored from CobaltMediaSession. Static so the state survives
  // service (re)creation and can be read on the service's main thread.
  private static volatile String sTitle;
  private static volatile String sArtist;
  private static volatile Bitmap sArtwork;
  private static volatile boolean sIsPlaying;
  private static volatile MediaSessionCompat.Token sToken;
  private static volatile boolean sRunning;

  public static void start(Context context) {
    Intent intent = new Intent(context, BackgroundPlaybackService.class);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent);
    } else {
      context.startService(intent);
    }
  }

  public static void stop(Context context) {
    context.stopService(new Intent(context, BackgroundPlaybackService.class));
  }

  /**
   * Updates the Now Playing notification. Called by CobaltMediaSession whenever metadata or
   * playback state changes. Safe to call when the service is not running (state is retained and
   * applied on the next start).
   */
  public static void update(
      Context context,
      String title,
      String artist,
      Bitmap artwork,
      boolean isPlaying,
      MediaSessionCompat.Token token) {
    if (sToken != token) {
      sToken = token;
    }
    sTitle = title;
    sArtist = artist;
    if (artwork != null) {
      sArtwork = artwork;
    }
    sIsPlaying = isPlaying;

    // Only post/refresh the notification while the service is actually in the
    // foreground (i.e. the app is in the background). In the foreground the
    // app's own UI is showing, so a Now Playing notification would be noise.
    NotificationManager manager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (sRunning && manager != null && sTitle != null) {
      manager.notify(NOTIFICATION_ID, buildNotification(context));
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "Background playback service started");
    sRunning = true;
    createNotificationChannel();
    startForeground(NOTIFICATION_ID, buildNotification(this));
    // The service is only here to keep the process alive; no work to re-deliver.
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    sRunning = false;
    super.onDestroy();
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }
    NotificationManager manager = getSystemService(NotificationManager.class);
    if (manager == null) {
      return;
    }
    NotificationChannel channel =
        new NotificationChannel(
            CHANNEL_ID, "Background playback", NotificationManager.IMPORTANCE_LOW);
    channel.setDescription("Keeps media playing while the app is in the background.");
    channel.setShowBadge(false);
    manager.createNotificationChannel(channel);
  }

  private static Notification buildNotification(Context context) {
    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(sTitle != null ? sTitle : "Cobalt")
            .setContentText(sArtist != null ? sArtist : "Playing in the background")
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

    if (sArtwork != null) {
      builder.setLargeIcon(sArtwork);
    }

    // Tapping the notification returns to the app.
    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
    if (launchIntent != null) {
      PendingIntent contentIntent =
          PendingIntent.getActivity(
              context,
              0,
              launchIntent,
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      builder.setContentIntent(contentIntent);
    }

    if (sToken != null) {
      builder.setStyle(
          new MediaStyle()
              .setMediaSession(sToken)
              .setShowActionsInCompactView(0));
    }

    return builder.build();
  }
}
