package com.example.hireapp.screens.applicant

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem     // ← 이 패키지 확인!
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory


class VideoPlayerViewModel(private val context: Context) : ViewModel() {
    private val cacheSize = 10
    // 재생 위치를 저장할 Map
    private val positions = mutableMapOf<String, Long>()
    private val players = object : LinkedHashMap<String, ExoPlayer>(cacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ExoPlayer>): Boolean {
            if (size > cacheSize) {
                // 버리기 전에 현재 위치 저장
                positions[eldest.key] = eldest.value.currentPosition
                eldest.value.stop()
                eldest.value.release()
                return true
            }
            return false
        }
    }

    @OptIn(UnstableApi::class)
    fun getPlayer(url: String): ExoPlayer =
        synchronized(players) {
            players[url]?.let { return it }

            // LRU eviction 이 동작하도록 LinkedHashMap 으로 생성
            val renderersFactory = DefaultRenderersFactory(context)
                .setEnableDecoderFallback(true)

            val player = ExoPlayer.Builder(context, renderersFactory).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                prepare()
                positions[url]?.let { seekTo(it) }
            }
            players[url] = player
            player
        }

    fun releaseAllPlayers() {
        players.forEach { (url, player) ->
            positions[url] = player.currentPosition
            player.stop()
            player.release()
        }
        players.clear()
    }

    override fun onCleared() {
        // ViewModel이 소멸될 때 플레이어 해제
        players.values.forEach { it.release() }
        super.onCleared()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VideoPlayerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return VideoPlayerViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}