package com.fortemedia.yunl.audiotest;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.*;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedOutputStream;
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
    private Button m_btnPlayStart = null;
    private Button m_btnStopPlay = null;

    private AudioRecord m_audioRecord = null;


    /*
    constant here
     */
    private static final int SAMPLE_RATE = 16000;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int CHANNEL_CONFIG_MASK = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSIONS_REQUEST_READ_STORAGE = 2;
    private static final int PERMISSIONS_REQUEST_WRITE_STORAGE = 3;

    private static final int ALERT_DIALOG = 1000;

    private static final String TAG = "MainActivity";

    private volatile boolean m_shoudContinue = true;

    //private Thread m_recordThread = null;
    private AudioWriter m_writeThread = null;
    private AudioRecorder m_recorder = null;

    private String m_filepath = null;

    private Handler m_handler = null;

    //todo: deal with app life cycle, such as onResume, onStop

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hookControl();
        wireEvent();

        checkPermession();

        setBtnState(true);

    }

    private void setupHandler() {
        m_handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                //todo: handle message
                //if(msg.what == )
            }
        };
    }

    private void checkPermession() {
        //request multiple permission at one-time
        /*
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_STORAGE);
        }
        */

        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (!hasPermission(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    public static boolean hasPermission(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    return;
                } else {
                    showAlertDialog("Error", "Record audio permission not Granted!", () -> exitActivity());
                }
            }

            /*
            case PERMISSIONS_REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isExternalStorageWritable()) {
                        return;
                    } else {
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
                    } else {
                        showAlertDialog("Error", "Record audio permission not Granted!", () -> exitActivity());
                    }
                }

            }

            */
        }
    }

    private void hookControl() {
        m_btn_start = (Button) findViewById(R.id.btnStart);
        m_btn_stop = (Button) findViewById(R.id.btnStop);
        m_txtFilepath = (TextView) findViewById(R.id.txtFilepath);

        m_btnPlayStart = (Button)findViewById(R.id.btnPlayStart);
        m_btnStopPlay = (Button)findViewById(R.id.btnStopPlay);
    }

    AudioPlayer m_audioPlayer = null;

    private void wireEvent() {
        m_btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                m_shoudContinue = true;

                String filename = getFileName();
                if (filename == null) {
                    return;  //todo: show alert dialog
                }
                Log.i(TAG, "filename: " + filename);
                m_filepath = filename;


                //start thread and use handler to pass alert messages

                int sampleRate = SAMPLE_RATE;
                int audioConfig = CHANNEL_CONFIG_MASK;
                int audioFormat = AUDIO_FORMAT;

                int buffersize = AudioRecord.getMinBufferSize(sampleRate, audioConfig, audioFormat);

                DataInfo info = new DataInfo(buffersize, filename);

                if (m_recorder == null) {
                    AudioConfig config = new AudioConfig();
                    config.sampleRate = sampleRate;
                    config.configMask = audioConfig;
                    config.format = audioFormat;

                    m_recorder = new AudioRecorder(config, info);

                    if (!m_recorder.CreateAudioRecord()) {
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
                }

                m_recorder.start();

                m_writeThread = new AudioWriter(info);
                m_writeThread.start();

                /*
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        record();
                    }
                });
                thread.start();
                */

                setBtnState(false);
            }
        });

        m_btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_shoudContinue = false;

                m_recorder.stopAction();
                m_writeThread.stopAction();

                try {
                    m_recorder.join();
                    m_writeThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    assert (false);
                }

                m_recorder = null;
                m_writeThread = null;

                if (m_filepath != null && m_filepath != "") {
                    m_txtFilepath.setText(m_filepath);
                }
                setBtnState(true);

            }
        });

        m_btnPlayStart.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //todo: when in recording, disable play

                if(m_audioPlayer == null) {
                    AudioConfig config = AudioConfig.getDefaultConfig();

                    //todo: just for test
                    String filename = "/sdcard/fortemedia/test_2019_03_22_15_42_53.pcm";
                    m_audioPlayer = new AudioPlayer(filename, config);
                    m_audioPlayer.setCompleteListener(new TaskListener() {
                        @Override
                        public void completeCallback(Object obj) {
                            m_audioPlayer = null;
                        }
                    });
                    m_audioPlayer.play();
                }
            }
        });

        m_btnStopPlay.setOnClickListener((v)-> {
            if(m_audioPlayer != null){
                m_audioPlayer.stop();
                m_audioPlayer = null;
            }
        });
    }

    private void setBtnState(boolean flag) {
        m_btn_start.setEnabled(flag);
        m_btn_stop.setEnabled(!flag);
    }

    private void showAlertDialog(String title, String message, final Runnable toRun) {
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

    private void exitActivity() {
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 21) {
            finishAffinity();
        } else if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        }
    }

    private void record() {

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_MASK, AUDIO_FORMAT);
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG_MASK,
                AUDIO_FORMAT,
                bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            assert (false);
        }

        short[] buffer = new short[bufferSize / 2];
        byte[] tempBuffer = new byte[bufferSize];


        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;

        String filename = getFileName();
        if (filename == "") {
            assert (false);
        }

        m_filepath = filename;

        try {

            fileOutputStream = new FileOutputStream(filename);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);


            audioRecord.startRecording();

            ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
            while (m_shoudContinue) {
                int perRead = audioRecord.read(buffer, 0, bufferSize / 2);


                for (int i = 0; i < perRead; i++) {
                    tempBuffer[2*i] = (byte) (buffer[i] & 0xff);
                    tempBuffer[2*i + 1] = (byte) ((buffer[i] >> 8) & 0xff);
                }

                bufferedOutputStream.write(tempBuffer, 0, perRead*2);

                /*
                byteBuffer.rewind();
                for (int i = 0; i < perRead; i++) {
                    byteBuffer.put((byte) (buffer[i] & 0xff));
                    byteBuffer.put((byte) ((buffer[i] >> 8) & 0xff));
                }

                bufferedOutputStream.write(byteBuffer.array(), 0, 2*perRead);
                */

            }

            audioRecord.stop();
            audioRecord.release();

            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            fileOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
            assert (false);
        }


        //while(m_shoudContinue)
    }

    private String getFileName() {

        String filename = "";

        Date currentTime = Calendar.getInstance().getTime();
        String suffix = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(currentTime);
        filename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/fortemedia/";

        boolean success = true;
        File folder = new File(filename);
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (!success) {
            Log.e(TAG, "Create folder failed!");
            return null;
        }

        filename += "test_" + suffix + ".pcm";
        return filename;
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
