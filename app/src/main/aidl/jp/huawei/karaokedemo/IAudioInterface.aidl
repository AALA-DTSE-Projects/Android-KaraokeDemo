package jp.huawei.karaokedemo;


interface IAudioInterface {
    void setLyrics(String lyrics);
    void startPlay();
    void playAudio(inout byte[] buffer);
    void stopPlay();
    void finish();
}