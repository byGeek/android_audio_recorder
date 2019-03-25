package com.fortemedia.yunl.audiotest;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class AudioPlayer {

    private static final String TAG = "AudioPlayer";
    private boolean m_continue = true;

    private String m_filename;
    private AudioConfig m_config;
    private TaskListener m_taskListener;

    public AudioPlayer(String filename, AudioConfig config) {
        m_filename = filename;
        m_config = config;
    }

    public String getFileName() {
        return m_filename;
    }

    public void setCompleteListener(TaskListener listener){
        m_taskListener = listener;
    }

    public void stop() {
        m_continue = false;
    }

    public void play() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int bufferSize = AudioTrack.getMinBufferSize(m_config.sampleRate,
                        m_config.configMask, m_config.format);

                if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
                    bufferSize = m_config.sampleRate * 2;
                }

                AudioTrack audioTrack = new AudioTrack(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                        new AudioFormat.Builder().setEncoding(m_config.format)
                                .setChannelMask(m_config.configMask).setSampleRate(m_config.sampleRate)
                                .build(),
                        bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);


                audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                    @Override
                    public void onMarkerReached(AudioTrack track) {
                        Log.v(TAG, "Audio file play ended.");
                        //track.release();
                        //track = null;
                        stop();

                    }

                    @Override
                    public void onPeriodicNotification(AudioTrack track) {

                    }
                });




                FileInputStream fileInputStream = null;
                BufferedInputStream bufferedInputStream = null;

                short[] audioBuffer = new short[bufferSize / 2];
                byte[] byteBuffer = new byte[bufferSize];

                try {
                    fileInputStream = new FileInputStream(m_filename);
                    bufferedInputStream = new BufferedInputStream(fileInputStream);


                    long fileSizeInBytes = fileInputStream.getChannel().size();
                    int bitsPerSample = 16;
                    /*
                    long duration = fileSizeInBytes / (m_config.sampleRate * getChannels(m_config.configMask)
                            * getBytesPerSample(m_config.format));
                            * */

                    int totalSamples = (int)fileSizeInBytes/(getChannels(m_config.configMask)* getBytesPerSample(m_config.format));

                    //audioTrack.setPositionNotificationPeriod(m_config.sampleRate / 10);  //10 times for one second
                    //audioTrack.setNotificationMarkerPosition(totalSamples);

                    audioTrack.play();  //start play audio action

                    Log.i(TAG, "start playing audio!");

                    while (m_continue) {
                        int readCount = bufferedInputStream.read(byteBuffer, 0, bufferSize);
                        if(readCount == -1){
                            Log.i(TAG, ("end of file!"));
                            m_continue = false;
                            break;
                        }

                        for (int i = 0, j = 0; i < readCount; i += 2, j++) {
                            audioBuffer[j] = (short) (byteBuffer[i] + (byteBuffer[i + 1] << 8));
                        }

                        audioTrack.write(audioBuffer, 0, readCount / 2);
                    }

                    bufferedInputStream.close();
                    fileInputStream.close();

                    if (audioTrack != null) {
                        audioTrack.release();
                        audioTrack = null;
                    }

                    notifyCompleteListener(null);

                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    assert (false);
                }
                //audioTrack.

            }
        }).start();
    }

    public static int getBytesPerSample(int format) {
        if (format == AudioFormat.ENCODING_PCM_8BIT) {
            return 1;
        } else if (format == AudioFormat.ENCODING_PCM_16BIT) {
            return 2;
        } else {
            Log.i(TAG, "Audio format not supported!");
            assert (false);
            return -1;
        }
    }

    public static int getChannels(int channelMask) {
        if (channelMask == AudioFormat.CHANNEL_IN_MONO) {
            return 1;
        } else if (channelMask == AudioFormat.CHANNEL_IN_STEREO) {
            return 2;
        } else {
            Log.i(TAG, "Audio channel config not supported yet!");
            assert (false);
            return -1;
        }
    }

    private void notifyCompleteListener(Object object){
        if(m_taskListener != null){
            m_taskListener.completeCallback(object);
        }
    }
}
