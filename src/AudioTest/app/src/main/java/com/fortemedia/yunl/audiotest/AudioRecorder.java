package com.fortemedia.yunl.audiotest;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;


public class AudioRecorder extends Thread {

    private final static String TAG = "AudioRecorder";

    private DataInfo m_info;
    private AudioRecord m_audioRecord;
    private boolean m_continue;
    private AudioConfig m_audioConfig;
    private int m_bufferSize;
    private short[] m_buffer;//todo: short?

    public AudioRecorder(AudioConfig config, DataInfo info) {
        m_audioConfig = config;
        m_info = info;
        m_continue = true;

        m_bufferSize = AudioRecord.getMinBufferSize(m_audioConfig.sampleRate, m_audioConfig.configMask, m_audioConfig.format);
        m_buffer = new short[m_bufferSize / 2];
    }

    public boolean CreateAudioRecord() {
        m_audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                m_audioConfig.sampleRate,
                m_audioConfig.configMask,
                m_audioConfig.format,
                m_bufferSize);

        if (m_audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            return true;
        } else {
            return false;
        }
    }

    public void stopAction() {
        m_continue = false;
        //this.join();
    }

    @Override
    public void run() {

        if (m_audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalArgumentException("AudioRecord not initialized!");
        }

        m_audioRecord.startRecording();

        while (m_continue) {
            int perRead = m_audioRecord.read(m_buffer, 0, m_buffer.length);

            try {
                synchronized (m_info) {
                    while (m_info.isDataReady()) {
                        Log.i(TAG, "wait for writer to consume data.");
                        m_info.wait();  //wait AudioWriter to finish writing to disk
                    }

                    Log.i(TAG, "produce data to buffer");
                    m_info.setBuffer(m_buffer, perRead, false);
                    m_info.notify();
                }

            } catch (InterruptedException ie) {
                ie.printStackTrace();
                assert (false);
            }
        }

        m_audioRecord.stop();
        m_audioRecord.release();
        m_audioRecord = null;

        synchronized (m_info) {
            m_info.fakeData();  //wake up other thread
            m_info.notifyAll();
        }


    }
}


