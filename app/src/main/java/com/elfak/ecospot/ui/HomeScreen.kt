package com.elfak.ecospot.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.elfak.ecospot.R
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogoutClick: () -> Unit,
    mapViewModel: MapViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val mapState by mapViewModel.mapState.collectAsState()
    val filteredSpots by mapViewModel.filteredRecyclingSpots.collectAsState()

    val scaffoldState = rememberBottomSheetScaffoldState()


    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            mapViewModel.startLocationUpdates()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("EcoSpot Mapa") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SPOT_LIST) }) {
                        Icon(Icons.Default.List, contentDescription = "Prikaži listu")
                    }
                    IconButton(onClick = { navController.navigate(Routes.RANKING) }) {
                        Icon(painterResource(id = R.drawable.ic_ranking), contentDescription = "Rang Lista")
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Odjavi se")
                    }
                }
            )
        },
        // Sadržaj "fioke" (filteri)
        sheetContent = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Filteri", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                // Filter po nazivu
                OutlinedTextField(
                    value = mapState.filterSearchText,
                    onValueChange = { mapViewModel.onSearchTextChanged(it) },
                    label = { Text("Pretraži po nazivu...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (mapState.filterSearchText.isNotEmpty()) {
                            IconButton(onClick = { mapViewModel.onSearchTextChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Očisti")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))


                // Filter po autoru
                OutlinedTextField(
                    value = mapState.filterAuthorName,
                    onValueChange = { mapViewModel.onAuthorFilterChanged(it) },
                    label = { Text("Pretraži po autoru...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, // Potreban import
                    trailingIcon = {
                        if (mapState.filterAuthorName.isNotEmpty()) {
                            IconButton(onClick = { mapViewModel.onAuthorFilterChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Očisti")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))


                // Filter po tipu otpada
                Text("Tip otpada:")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(WasteType.values()) { wasteType ->
                        FilterChip(
                            selected = mapState.filterWasteType == wasteType,
                            onClick = {
                                if (mapState.filterWasteType == wasteType) {
                                    mapViewModel.onWasteTypeChanged(null)
                                } else {
                                    mapViewModel.onWasteTypeChanged(wasteType)
                                }
                            },
                            label = { Text(wasteType.displayName) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Filter po radijusu
                Text("Prikaži samo u blizini (km): ${mapState.filterRadius?.toInt() ?: "Sve"}")
                var sliderPosition by rememberSaveable { mutableStateOf(mapState.filterRadius?.toFloat() ?: 10f) }
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = {
                        mapViewModel.onRadiusFilterChanged(sliderPosition.toDouble())
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1 km")
                    Text("10 km")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        mapViewModel.onRadiusFilterChanged(null)
                        sliderPosition = 10f
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Poništi filter udaljenosti")
                }
            }
        },
        sheetPeekHeight = 64.dp
    ) { paddingValues ->
        // Glavni sadržaj ekrana
        Box(modifier = Modifier.padding(paddingValues)) {
            if (hasLocationPermission) {
                val userLocation = mapState.lastKnownLocation ?: LatLng(43.3209, 21.8958)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(userLocation, 15f)
                }

                LaunchedEffect(userLocation) {
                    if (cameraPositionState.position.target != userLocation) {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(userLocation, 17f),
                            durationMs = 1000
                        )
                    }
                }

                val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }
                val mapUiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false)) }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = mapUiSettings
                ) {

                    // Crtanje markera na osnovu FILTRIRANE liste
                    filteredSpots.forEach { spot ->
                        val spotPosition = LatLng(spot.location.latitude, spot.location.longitude)
                        Marker(
                            state = MarkerState(position = spotPosition),
                            title = spot.name,
                            snippet = spot.wasteTypes.joinToString(", "),
                            zIndex = 10f,
                            onClick = {
                                if (spot.id.isNotBlank()) {
                                    navController.navigate(Routes.SPOT_DETAIL_ROUTE.replace("{spotId}", spot.id))
                                }
                                true
                            }
                        )
                    }
                }
            } else {

                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Potrebna je dozvola za lokaciju da bi aplikacija radila.")
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                        Text("Dozvoli pristup lokaciji")
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    mapState.lastKnownLocation?.let { userLocation ->
                        navController.navigate("${Routes.ADD_SPOT}/${userLocation.latitude}/${userLocation.longitude}")
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj tačku")
            }
        }
    }
}