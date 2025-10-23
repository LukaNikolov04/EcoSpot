package com.elfak.ecospot.ui

import android.annotation.SuppressLint
import android.app.Application
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.math.*
import kotlinx.coroutines.launch
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.FirebaseException


data class MapState(
    val lastKnownLocation: LatLng? = null,
    val allRecyclingSpots: List<RecyclingSpot> = emptyList(),
    val selectedSpotDetails: RecyclingSpot? = null,
    val spotComments: List<Comment> = emptyList(),
    val userRatingForSelectedSpot: Int = 0,

    val filterSearchText: String = "",
    val filterWasteType: WasteType? = null,
    val filterRadius: Double? = null,
    val filterAuthorName: String = "",
)

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val _mapState = MutableStateFlow(MapState())


    val mapState: StateFlow<MapState> = _mapState


    val filteredRecyclingSpots: StateFlow<List<RecyclingSpot>> =
        _mapState.combine(mapState) { state, _ ->
            applyFilters(state)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun applyFilters(state: MapState): List<RecyclingSpot> {
        var spots = state.allRecyclingSpots

        // 1. Filter po tekstu (nazivu)
        if (state.filterSearchText.isNotBlank()) {
            spots = spots.filter { it.name.contains(state.filterSearchText, ignoreCase = true) }
        }

        // 2. Filter po tipu otpada
        state.filterWasteType?.let { wasteType ->
            spots = spots.filter { it.wasteTypes.contains(wasteType.name) }
        }

        // 3. Filter po radijusu
        state.filterRadius?.let { radius ->
            state.lastKnownLocation?.let { userLocation ->
                spots = spots.filter { spot ->
                    val distance = calculateDistance(
                        userLocation.latitude, userLocation.longitude,
                        spot.location.latitude, spot.location.longitude
                    )
                    distance <= radius
                }
            }
        }
        // 4. Filter po autoru
        if (state.filterAuthorName.isNotBlank()) {
            spots = spots.filter { it.authorName.contains(state.filterAuthorName, ignoreCase = true) }
        }


        return spots
    }


    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = (lat2 - lat1) * (Math.PI / 180)
        val dLon = (lon2 - lon1) * (Math.PI / 180)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * (Math.PI / 180)) * cos(lat2 * (Math.PI / 180)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    // --- Funkcije za ažuriranje filtera ---
    fun onSearchTextChanged(text: String) {
        _mapState.value = _mapState.value.copy(filterSearchText = text)
    }

    fun onWasteTypeChanged(wasteType: WasteType?) {
        _mapState.value = _mapState.value.copy(filterWasteType = wasteType)
    }

    fun onRadiusFilterChanged(radius: Double?) {
        _mapState.value = _mapState.value.copy(filterRadius = radius)
    }

    fun onAuthorFilterChanged(authorName: String) {
        _mapState.value = _mapState.value.copy(filterAuthorName = authorName)
    }


    // Klijent za dobijanje lokacije od Google Play servisa
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)


    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // --- KONSTANTE ZA POENE ---
    private val POINTS_FOR_ADDING_SPOT = 10L
    private val POINTS_FOR_RATING = 3L
    private val POINTS_FOR_COMMENT = 5L


    private var spotListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null

    init {
        observeRecyclingSpots()
    }


    fun observeSpotDetails(spotId: String) {
        val userId = auth.currentUser?.uid ?: return

        clearSpotDetails()


        spotListener = firestore.collection("recycling_spots").document(spotId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("MapViewModel", "Listen failed for spot details.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val spot = snapshot.toObject<RecyclingSpot>()
                    val spotWithId = spot?.copy(id = snapshot.id)
                    _mapState.value = _mapState.value.copy(selectedSpotDetails = spotWithId)
                } else {
                    Log.d("MapViewModel", "Current data: null")
                }
            }


        commentsListener = firestore.collection("recycling_spots").document(spotId)
            .collection("comments").orderBy("createdAt")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                snapshot?.let {
                    _mapState.value =
                        _mapState.value.copy(spotComments = it.toObjects(Comment::class.java))
                }
            }

        firestore.collection("users").document(userId)
            .collection("ratings").document(spotId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val rating = snapshot?.getLong("rating")?.toInt() ?: 0
                _mapState.value = _mapState.value.copy(userRatingForSelectedSpot = rating)
            }

    }


    fun addRating(spotId: String, newRating: Int) {
        val userId = auth.currentUser?.uid ?: return

        val spotRef = firestore.collection("recycling_spots").document(spotId)
        val userRatingRef = firestore.collection("users").document(userId)
            .collection("ratings").document(spotId)

        firestore.runTransaction { transaction ->
            val userRatingSnapshot = transaction.get(userRatingRef)
            val spotSnapshot = transaction.get(spotRef)

            if (!spotSnapshot.exists()) {
                throw FirebaseException("Reciklažna tačka ne postoji.")
            }

            val oldRating = userRatingSnapshot.getLong("rating")?.toInt()

            if (oldRating != null) {

                val oldRatingCount = spotSnapshot.getLong("ratingCount")?.toInt() ?: 1
                val oldAverageRating = spotSnapshot.getDouble("averageRating") ?: 0.0


                val newAverageRating =
                    ((oldAverageRating * oldRatingCount) - oldRating + newRating) / oldRatingCount

                transaction.update(spotRef, "averageRating", newAverageRating)
                transaction.set(userRatingRef, hashMapOf("rating" to newRating))

            } else {

                val oldRatingCount = spotSnapshot.getLong("ratingCount")?.toInt() ?: 0
                val oldAverageRating = spotSnapshot.getDouble("averageRating") ?: 0.0

                val newRatingCount = oldRatingCount + 1
                val newAverageRating =
                    ((oldAverageRating * oldRatingCount) + newRating) / newRatingCount

                transaction.update(spotRef, "ratingCount", newRatingCount)
                transaction.update(spotRef, "averageRating", newAverageRating)
                transaction.set(userRatingRef, hashMapOf("rating" to newRating))


                addPointsToCurrentUser(POINTS_FOR_RATING)
            }
            null
        }.addOnSuccessListener {
            Log.d("MapViewModel", "Rating successfully submitted/updated!")
        }.addOnFailureListener { e ->
            Log.e("MapViewModel", "Error submitting rating", e)
        }
    }

    fun addComment(spotId: String, text: String) {
        auth.currentUser?.uid?.let { userId ->

            firestore.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("fullName") ?: "Nepoznat korisnik"

                val comment = hashMapOf(
                    "text" to text,
                    "authorId" to userId,
                    "authorName" to userName,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                firestore.collection("recycling_spots").document(spotId)
                    .collection("comments").add(comment)
                    .addOnSuccessListener {
                        Log.d("MapViewModel", "Comment successfully added!")
                        addPointsToCurrentUser(POINTS_FOR_COMMENT)
                    }
            }
        }
    }


    fun clearSpotDetails() {
        spotListener?.remove()
        commentsListener?.remove()

        _mapState.value = _mapState.value.copy(
            selectedSpotDetails = null,
            spotComments = emptyList(),
            userRatingForSelectedSpot = 0
        )
    }

    private fun addPointsToCurrentUser(points: Long) {
        auth.currentUser?.uid?.let { userId ->
            val userDocRef = firestore.collection("users").document(userId)
            userDocRef.update("points", FieldValue.increment(points))
                .addOnSuccessListener { Log.d("MapViewModel", "$points points added to user!") }
        }
    }

    private fun observeRecyclingSpots() {
        firestore.collection("recycling_spots").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("MapViewModel", "Listen failed.", e)
                return@addSnapshotListener
            }

            snapshot?.let { querySnapshot ->

                val spots = querySnapshot.documents.mapNotNull { document ->

                    val spot = document.toObject<RecyclingSpot>()

                    spot?.copy(id = document.id)
                }

                _mapState.value = _mapState.value.copy(allRecyclingSpots = spots)
                Log.d("MapViewModel", "Spots updated: ${spots.size}")
            }
        }
    }

    fun addRecyclingSpot(
        name: String,
        description: String,
        wasteTypes: List<String>,
        location: LatLng
    ) {
        auth.currentUser?.uid?.let { userId ->


            firestore.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("fullName") ?: "Nepoznat autor"


                val newSpot = hashMapOf(
                    "name" to name,
                    "description" to description,
                    "wasteTypes" to wasteTypes,
                    "location" to GeoPoint(location.latitude, location.longitude),
                    "authorId" to userId,
                    "authorName" to userName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "averageRating" to 0.0,
                    "ratingCount" to 0
                )

                firestore.collection("recycling_spots").add(newSpot)
                    .addOnSuccessListener {
                        Log.d("MapViewModel", "Spot successfully added!")

                        addPointsToCurrentUser(POINTS_FOR_ADDING_SPOT)
                    }
                    .addOnFailureListener { e ->
                        Log.w("MapViewModel", "Error adding spot", e)
                    }
            }
        }
    }


        private val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                locationResult.lastLocation?.let { location ->
                    val newLatLng = LatLng(location.latitude, location.longitude)

                    _mapState.value = _mapState.value.copy(
                        lastKnownLocation = newLatLng
                    )

                    updateUserLocationInFirestore(newLatLng)
                }
            }
        }

        private fun updateUserLocationInFirestore(latLng: LatLng) {

            auth.currentUser?.uid?.let { userId ->
                val userDocRef = firestore.collection("users").document(userId)

                val locationData = hashMapOf(
                    "location" to GeoPoint(latLng.latitude, latLng.longitude),
                    "lastUpdated" to FieldValue.serverTimestamp()
                )


                userDocRef.update(locationData)
            }
        }



        @SuppressLint("MissingPermission")
        fun startLocationUpdates() {

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000L)
                .build()


            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        // Kada se aplikacija ugasi, prestajemo sa pracenjem zbog baterije
        override fun onCleared() {
            super.onCleared()
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
