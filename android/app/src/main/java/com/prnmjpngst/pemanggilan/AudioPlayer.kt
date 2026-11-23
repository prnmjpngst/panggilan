package com.prnmjpngst.pemanggilan

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object AudioPlayer {
    private var player: ExoPlayer? = null

    fun init(context: Context) {
        if (player == null) player = ExoPlayer.Builder(context.applicationContext).build()
    }

    fun clipUri(context: Context, name: String): Uri =
        Uri.parse("asset:///audio/$name")

    suspend fun playClip(context: Context, name: String) {
        init(context)
        val p = player ?: return
        suspendCancellableCoroutine<Unit> { cont ->
            p.setMediaItem(MediaItem.fromUri(clipUri(context, name)))
            p.prepare()
            p.playWhenReady = true
            val listener = object : androidx.media3.common.Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == androidx.media3.common.Player.STATE_ENDED) {
                        p.removeListener(this)
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            }
            p.addListener(listener)
            cont.invokeOnCancellation {
                p.removeListener(listener)
                p.stop()
            }
        }
    }

    suspend fun playSequence(context: Context, items: List<Any>) {
        init(context)
        for (item in items) {
            when (item) {
                is String -> playClip(context, item)
                is Int -> delay(item.toLong())
                is Long -> delay(item)
            }
        }
    }

    fun stop() { player?.stop() }
}