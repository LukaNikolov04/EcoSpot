package com.elfak.ecospot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    navController: NavController,
    rankingViewModel: RankingViewModel = viewModel()
) {
    val rankingState by rankingViewModel.rankingState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rang Lista") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (rankingState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold)
                        Text("Korisnik", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("Poeni", fontWeight = FontWeight.Bold)
                    }
                    Divider()
                }

                itemsIndexed(rankingState.userList) { index, user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text((index + 1).toString(), modifier = Modifier.width(40.dp))
                        Text(user.fullName, modifier = Modifier.weight(1f))
                        Text(user.points.toString(), fontWeight = FontWeight.Bold)
                    }
                    Divider()
                }
            }
        }
    }
}