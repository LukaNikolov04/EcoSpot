package com.elfak.ecospot.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class RankingState(
    val userList: List<RankedUser> = emptyList(),
    val isLoading: Boolean = true
)

class RankingViewModel : ViewModel() {

    private val _rankingState = MutableStateFlow(RankingState())
    val rankingState = _rankingState.asStateFlow()

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        fetchUsersForRanking()
    }

    private fun fetchUsersForRanking() {
        viewModelScope.launch {
            _rankingState.value = _rankingState.value.copy(isLoading = true)
            try {
                val snapshot = firestore.collection("users")
                    .orderBy("points", Query.Direction.DESCENDING)
                    .limit(100)
                    .get()
                    .await()

                val users = snapshot.toObjects(RankedUser::class.java)
                _rankingState.value = _rankingState.value.copy(userList = users, isLoading = false)

            } catch (e: Exception) {
                Log.e("RankingViewModel", "Error fetching users", e)
                _rankingState.value = _rankingState.value.copy(isLoading = false)
            }
        }
    }
}