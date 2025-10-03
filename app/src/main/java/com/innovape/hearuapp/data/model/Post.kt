package com.innovape.hearuapp.data.model

import com.google.firebase.Timestamp

data class Post(
    var content: String = "",
    var isAnonymous: Boolean = false,
    var timestamp: Timestamp? = null,
    var userId: String = "",
    var username: String = ""
)
