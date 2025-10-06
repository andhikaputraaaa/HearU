package com.innovape.hearuapp.data.model

import com.google.firebase.Timestamp

data class Comment(
    var id: String? = null,
    var userId: String = "",
    var username: String = "",
    var content: String = "",
    var profileImageUrl: String? = null,
    var profileImageResource: String? = null,
    var timestamp: Timestamp? = null
)
