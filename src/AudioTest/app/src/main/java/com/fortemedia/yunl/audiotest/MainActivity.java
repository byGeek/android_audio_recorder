package com.fortemedia.yunl.audiotest;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    /*
    control here
     */

    private Button m_btn_start = null;
    private Button m_btn_stop = null;
    private TextView m_txtFilepath = null;

    private AudioRecord m_audioRecord = null;


    /*
    constant here
     */
    private static final int SAMPLE_RATE = 16000;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int CHANNEL_CONFIG_MASK = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSIONS_REQUEST_READ_STORAGE =2;
    private static final int PERMISSIONS_REQUEST_WRITE_STORAGE =3;

    private static final String TAG = "MainActivity";

    private volatile boolean m_shoudContinue = true;
    private String m_filepath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hookControl();
        wireEvent();



        setBtnState(true);

    }

    private void checkPermession(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            //ask for permission

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_RECORD_AUDIO:
            {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return;
                }else{
                    showAlertDialog("Error", "Record audio permission not Granted!", () -> exitActivity());
                }
            }
            case PERMISSIONS_REQUEST_WRITE_STORAGE:
            {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(isExternalStorageWritable()){
                        return;
                    }else{
                        showAlertDialog("Error", "Record audio permission not Granted!", () -> exitActivity());
                    }
                }
            }
            case PERMISSIONS_REQUEST_READ_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (isExternalStorageReadable()) {
                        return;
                    }else{
                        showAlertDialog("Error", "Record audio permission not Granted!", () -> exitActivity());
                    }
                }

            }
        }
    }

    private void hookControl(){
        m_btn_start = (Button)findViewById(R.id.btnStart);
        m_btn_stop = (Button)findViewById(R.id.btnStop);
        m_txtFilepath = (TextView)findViewById(R.id.txtFilepath);
    }

    private void wireEvent(){
        m_btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                m_shoudContinue = true;

                int sampleRate = SAMPLE_RATE;
                int audioConfig = CHANNEL_CONFIG_MASK;
                int audioFormat = AUDIO_FORMAT;

                int buffersize = AudioRecord.getMinBufferSize(sampleRate, audioConfig, audioFormat);
                if(!CreateAudioRecord(sampleRate, audioConfig, audioFormat, buffersize)){
                    Log.e(TAG, "CreateAudioRecord failed!");
                    //todo: use DialogFragment
                    showAlertDialog("Error", "Failed to aquire audio record!", new Runnable() {
                        @Override
                        public void run() {
                            exitActivity();
                        }
                    });
                    return;
                }
                //start record
                recordAudio(buffersize);
                setBtnState(false);
            }
        });

        m_btn_stop.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                m_shoudContinue = false;
                setBtnState(true);

                if(m_filepath != null && m_filepath != "") {
                    m_txtFilepath.setText(m_filepath);
                }
            }
        });
    }

    private void setBtnState(boolean flag) {
        m_btn_start.setEnabled(flag);
        m_btn_stop.setEnabled(!flag);
    }

    private  boolean CreateAudioRecord(int sampleRate, int audioConfig, int audioFormat, int buffersize){
        if(m_audioRecord != null){
            m_audioRecord.stop();
            m_audioRecord.release();
            m_audioRecord = null;
        }
        m_audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate,
                audioConfig,
                audioFormat,
                buffersize);

        if(m_audioRecord.getState() == AudioRecord.STATE_INITIALIZED){
            return true;
        }else{
            return false;
        }
    }

    private void showAlertDialog(String title, String message, final Runnable toRun){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        toRun.run();
                    }
                });

        alertDialog.show();
    }

    private void exitActivity(){
        if(Build.VERSION.SDK_INT>=16 && Build.VERSION.SDK_INT<21){
            finishAffinity();
        } else if(Build.VERSION.SDK_INT>=21){
            finishAndRemoveTask();
        }
    }

    private void recordAudio(final int buffersize){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(m_audioRecord == null || m_audioRecord.getState() != AudioRecord.STATE_INITIALIZED){
                    Log.e(TAG, "AudioRecord not initilized, return!");
                    return;
                }

                short[] audioBuffer = new short[buffersize/2];

                m_audioRecord.startRecording();
                Log.i(TAG, "start recording.");


                //todo: use Producer-consumer pattern
                Date currentTime = Calendar.getInstance().getTime();
                String suffix = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss").format(currentTime);
                m_filepath = Environment.getExternalStorageDirectory().getAbsolutePath()  + "/fortemedia/";

                boolean success = true;
                File folder = new File(m_filepath);
                if(!folder.exists()){
                    success = folder.mkdirs();
                }
                if(!success){
                    Log.e(TAG, "Create folder failed!");
                    return;
                }

                m_filepath += "test_" + suffix + ".pcm";

                FileOutputStream os = null;

                try{
                    //os = openFileOutput(m_filepath, Context.MODE_PRIVATE);
                    os = new FileOutputStream(m_filepath);
                }catch(Exception e){
                    Log.e(TAG, "openFileOutput failed");
                    e.printStackTrace();
                }

                if(os == null){
                    return;
                }

                ByteBuffer bytebuf = ByteBuffer.allocate(buffersize);

                int totalRead = 0;
                while(m_shoudContinue){
                    int perRead = m_audioRecord.read(audioBuffer, 0, audioBuffer.length);

                    int i = 0;
                    bytebuf.clear();
                    byte low = 0;
                    byte high = 0;
                    while(i< perRead){

                        //wave file: litter endian

                        high = (byte)((audioBuffer[i] >> 8) & 0xff);
                        low = (byte)(audioBuffer[i] & 0xff);
                        bytebuf.put(low);
                        bytebuf.put(high);
                        ++i;
                    }

                    try{
                        os.write(bytebuf.array(), 0, perRead*2);
                    }catch (IOException e){
                        Log.e(TAG, "Write file failed.");
                        e.printStackTrace();
                        break;
                    }

                    totalRead += perRead;

                }

                m_audioRecord.stop();
                m_audioRecord.release();
                m_audioRecord = null;

                try{
                    os.close();
                }catch (IOException e){
                    Log.e(TAG, "Close file failed.");
                    e.printStackTrace();
                }


                Log.i(TAG, String.format("recording stopped, samples read: %d", totalRead));
            }
        }).start();
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}
