package com.elfak.ecospot.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpotScreen(
    location: LatLng,
    mapViewModel: MapViewModel,
    navController: NavController
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var selectedWasteType by remember { mutableStateOf<WasteType?>(null) }
    val context = LocalContext.current

    val textFieldColors = TextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.primary
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dodaj Novu Tačku") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Naziv tačke (npr. 'Kontejner kod parka')") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opcioni opis") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = it }
            ) {
                TextField(
                    value = selectedWasteType?.displayName ?: "Izaberi tip otpada",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = textFieldColors
                )
                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    WasteType.values().forEach { wasteType ->
                        DropdownMenuItem(
                            text = { Text(wasteType.displayName) },
                            onClick = {
                                selectedWasteType = wasteType
                                isExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && selectedWasteType != null) {
                        mapViewModel.addRecyclingSpot(
                            name,
                            description,
                            listOf(selectedWasteType!!.name),
                            location
                        )
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context, "Naziv i tip otpada su obavezni.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sačuvaj Tačku")
            }
        }
    }
}