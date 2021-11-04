package jp.huawei.karaokedemo.app

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.IBinder
import jp.huawei.karaokedemo.IAudioInterface
import jp.huawei.karaokedemo.app.model.SetDeviceIdEvent
import jp.huawei.karaokedemo.app.model.TerminateEvent
import org.greenrobot.eventbus.EventBus

class AudioService : Service() {
    private lateinit var audio: AudioTrack
    private val bufferSize = AudioTrack.getMinBufferSize(
        AUDIO_SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    companion object {
        const val AUDIO_SAMPLE_RATE = 44100
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun initAudioPlayer() {
        audio = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.USAGE_MEDIA)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(AUDIO_SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audio.play()
    }

    private val binder = object : IAudioInterface.Stub() {
        override fun connect(deviceId: String) {
            EventBus.getDefault().post(SetDeviceIdEvent(deviceId))
        }

        override fun startPlay() {
            initAudioPlayer()
        }

        override fun playAudio(buffer: ByteArray?) {
            buffer?.let {
                audio.write(buffer, 0, buffer.size)
            }
        }

        override fun stopPlay() {
            audio.stop()
            audio.release()
        }

        override fun finish() {
            EventBus.getDefault().post(TerminateEvent())
            onDestroy()
        }
    }

}