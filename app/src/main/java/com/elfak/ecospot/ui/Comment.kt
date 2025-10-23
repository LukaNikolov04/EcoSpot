package com.elfak.ecospot.ui

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Comment(
    val id: String = "",
    val text: String = "",
    val authorId: String = "",
    val authorName: String = "Nepoznat korisnik",
    @ServerTimestamp val createdAt: Date? = null
)