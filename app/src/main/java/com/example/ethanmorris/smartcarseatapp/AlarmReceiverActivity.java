package com.example.ethanmorris.smartcarseatapp;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

public class AlarmReceiverActivity extends AppCompatActivity {
    private static final String TAG = "AlarmReceiverActivity";
    private MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_receiver);

        Button alarmOffButton = (Button) findViewById(R.id.alarmOffButton);
        alarmOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaPlayer.stop();
                finish();
            }
        });

        playAlarm(this, getURI());
    }

    private Uri getURI(){

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null){
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        if (alarmSound == null){
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }

        return alarmSound;
    }

    private void playAlarm(Context context, Uri alarmSoundUri){
        mMediaPlayer = new MediaPlayer();

        try{
            mMediaPlayer.setDataSource(context, alarmSoundUri);
            final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0){
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        }catch (IOException e){
            Log.e(TAG, "Error playing alarm");
            Log.i(TAG, e.toString());
        }
    }
}
