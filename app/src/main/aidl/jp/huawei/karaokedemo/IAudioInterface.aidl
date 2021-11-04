package jp.huawei.karaokedemo;


interface IAudioInterface {
    void connect(String deviceId);
    void startPlay();
    void playAudio(inout byte[] buffer);
    void stopPlay();
    void finish();
}