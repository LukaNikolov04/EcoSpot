package com.elfak.ecospot.ui

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date


enum class WasteType(val displayName: String) {
    BATTERIES("Baterije"),
    ELECTRONICS("Elektronika"),
    GLASS("Staklo"),
    PLASTIC("Plastika"),
    METAL("Metal"),
    PAPER("Papir")
}

data class RecyclingSpot(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val wasteTypes: List<String> = emptyList(),
    val authorId: String = "",
    val authorName: String = "Nepoznat autor",
    @ServerTimestamp val createdAt: Date? = null,
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0
)