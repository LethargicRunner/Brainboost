package com.lethargicrunner.brainboost.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import com.lethargicrunner.brainboost.MainActivity
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service de premier plan : génère et joue le son binaural en continu,
 * même écran éteint. S'arrête seul à la fin de la durée réglée (0 = illimité).
 */
class PlaybackService : Service() {

    private val stopping = AtomicBoolean(false)
    private var playbackThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlayback()
            return START_NOT_STICKY
        }
        val beatHz = intent?.getDoubleExtra(EXTRA_BEAT_HZ, 10.0) ?: 10.0
        val carrierHz = intent?.getDoubleExtra(EXTRA_CARRIER_HZ, 100.0) ?: 100.0
        val durationMin = intent?.getIntExtra(EXTRA_DURATION_MIN, 0) ?: 0

        stopping.set(false)
        startAsForeground()
        startPlayback(beatHz, carrierHz, durationMin)
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startPlayback(beatHz: Double, carrierHz: Double, durationMin: Int) {
        playbackThread?.let { stopPlayback() }
        val durationNanos: Long = if (durationMin <= 0) -1L else durationMin * 60L * 1_000_000_000L

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(BinauralGenerator.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(BinauralGenerator.CHUNK_SIZE * 2)
            .build()
        audioTrack = track
        track.play()

        playbackThread = Thread {
            val chunk = ShortArray(BinauralGenerator.CHUNK_SIZE)
            var chunkIndex = 0L
            val start = System.nanoTime()
            while (!stopping.get()) {
                if (durationNanos > 0 && System.nanoTime() - start >= durationNanos) break
                BinauralGenerator.fillStereoChunk(chunk, carrierHz, beatHz, chunkIndex.toDouble())
                track.write(chunk, 0, chunk.size)
                chunkIndex++
            }
            track.stop()
            track.release()
            audioTrack = null
            stopSelf()
        }.also { it.start() }
    }

    private fun stopPlayback() {
        stopping.set(true)
        playbackThread?.let {
            it.interrupt()
            it.join(500)
        }
        playbackThread = null
        audioTrack?.let { runCatching { it.stop() }; runCatching { it.release() } }
        audioTrack = null
        stopSelf()
    }

    override fun onDestroy() {
        stopping.set(true)
        audioTrack?.let { runCatching { it.stop() }; runCatching { it.release() } }
        audioTrack = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Lecture", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, PlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Brainboost")
            .setContentText("Son binaural en lecture — casque stéréo requis")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Arrêter", stopIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "brainboost_playback"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.lethargicrunner.brainboost.STOP"
        const val EXTRA_BEAT_HZ = "beat_hz"
        const val EXTRA_CARRIER_HZ = "carrier_hz"
        const val EXTRA_DURATION_MIN = "duration_min"

        fun start(context: Context, beatHz: Double, carrierHz: Double, durationMin: Int) {
            val intent = Intent(context, PlaybackService::class.java)
                .putExtra(EXTRA_BEAT_HZ, beatHz)
                .putExtra(EXTRA_CARRIER_HZ, carrierHz)
                .putExtra(EXTRA_DURATION_MIN, durationMin)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, PlaybackService::class.java).setAction(ACTION_STOP))
        }
    }
}
