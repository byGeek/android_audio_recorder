package com.fortemedia.yunl.audiotest;

import android.media.AudioFormat;

public class AudioConfig {
    public int sampleRate;
    public int configMask;
    public int format;

    public static AudioConfig getDefaultConfig(){
        AudioConfig config = new AudioConfig();
        config.sampleRate = 16000;
        config.configMask = AudioFormat.CHANNEL_IN_STEREO;
        config.format = AudioFormat.ENCODING_PCM_16BIT;
        return config;
    }
}
