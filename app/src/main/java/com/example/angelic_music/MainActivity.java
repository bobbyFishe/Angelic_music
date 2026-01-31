package com.example.angelic_music;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private final List<String> trackList = new ArrayList<>();
    private String selectedTrack = "";
    private String currentPlayingTrack = "";
    private TextView name_track;
    private TextView fullTime, currentTime;
    private final Handler handler = new Handler();
    private Runnable updateTimeRunnable;
    private SeekBar bar;
    private int currentTrackIndex = 0;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private BroadcastReceiver closeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finishAffinity();
        }
    };

    @SuppressLint({"MissingInflatedId", "SetTextI18n", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        requestNotificationPermission();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(closeReceiver,
                    new IntentFilter("com.example.angelic_music.closeApp"),
                    Context.RECEIVER_NOT_EXPORTED);
        } else {
            // Для старых версий без флага
            registerReceiver(closeReceiver,
                    new IntentFilter("com.example.angelic_music.closeApp"));
        }

        ImageButton mus = findViewById(R.id.imageButton_music);
        ImageButton mus_play = findViewById(R.id.imageButton_play_pause);
        ImageButton close = findViewById(R.id.imageButton_closeApp);
        ImageButton track_prev = findViewById(R.id.imageButton_trackPrev);
        ImageButton track_next = findViewById(R.id.imageButton_track_next);
        ListView listViewTrack = findViewById(R.id.listView_tracks);
        listViewTrack.setVerticalScrollBarEnabled(true);
        listViewTrack.setScrollbarFadingEnabled(false);
        name_track = findViewById(R.id.textView_name_track);
        name_track.setSelected(true);
        fullTime = findViewById(R.id.editTextTime_fullTime);
        currentTime = findViewById((R.id.editTextTime));
        bar = findViewById(R.id.seekBar2);
        mediaPlayer = new MediaPlayer();
        bar.setEnabled(false);

        track_next.setOnClickListener(view -> {
            playNextTrack();
        });

        track_prev.setOnClickListener(view -> {
            playPrevTrack();
        });

        close.setOnClickListener(view -> {
            showFirstDialog();
        });

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateTimeRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer == null) {
                    return;
                }
                int newPosition = seekBar.getProgress();
                int duration = mediaPlayer.getDuration();
                if(duration <= 0) {
                    return;
                }
                if(newPosition > duration - 1000) {
                    playNextTrack();
                } else {
                    try {
                        mediaPlayer.seekTo(newPosition);
                        currentTime.setText(formatTime(newPosition));
                        boolean wasPlaying = mediaPlayer.isPlaying();
                        if (wasPlaying) {
                            handler.post(updateTimeRunnable);
                        }
                        showQuickToast("Перемотано на " + formatTime(newPosition));

                    } catch (IllegalStateException e) {
                        showQuickToast("Ошибка перемотки");
                    }
                }
            }
        });

        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentTime();
            }
        };


        loadTrackList();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, trackList);
        listViewTrack.setAdapter(adapter);


        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                int duration = mediaPlayer.getDuration();
                currentTime.setText(formatTime(duration));
                bar.setProgress(duration);
                handler.removeCallbacks(updateTimeRunnable);
                playNextTrack();
            }
        });

        listViewTrack.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedTrack = trackList.get(i);

                if (mediaPlayer.isPlaying() && selectedTrack.equals(currentPlayingTrack)) {
                    showQuickToast("Этот трек уже играет");
                    return;
                }
                playMusic(selectedTrack);
                showQuickToast("Играет: " + selectedTrack);
                mus_play.setImageResource(android.R.drawable.ic_media_pause);
            }
        });


        mus.setOnClickListener(view -> {
            if(mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                //updateMusicService(null);  // null = скрыть уведомление
                //stopService(new Intent(this, MusicService.class));


                currentPlayingTrack = "";
                handler.removeCallbacks(updateTimeRunnable);
                currentTime.setText("00:00");
                bar.setProgress(0);
                bar.setMax(0);
                bar.setEnabled(false);
                showQuickToast("Остановил");
                mus_play.setImageResource(android.R.drawable.ic_media_play);
            }
        });

        mus_play.setOnClickListener(view -> {
            if(!mediaPlayer.isPlaying()) {
                mus_play.setImageResource(android.R.drawable.ic_media_pause);
                if(!currentPlayingTrack.isEmpty()) {
                    mediaPlayer.start();
                    updateCurrentTime();
                    showQuickToast("Продолжил");
                } else {
                    try {
                        String mes = "";
                        if (selectedTrack.isEmpty()) {
                            selectedTrack = trackList.get(0);
                            mes = "Запустил " + selectedTrack;
                        }
                        String path = "audio/" + selectedTrack;
                        mediaPlayer.setDataSource(getAssets().openFd(path));
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        bar.setEnabled(true);
                        currentPlayingTrack = selectedTrack;
                        updateMusicService(currentPlayingTrack);
                        name_track.setText(currentPlayingTrack);
                        fullTime.setText(formatTime(mediaPlayer.getDuration()));
                        bar.setMax(mediaPlayer.getDuration());
                        currentTime.setText("00:00");
                        handler.post(updateTimeRunnable);
                        showQuickToast(mes.isEmpty() ? "Перезапустил" : mes);
                    } catch (IOException e) {
                        Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                mediaPlayer.pause();
                handler.removeCallbacks(updateTimeRunnable);
                showQuickToast("Пауза");
                mus_play.setImageResource(android.R.drawable.ic_media_play);
            }
        });

    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void loadTrackList() {
        try {
            String[] files = getAssets().list("audio");
            assert files != null;
            for (String file : files) {
                if(file.endsWith(".mp3")) {
                    trackList.add(file);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SetTextI18n")
    private void playMusic(String trackName) {
        try {
            if (mediaPlayer.isPlaying() && currentPlayingTrack.equals(trackName)) {
                return;
            }

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }

            mediaPlayer.reset();
            String path = "audio/" + trackName;
            mediaPlayer.setDataSource(getAssets().openFd(path));
            mediaPlayer.prepare();
            mediaPlayer.start();
            bar.setEnabled(true);

            currentPlayingTrack = trackName;
            updateMusicService(trackName);
            currentTrackIndex = trackList.indexOf(trackName);
            name_track.setText(currentPlayingTrack);
            int duration = mediaPlayer.getDuration();
            fullTime.setText(formatTime(duration));
            bar.setMax(duration);
            bar.setProgress(0);
            handler.post(updateTimeRunnable);
        } catch (IOException e) {
            showQuickToast("Ошибка загрузки: " + trackName);
        } catch (Exception e) {
            showQuickToast("Ошибка: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(closeReceiver);
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(updateTimeRunnable);
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void showQuickToast(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, null);

        TextView text = layout.findViewById(R.id.toast_message);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setView(layout);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 120);
        toast.show();

        new Handler(Looper.getMainLooper()).postDelayed(toast::cancel, 800);
    }

    private String formatTime(int milliseconds) {
        if (milliseconds <= 0) return "00:00";
        int totalSeconds = milliseconds / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void updateCurrentTime() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            int currentPos = mediaPlayer.getCurrentPosition();
            int duration = mediaPlayer.getDuration();
            if (currentPos > duration) {
                currentPos = duration;
            }

            currentTime.setText(formatTime(currentPos));
            bar.setProgress(currentPos);

            handler.postDelayed(updateTimeRunnable, 1000);
        }
    }

    private void playNextTrack() {
        if (trackList.isEmpty()){
            return;
        }
        currentTrackIndex++;
        if (currentTrackIndex >= trackList.size()) {
            currentTrackIndex = 0;
        }
        String nextTrack = trackList.get(currentTrackIndex);
        selectedTrack = nextTrack;

        playMusic(nextTrack);
        showQuickToast("Следующий трек: " + nextTrack);
    }

    private void playPrevTrack() {
        if(trackList.isEmpty()) return;
        currentTrackIndex--;
        if(currentTrackIndex < 0) {
            currentTrackIndex = trackList.size() - 1;
        }
        String nextTrack = trackList.get(currentTrackIndex);
        selectedTrack = nextTrack;
        playMusic(nextTrack);
        showQuickToast("Предыдущий трек: " + nextTrack);
    }

    private void showFirstDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Подтверждение")
                .setMessage("Хотите выйти?")
                .setCancelable(false)
                .setNegativeButton("Нет", (dialog, which) -> showQuickToast("Отмена"))
                .setPositiveButton("Да", (dialog, which) -> {
                    showQuickToast("Выхожу");
                    stopService(new Intent(MainActivity.this, MusicService.class));
                    finishAffinity();
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showQuickToast("Уведомления разрешены");
            } else {
                showQuickToast("Уведомления запрещены — музыка в фоне не будет работать");
            }
        }
    }

    private void updateMusicService(String trackName) {
        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.putExtra("track_name", trackName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
