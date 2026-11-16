package com.prnmjpngst.pemanggilan

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object AudioPlayer {
    private var player: ExoPlayer? = null

    fun init(context: Context) {
        if (player == null) player = ExoPlayer.Builder(context.applicationContext).build()
    }

    fun clipUri(context: Context, name: String): Uri =
        Uri.parse("asset:///audio/$name")

    suspend fun playSequence(context: Context, clips: List<String>) {
        init(context)
        val p = player ?: return
        // build media items
        val items = clips.map { MediaItem.fromUri(clipUri(context, it)) }
        suspendCancellableCoroutine<Unit> { cont ->
            p.setMediaItems(items)
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

    fun stop() { player?.stop() }
}