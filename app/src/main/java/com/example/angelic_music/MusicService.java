package com.example.angelic_music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class MusicService extends Service {

    private static final String CHANNEL_ID = "music_channel";
    private static final int NOTIFICATION_ID = 1;
    private boolean isPlaying = false;
    private String currentTrackName = "Плеер в фоне";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification(currentTrackName, isPlaying));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Музыкальный плеер",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Уведомления о воспроизведении");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String trackName, boolean playing) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        // Используем переданное имя трека или значение по умолчанию
        String displayText = (trackName != null && !trackName.isEmpty()) ? trackName : currentTrackName;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                //.setContentTitle("Angelic Music")  // Добавлен заголовок
                .setContentText(displayText)
                .setLargeIcon(largeIcon)
                .setColor(Color.parseColor("#461134"))
                .setColorized(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        // Предыдущий трек
        Intent prevTrack = new Intent(this, MusicService.class);
        prevTrack.setAction("prevTrack");
        PendingIntent prevPendingInt = PendingIntent.getService(this, 0, prevTrack,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(
                android.R.drawable.ic_media_previous,
                "Предыдущий",
                prevPendingInt
        );

        // Play/Pause
        Intent playPause = new Intent(this, MusicService.class);
        playPause.setAction("playPause");
        int playPauseIcon = playing ?
                android.R.drawable.ic_media_pause :
                android.R.drawable.ic_media_play;
        String playPauseText = playing ? "Пауза" : "Воспроизвести";
        PendingIntent playPausePendingInt = PendingIntent.getService(this, 1, playPause,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(
                playPauseIcon,
                playPauseText,
                playPausePendingInt
        );

        // Следующий трек
        Intent nextInt = new Intent(this, MusicService.class);
        nextInt.setAction("nextTrack");
        PendingIntent nextPendingInt = PendingIntent.getService(this, 2, nextInt,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(
                android.R.drawable.ic_media_next,
                "Следующий",
                nextPendingInt
        );

        Intent empty = new Intent(this, MusicService.class);
        PendingIntent emptyPendingInt = PendingIntent.getService(this, 3, empty,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        builder.addAction(
                R.drawable.empty,
                "Пустая",
                emptyPendingInt
        );

        // Закрыть
        Intent closeInt = new Intent(this, MusicService.class);
        closeInt.setAction("closeApp");
        PendingIntent closePendingInt = PendingIntent.getService(this, 4, closeInt,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Закрыть",
                closePendingInt
        );

        // Исправлен MediaStyle - только 4 действия (индексы 0,1,2,3)
        androidx.media.app.NotificationCompat.MediaStyle mediaStyle =
                new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2, 3, 4);
        builder.setStyle(mediaStyle);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Intent broadcastIntent;

            switch (action) {
                case "closeApp":
                    stopForeground(true);
                    broadcastIntent = new Intent("com.example.angelic_music.closeApp");
                    sendBroadcast(broadcastIntent);
                    stopSelf();
                    return START_NOT_STICKY;

                case "nextTrack":
                    broadcastIntent = new Intent("com.example.angelic_music.nextTrack");
                    sendBroadcast(broadcastIntent);
                    return START_NOT_STICKY;

                case "prevTrack":
                    broadcastIntent = new Intent("com.example.angelic_music.prevTrack");
                    sendBroadcast(broadcastIntent);
                    return START_NOT_STICKY;

                case "playPause":
                    // Переключаем состояние воспроизведения
                    isPlaying = !isPlaying;
                    broadcastIntent = new Intent("com.example.angelic_music.playPause");
                    broadcastIntent.putExtra("is_playing", isPlaying);
                    sendBroadcast(broadcastIntent);
                    // Обновляем уведомление с новым состоянием
                    updateNotification(isPlaying);
                    return START_NOT_STICKY;
            }
        }

        // Получаем данные из intent
        if (intent != null) {
            String trackName = intent.getStringExtra("track_name");
            boolean playing = intent.getBooleanExtra("is_playing", isPlaying);

            if (trackName != null) {
                currentTrackName = trackName;
            }
            isPlaying = playing;
        }

        // Обновляем уведомление
        startForeground(NOTIFICATION_ID, createNotification(currentTrackName, isPlaying));
        return START_STICKY;
    }

    private void updateNotification(boolean playing) {
        isPlaying = playing;
        Notification notification = createNotification(currentTrackName, isPlaying);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    public void updateTrackInfo(String trackName, boolean playing) {
        this.currentTrackName = (trackName != null && !trackName.isEmpty()) ? trackName : "Плеер в фоне";
        this.isPlaying = playing;
        updateNotification(playing);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopForeground(true);
        stopSelf();
    }
}