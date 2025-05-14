package com.example.hireapp.data

enum class Category(val label: String) {
    COMPANY("기업"),
    OFFICIAL("공무원"),
    EDUCATION("교육"),
    UNIVERSITY("대학"),
    NONE("지정 없음");
}

val subCategory = mapOf(
    Category.COMPANY.label to listOf("IT", "디자인", "경영/사무", "생산/기술"),
    Category.OFFICIAL.label to listOf("7,9급", "경찰", "소방", "군무원"),
    Category.EDUCATION.label to listOf("초·중등교사", "유치원교사", "강사"),
    Category.UNIVERSITY.label to listOf("학부 입시", "편입", "대학원"),
    Category.NONE.label to listOf("지정 없음")
)