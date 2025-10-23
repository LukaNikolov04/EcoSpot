package com.elfak.ecospot.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDetailScreen(
    navController: NavController,
    mapViewModel: MapViewModel = viewModel()
) {
    val mapState by mapViewModel.mapState.collectAsState()
    val spot = mapState.selectedSpotDetails
    val comments = mapState.spotComments
    val userRating = mapState.userRatingForSelectedSpot
    var commentText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(spot?.name ?: "Učitavanje...") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (spot == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                item {
                    Text("Opis", style = MaterialTheme.typography.titleMedium)
                    Text(spot.description.ifBlank { "Nema opisa." })
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Dodao/la", style = MaterialTheme.typography.titleMedium)
                    Text(spot.authorName)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Tipovi otpada", style = MaterialTheme.typography.titleMedium)
                    Text(spot.wasteTypes.joinToString(", ") { type -> WasteType.valueOf(type).displayName })
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Prosečna ocena", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(String.format("%.1f", spot.averageRating), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Icon(Icons.Filled.Star, contentDescription = "Ocena", tint = Color.Yellow)
                        Text(" (${spot.ratingCount} ocena)")
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Ostavi ocenu", style = MaterialTheme.typography.titleMedium)
                    Row {
                        (1..5).forEach { index ->
                            Icon(
                                imageVector = if (index <= userRating) Icons.Filled.Star else Icons.Default.StarBorder,
                                contentDescription = "Ocena $index",
                                tint = Color.Yellow,
                                modifier = Modifier.clickable {
                                    mapViewModel.addRating(spot.id, index)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Text("Komentari", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        label = { Text("Napiši komentar...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                mapViewModel.addComment(spot.id, commentText)
                                commentText = ""
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Pošalji")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(comments) { comment ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(comment.authorName, fontWeight = FontWeight.Bold)
                            Text(comment.text)
                        }
                    }
                }
            }
        }
    }
}