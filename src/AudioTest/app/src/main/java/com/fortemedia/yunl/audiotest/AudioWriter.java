package com.fortemedia.yunl.audiotest;


import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioWriter extends Thread {

    private final static String TAG = "AudioWriter";


    private DataInfo m_info;
    private boolean m_continue;

    public AudioWriter(DataInfo info) {
        m_info = info;
        m_continue = true;
    }

    public void stopAction() {
        m_continue = false;
    }

    @Override
    public void run() {

        String filename = m_info.getFileName();
        if (filename.equals("") || filename == null) {
            throw new IllegalArgumentException("filename is empty!");
        }

        //todo: file exist ? create directory
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        try {
            fos = new FileOutputStream(filename);
            bos = new BufferedOutputStream(fos);

            byte[] buffer = m_info.getBuffer();
            int offset = 0;

            try {
                while (m_continue) {
                    synchronized (m_info) {
                        //wait for data ready
                        while (!m_info.isDataReady()) {
                            Log.i(TAG, "wait for producer data");
                            m_info.wait();
                        }

                        if (m_info.getDataSize() != 0) {
                            Log.i(TAG, "Got data, write to file");

                            bos.write(buffer, 0, m_info.getDataSize());
                            //offset += m_info.getDataSize();
                        }
                        m_info.clearBuffer();  //data is consumed
                        m_info.notify();
                    }
                }

                bos.flush();

                bos.close();
                fos.close();


            } catch (InterruptedException ie) {
                ie.printStackTrace();
                assert (false);
            }

        } catch (IOException e) {
            e.printStackTrace();
            assert (false);
        }

    }
}
