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
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * A minimal foreground service that keeps the app process alive while media is
 * playing in the background. Started only when background playback is enabled
 * by the web app (see StarboardBridge.setBackgroundPlaybackEnabled).
 */
public class BackgroundPlaybackService extends Service {
  private static final String TAG = "BackgroundPlayback";
  private static final String CHANNEL_ID = "cobalt_background_playback";
  private static final int NOTIFICATION_ID = 4242;

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

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "Background playback service started");
    createNotificationChannel();
    startForeground(NOTIFICATION_ID, buildNotification());
    // The service is only here to keep the process alive; no work to re-deliver.
    return START_NOT_STICKY;
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
    manager.createNotificationChannel(channel);
  }

  private Notification buildNotification() {
    Notification.Builder builder =
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);
    return builder
        .setContentTitle("Cobalt")
        .setContentText("Playing in the background")
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setOngoing(true)
        .build();
  }
}
