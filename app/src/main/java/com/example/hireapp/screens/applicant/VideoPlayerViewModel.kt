package com.example.hireapp.screens.applicant

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem     // ← 이 패키지 확인!
import androidx.media3.exoplayer.ExoPlayer

class VideoPlayerViewModel(private val context: Context) : ViewModel() {
    // URL 당 하나의 ExoPlayer 인스턴스를 캐싱합니다.
    private val players = mutableMapOf<String, ExoPlayer>()

    fun getPlayer(url: String): ExoPlayer =
        players.getOrPut(url) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                prepare()
                playWhenReady = false
            }
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