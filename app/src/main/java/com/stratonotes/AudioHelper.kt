package com.stratonotes

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem

class AudioHelper(private val context: Context) {

    private var player: ExoPlayer? = null
    private var currentUri: Uri? = null

    fun play(uri: Uri) {
        if (currentUri != uri) {
            release() // Release old player if new file
            player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                play()
            }
            currentUri = uri
        } else {
            // Resume if same file
            player?.play()
        }
    }

    fun pause() {
        player?.pause()
    }

    fun stop() {
        player?.stop()
    }

    fun release() {
        player?.release()
        player = null
        currentUri = null
    }

    fun isPlaying(): Boolean {
        return player?.isPlaying == true
    }

    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0L
    }

    fun getDuration(): Long {
        return player?.duration ?: 0L
    }
}
