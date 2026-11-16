package com.prnmjpngst.pemanggilan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.prnmjpngst.pemanggilan.AppConfig
import com.prnmjpngst.pemanggilan.AudioPlayer
import com.prnmjpngst.pemanggilan.Vehicle
import com.prnmjpngst.pemanggilan.buildClips
import com.prnmjpngst.pemanggilan.configFlow
import com.prnmjpngst.pemanggilan.createApi
import com.prnmjpngst.pemanggilan.isGagal
import com.prnmjpngst.pemanggilan.isLulus
import com.prnmjpngst.pemanggilan.saveConfig
import com.prnmjpngst.pemanggilan.toVehicle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AnnouncerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1, buildNotification("Pemanggilan aktif"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (loopJob == null) {
            loopJob = scope.launch { runLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun runLoop() {
        while (scope.isActive) {
            try {
                val cfg = applicationContext.configFlow().first()
                val api = createApi(cfg.baseUrl, cfg.apiKey)
                val resp = api.vehicles()
                if (resp.isSuccessful) {
                    val items = resp.body()?.items ?: emptyList()
                    val vehicles = items.map { it.toVehicle() }
                    // auto play gagal
                    if (cfg.autoPlayGagal) {
                        val pendingGagal = vehicles.filter { isGagal(it, cfg) && it.status != "done" }
                        for (v in pendingGagal) {
                            // simple throttle: play if never called or last called > repeatDelay
                            val shouldPlay = v.lastCalledAt == null ||
                                (System.currentTimeMillis() - (parseTime(v.lastCalledAt) ?: 0L)) > cfg.repeatDelaySec * 1000L
                            if (shouldPlay) {
                                playVehicle(cfg, v, api)
                            }
                        }
                    }
                    if (cfg.autoPlayLulus) {
                        val pendingLulus = vehicles.filter { isLulus(it, cfg) && it.status != "done" && it.calledCount == 0 }
                        for (v in pendingLulus) {
                            playVehicle(cfg, v, api)
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore, retry next cycle
            }
            val cfg = applicationContext.configFlow().first()
            delay(cfg.pollIntervalSec * 1000L)
        }
    }

    private suspend fun playVehicle(cfg: AppConfig, v: Vehicle, api: com.prnmjpngst.pemanggilan.PemanggilanApi) {
        val plate = v.field(cfg, "plate")
        val gagal = isGagal(v, cfg)
        val clips = buildClips(plate, gagal)
        repeat(cfg.repeatCount.coerceAtLeast(1)) { i ->
            AudioPlayer.playSequence(applicationContext, clips)
            if (i < cfg.repeatCount - 1) delay(cfg.repeatDelaySec * 1000L)
        }
        try { api.call(v.id) } catch (_: Exception) {}
    }

    private fun parseTime(s: String?): Long? {
        if (s == null) return null
        return try { java.time.Instant.parse(s).toEpochMilli() } catch (_: Exception) { null }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("pemanggilan", "Pemanggilan", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, "pemanggilan")
            .setContentTitle("Pemanggilan")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
}