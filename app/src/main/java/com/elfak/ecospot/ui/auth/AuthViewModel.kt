package com.elfak.ecospot.ui.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.SetOptions
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuthException

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _errorState = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errorState = _errorState.asStateFlow()


    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun registerUser(email: String, pass: String, fullName: String, phone: String, imageUri: Uri?) {

        if (!isEmailValid(email) || !isPasswordValid(pass) || fullName.isBlank() || phone.isBlank()) {
            _authState.value = AuthState.Error("Proverite unete podatke.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {

                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val user = authResult.user ?: throw Exception("Kreiranje korisnika neuspešno.")


                var imageUrl: String? = null
                if (imageUri != null) {
                    val storageRef = storage.reference.child("profile_images/${user.uid}")
                    storageRef.putFile(imageUri).await()
                    imageUrl = storageRef.downloadUrl.await().toString()
                }


                val userData = hashMapOf(
                    "fullName" to fullName,
                    "phone" to phone,
                    "email" to email,
                    "profileImageUrl" to imageUrl,
                    "points" to 0
                )
                firestore.collection("users").document(user.uid).set(userData).await()
                saveFcmToken(user.uid)
                _authState.value = AuthState.Success

                _authState.value = AuthState.Success
            }catch (e: FirebaseAuthException) {

                when (e.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> _authState.value = AuthState.Error("Email adresa je već u upotrebi.")
                    "ERROR_WEAK_PASSWORD" -> _authState.value = AuthState.Error("Lozinka je previše slaba. Mora imati bar 6 karaktera.")
                    else -> _authState.value = AuthState.Error(e.localizedMessage ?: "Došlo je do nepoznate greške.")
                }
            }
            catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Došlo je do nepoznate greške.")
            }
        }
    }

    private fun saveFcmToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val tokenData = hashMapOf("fcmToken" to token)
                firestore.collection("users").document(userId)
                    .set(tokenData, SetOptions.merge())
            }
        }
    }

    fun loginUser(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Morate popuniti oba polja.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                result.user?.uid?.let { saveFcmToken(it) }
                _authState.value = AuthState.Success
            } catch (e: FirebaseAuthException) {
                when (e.errorCode) {
                    "ERROR_INVALID_EMAIL", "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD" ->
                        _authState.value = AuthState.Error("Pogrešan email ili lozinka.")
                    else -> _authState.value = AuthState.Error(e.localizedMessage ?: "Došlo je do nepoznate greške.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Došlo je do nepoznate greške.")
            }
        }
    }


    fun isEmailValid(email: String): Boolean {
        val isValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
        _errorState.value = _errorState.value.plus("email" to if (isValid) null else "Neispravan format email adrese")
        return isValid
    }

    fun isPasswordValid(password: String): Boolean {
        val isValid = password.length >= 6
        _errorState.value = _errorState.value.plus("password" to if (isValid) null else "Lozinka mora imati bar 6 karaktera")
        return isValid
    }

    fun clearErrors() {
        _errorState.value = emptyMap()
    }




    fun logout() {

        auth.signOut()

        _authState.value = AuthState.Unauthenticated
    }


    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}


sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}