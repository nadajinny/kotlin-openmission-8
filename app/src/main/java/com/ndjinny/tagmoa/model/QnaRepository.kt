package com.ndjinny.tagmoa.model

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object QnaRepository {

    private val qnaRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("qna")
    }

    fun entriesRef(): DatabaseReference = qnaRef
}
