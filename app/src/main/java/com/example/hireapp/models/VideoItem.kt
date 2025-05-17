package com.example.hireapp.models

data class VideoItem(
    val id: String = "",
    val title: String = "",
    val userName: String = "",
    val fileUrl: String? = null,
    val fileUrls: List<String> = emptyList(),
    val major: String = "",
    val sub: String = ""
)