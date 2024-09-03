package com.example.rmas.navigation

import Home
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rmas.CreateRequestScreen
import com.example.rmas.LeaderboardScreen
import com.example.rmas.MapScreen
import com.example.rmas.MyPlacesScreen
import com.example.rmas.Profile
import com.example.rmas.R
import com.example.rmas.RequestDetails
import com.example.rmas.database.FirebaseDatabase.getCurrentUser

@SuppressLint("ComposableDestinationInComposeScope")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Navbar() {
    val navigationController = rememberNavController()
    val selected = remember { mutableStateOf(Icons.Default.Home) }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(48.dp),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                IconButton(
                    onClick = {
                        selected.value = Icons.Default.Home
                        navigationController.navigate(Screens.Home.screen) {
                            popUpTo(Screens.Home.screen) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = if (selected.value == Icons.Default.Home) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.tertiary
                    )
                }



                IconButton(
                    onClick = {
                        selected.value = Icons.Default.Place
                        navigationController.navigate(Screens.MapScreen.screen) {
                            popUpTo(Screens.Home.screen) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = if (selected.value == Icons.Default.Place) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.tertiary
                    )
                }

                IconButton(
                    onClick = {
                        selected.value = Icons.Default.PlayArrow
                        navigationController.navigate(Screens.LeaderboardScreen.screen) {
                            popUpTo(Screens.Home.screen) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.leaderboard), // Use the resource ID of the vector asset
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = if (selected.value == Icons.Default.PlayArrow) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.tertiary
                    )
                }
                IconButton(
                    onClick = {
                        selected.value = Icons.Default.Star
                        navigationController.navigate(Screens.MyPlaces.screen) {
                            popUpTo(Screens.Home.screen) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = if (selected.value == Icons.Default.Star) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.tertiary
                    )
                }

                IconButton(
                    onClick = {
                        selected.value = Icons.Default.Person
                        navigationController.navigate(Screens.Profile.screen) {
                            popUpTo(Screens.Home.screen) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = if (selected.value == Icons.Default.Person) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navigationController,
            startDestination = Screens.Home.screen,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screens.Home.screen) {
                Home(getCurrentUser()!!, navigationController) { requestId ->
                    navigationController.navigate("request_details/$requestId")
                }
            }

            composable(Screens.Profile.screen) {
                Profile(getCurrentUser()!!)
            }

            composable("request_details/{requestId}") { backStackEntry ->
                val requestId: String? = backStackEntry.arguments?.getString("requestId")
                if (requestId != null) {
                    RequestDetails(
                        userId = getCurrentUser()!!,
                        requestId = requestId,
                        navHostController = navigationController
                    ) { mapRequestId ->
                        navigationController.navigate("map_screen/$mapRequestId")
                    }
                }
            }

            composable("map_screen/{requestId}") { backStackEntry ->
                val requestId: String? = backStackEntry.arguments?.getString("requestId")
                MapScreen(requestId = requestId, placeId = null, navHostController = navigationController)
            }

            composable("map_screen/place/{placeId}") { backStackEntry ->
                val placeId: String? = backStackEntry.arguments?.getString("placeId")
                MapScreen(requestId = null, placeId = placeId, navHostController = navigationController)
            }

            composable(Screens.MapScreen.screen) {
                MapScreen(requestId = null, placeId = null, navHostController = navigationController)
            }

            composable(Screens.CreateRequest.screen) {
                CreateRequestScreen(id = getCurrentUser()!!, navigationController)
            }

            composable(Screens.LeaderboardScreen.screen) {
                LeaderboardScreen(navigationController)
            }

            composable("my_places/{placeId}") { backStackEntry ->
                val placeId: String? = backStackEntry.arguments?.getString("placeId")
                MyPlacesScreen(navHostController = navigationController) { selectedPlaceId ->
                    navigationController.navigate("map_screen/place/$selectedPlaceId")
                }
            }

            composable(Screens.MyPlaces.screen) {
                MyPlacesScreen(navHostController = navigationController) { selectedPlaceId ->
                    navigationController.navigate("map_screen/place/$selectedPlaceId")
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun PreviewNavBar() {
    Navbar()
}