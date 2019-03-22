package com.fortemedia.yunl.audiotest;

public class DataInfo {

    public DataInfo(int bufferSize, String filename) {
        m_buffer = new byte[bufferSize];
        m_filename = filename;
        m_dataReady = false;
        m_dataSize = 0;
    }

    public byte[] getBuffer() {
        return m_buffer;
    }

    /*
    @len: size in byte
     */
    public void setBuffer(byte[] srcBuffer, int len) {
        System.arraycopy(srcBuffer, 0, m_buffer, 0, len);
        m_dataReady = true;
        m_dataSize = len;
    }

    /*
    @len: size in short
     */
    public void setBuffer(short[] srcBuffer, int len, boolean isBigEndian) {
        if (isBigEndian) {
            for (int i = 0; i < len; i++) {
                m_buffer[i] = (byte) ((srcBuffer[i] >> 8) & 0xff);
                m_buffer[i + 1] = (byte) (srcBuffer[i] & 0xff);
            }
        } else {
            for (int i = 0; i < len; i++) {
                m_buffer[i] = (byte) (srcBuffer[i] & 0xff);
                m_buffer[i + 1] = (byte) ((srcBuffer[i] >> 8) & 0xff);
            }
        }

        m_dataReady = true;
        m_dataSize = len * 2;
    }

    public void fakeData() {
        m_dataReady = true;
        m_dataSize = 0;
    }

    public String getFileName() {
        return m_filename;
    }

    public boolean isDataReady() {
        return m_dataReady;
    }

    /*
    return size in byte
     */
    public int getDataSize() {
        if (!m_dataReady) {
            return 0;
        }
        return m_dataSize;
    }

    public void clearBuffer() {
        m_dataReady = false;
        m_dataSize = 0;
    }

    private byte[] m_buffer;
    private String m_filename;
    private boolean m_dataReady;
    private int m_dataSize;
}
