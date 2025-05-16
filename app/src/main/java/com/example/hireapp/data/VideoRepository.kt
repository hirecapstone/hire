package com.example.hireapp.data

import android.util.Log
import com.example.hireapp.models.VideoItem
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * 필터 조건에 맞는 비디오 리스트를 최신 업로드순으로 반환합니다.
 *
 * @param major 대분류 리스트
 * @param sub 소분류 리스트
 */
suspend fun fetchPublicVideos(major: String?, sub: List<String>?): List<VideoItem> {
    val db = Firebase.firestore
    val resultList = mutableListOf<VideoItem>()

    try {
        var query = db.collection("interview")
            .whereEqualTo("public", true)

        // major 조건이 있는 경우
        if (major != null) {
            Log.d("Fetch-videos", major)
            query = query.whereEqualTo("category.major", major)
        }

        // sub 조건이 있는 경우
        if (sub != null) {
            if (sub.isNotEmpty()) {
                Log.d("Fetch-videos", "sub 조건 추가, ${sub.toString()}")
                query = query.whereIn("category.sub", sub)
            }
        }

        val interviewDocs = query.orderBy("uploadTime", Query.Direction.DESCENDING) .get().await()

        for (doc in interviewDocs) {
            val title = doc.getString("title") ?: continue

            val userField = doc.get("user")
            val userName = when (userField) {
                is DocumentReference -> {
                    try {
                        val snapshot = userField.get().await()
                        snapshot.getString("name")
                    } catch (e: Exception) {
                        Log.e("FirestoreDebug", "문서 ${doc.id} → user 문서 불러오기 실패: ${e.message}")
                        null
                    }
                }
                is String -> {
                    try {
                        val snapshot = db.collection("users").document(userField).get().await()
                        snapshot.getString("name")
                    } catch (e: Exception) {
                        Log.e("FirestoreDebug", "문서 ${doc.id} → UUID로 유저 조회 실패: ${e.message}")
                        null
                    }
                }
                else -> null
            }

            if (userName.isNullOrEmpty()) continue

            val videos = doc.get("videos") as? List<Map<String, Any>>
            val fileUrl = videos?.firstOrNull()?.get("fileUrl") as? String ?: continue

            resultList.add(VideoItem(doc.id, title, userName, fileUrl))
        }

    } catch (e: Exception) {
        Log.e("VideoRepository", "영상 목록 불러오기 실패: ${e.message}")
    }

    return resultList
}