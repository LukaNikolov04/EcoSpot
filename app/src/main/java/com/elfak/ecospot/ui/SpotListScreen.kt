package com.elfak.ecospot.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotListScreen(
    navController: NavController,
    mapViewModel: MapViewModel = viewModel()
) {
    val filteredSpots by mapViewModel.filteredRecyclingSpots.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista Reciklažnih Tačaka") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredSpots.isEmpty()) {
                item { Text("Nema rezultata za izabrane filtere.") }
            } else {
                items(filteredSpots, key = { it.id }) { spot ->
                    SpotListItem(spot = spot, onClick = {
                        navController.navigate(Routes.SPOT_DETAIL_ROUTE.replace("{spotId}", spot.id))
                    })
                }
            }
        }
    }
}

@Composable
fun SpotListItem(spot: RecyclingSpot, onClick: () -> Unit) {
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(spot.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))

            Text(
                "Autor: ${spot.authorName}",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
            Spacer(Modifier.height(4.dp))

            Text(
                "Tip: ${spot.wasteTypes.joinToString { WasteType.valueOf(it).displayName }}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Ocena: ${String.format("%.1f", spot.averageRating)} (${spot.ratingCount})",
                    style = MaterialTheme.typography.bodySmall
                )
                spot.createdAt?.let {
                    Text(
                        dateFormatter.format(it),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}