package com.example.angelic_music;


import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private ListView listViewTrack;
    private List<String> trackList = new ArrayList<>();
    private String selectedTrack = "";
    private String currentPlayingTrack = "";
    private ImageButton mus, mus_play, mus_pause;
    private TextView name_track;
    private TextView fullTime, currentTime;
    private Handler handler = new Handler();
    private Runnable updateTimeRunnable;
    private SeekBar bar;
    private int currentTrackIndex = -1;
    private boolean isFirstLaunch = true;

    @SuppressLint("MissingInflatedId")
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
        mus = findViewById(R.id.imageButton_music);
        mus_play = findViewById(R.id.imageButton_music_play);
        mus_pause = findViewById(R.id.imageButton_pause);
        name_track = findViewById(R.id.textView_name_track);
        name_track.setSelected(true);
        fullTime = findViewById(R.id.editTextTime_fullTime);
        currentTime = findViewById((R.id.editTextTime));
        bar = findViewById(R.id.seekBar2);
        mediaPlayer = new MediaPlayer();
        bar.setEnabled(false);

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
                    if (newPosition > duration) {
                        newPosition = duration;
                        seekBar.setProgress(newPosition);
                    }
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

        listViewTrack = findViewById(R.id.listView_tracks);
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
            }
        });


        mus.setOnClickListener(view -> {
            if(mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                currentPlayingTrack = "";
                handler.removeCallbacks(updateTimeRunnable);
                currentTime.setText("00:00");
                bar.setProgress(0);
                bar.setMax(0);
                bar.setEnabled(false);
                showQuickToast("Остановил");
            }
        });

        mus_pause.setOnClickListener(view -> {
            if(mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                handler.removeCallbacks(updateTimeRunnable);
                showQuickToast("Пауза");
            }
        });

        mus_play.setOnClickListener(view -> {
            if(!mediaPlayer.isPlaying()) {
                if(!currentPlayingTrack.isEmpty()) {
                    mediaPlayer.start();
                    updateCurrentTime();
                    showQuickToast("Продолжил");
                } else {
                    try {
                        if (selectedTrack.isEmpty()) {
                            selectedTrack = trackList.get(0);
                        }
                        String path = "audio/" + selectedTrack;
                        mediaPlayer.setDataSource(getAssets().openFd(path));
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        bar.setEnabled(true);
                        currentPlayingTrack = selectedTrack;
                        fullTime.setText(formatTime(mediaPlayer.getDuration()));
                        bar.setMax(mediaPlayer.getDuration());
                        currentTime.setText("00:00");
                        handler.post(updateTimeRunnable);
                        showQuickToast("Перезапустил");
                    } catch (IOException e) {
                        Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }
                isFirstLaunch = false;
            }
        });

    }

    private void loadTrackList() {
        try {
            String[] files = getAssets().list("audio");
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
            currentTrackIndex = trackList.indexOf(trackName);
            name_track.setText(currentPlayingTrack);
            int duration = mediaPlayer.getDuration();
            fullTime.setText(formatTime(duration));
            bar.setMax(duration);
            bar.setProgress(0);
            handler.post(updateTimeRunnable);
            isFirstLaunch = false;
        } catch (IOException e) {
            showQuickToast("Ошибка загрузки: " + trackName);
        } catch (Exception e) {
            showQuickToast("Ошибка: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
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
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.show();

        new Handler().postDelayed(toast::cancel, 800);
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

            currentTime.setText(formatTime(currentPos));
            bar.setProgress(currentPos);

            if (currentPos < duration) {
                handler.postDelayed(updateTimeRunnable, 1000);
            } else {
                currentTime.setText(formatTime(duration));
                bar.setProgress(duration);

                mediaPlayer.pause();
                handler.removeCallbacks(updateTimeRunnable);
            }
        }
    }

    private void playNextTrack() {
        if (trackList.isEmpty()) return;
        currentTrackIndex++;
        if (currentTrackIndex >= trackList.size()) {
            currentTrackIndex = 0;
        }
        String nextTrack = trackList.get(currentTrackIndex);
        selectedTrack = nextTrack;

        playMusic(nextTrack);
        showQuickToast("Следующий трек: " + nextTrack);
    }
}
