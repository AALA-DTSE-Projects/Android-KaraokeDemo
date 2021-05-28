package jp.huawei.karaokedemo;


interface IAudioInterface {
    void setLyrics(String lyrics);
    void startPlay();
    void playAudio(out byte[] buffer);
    void stopPlay();
    void finish();
}