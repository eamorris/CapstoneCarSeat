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
import com.google.android.gms.location.LocationServices;
import java.io.IOException;

// The alarm code was modeled after a guide posted on blog.mikesir87.io

public class AlarmReceiverActivity extends AppCompatActivity {
    private static final String TAG = "AlarmReceiverActivity";
    public static MediaPlayer mMediaPlayer;


    // This method simply handles the activity view for the alarm
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


    // This function retrieves the user's default alarm sound to use
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

    // This function actually plays the alarm sound
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
