package com.ndjinny.tagmoa.model

data class QnaEntry(
    var id: String? = null,
    var title: String? = null,
    var content: String? = null,
    var authorUid: String? = null,
    var authorName: String? = null,
    var authorEmail: String? = null,
    var isPublic: Boolean = true,
    var createdAt: Long = 0L,
    var comments: Map<String, QnaComment>? = null
)

data class QnaComment(
    var id: String? = null,
    var authorUid: String? = null,
    var authorName: String? = null,
    var authorEmail: String? = null,
    var content: String? = null,
    var createdAt: Long = 0L
)
