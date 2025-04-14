package com.example.hireapp.util

import com.google.firebase.firestore.DocumentSnapshot

data class Paging(
    val lastDocument: DocumentSnapshot? = null,
    val elementsTotal: Long = 0
)