package com.elfak.ecospot.ui.auth

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.elfak.ecospot.R

@Composable
fun RegistrationScreen(
    authState: AuthState,
    errorState: Map<String, String?>,
    onRegisterClick: (String, String, String, String, Uri?) -> Unit,
    onValidateEmail: (String) -> Unit,
    onValidatePassword: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onResetAuthState: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val emailError = errorState["email"]
    val passwordError = errorState["password"]


    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> imageUri = uri }
    )


    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            Toast.makeText(context, authState.message, Toast.LENGTH_SHORT).show()
            onResetAuthState()
        }
    }

    val textFieldColors = TextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Registracija", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painter = if (imageUri != null) {
                rememberAsyncImagePainter(imageUri)
            } else {
                painterResource(id = R.drawable.ic_launcher_foreground)
            },
            contentDescription = "Profilna slika",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .clickable { galleryLauncher.launch("image/*") },
            contentScale = ContentScale.Crop
        )
        Text("Dodaj fotografiju", modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                onValidateEmail(it)
            },
            label = { Text("Email (korisničko ime)") },
            modifier = Modifier.fillMaxWidth(),
            isError = emailError != null,
            colors = textFieldColors
        )
        if (emailError != null) {
            Text(text = emailError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                onValidatePassword(it)
            },
            label = { Text("Lozinka") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = passwordError != null,
            colors = textFieldColors
        )
        if (passwordError != null) {
            Text(text = passwordError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Ime i prezime") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Broj telefona") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onRegisterClick(email, password, fullName, phone, imageUri) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registruj se")
        }
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Već imaš nalog? Prijavi se")
        }
    }
}