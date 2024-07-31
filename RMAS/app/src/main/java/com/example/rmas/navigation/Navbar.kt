package com.example.rmas.navigation

import Home
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rmas.CreateRequestScreen
import com.example.rmas.Profile
import com.example.rmas.RequestDetails
import com.example.rmas.database.FirebaseDatabase.getCurrentUser


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Navbar() {
    val navigationController = rememberNavController()
    val context = LocalContext.current.applicationContext
    val selected = remember { mutableStateOf(Icons.Default.Home) }
    val navigationCont: (String) ->Unit

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
                            popUpTo(0)
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
                        selected.value = Icons.Default.Person
                        navigationController.navigate(Screens.Profile.screen) {
                            popUpTo(0)
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
        NavHost(navController = navigationController,  startDestination = Screens.Home.screen,
            modifier = Modifier.padding(paddingValues)) {
            composable(Screens.Home.screen){
                Home(getCurrentUser()!!, navigationController){

                navigationController.navigate("request_details/{$it}")
            } }
            composable(Screens.Profile.screen) { Profile(getCurrentUser()!!)
            }

            composable("request_details/{requestId}") { backStackEntry ->
                val requestId: String? = backStackEntry.arguments?.getString("requestId")
                if (requestId != null) {
                    RequestDetails(userId = getCurrentUser()!!, requestId = requestId, navHostController = navigationController)
                }
            }
            composable(Screens.CreateRequest.screen){ CreateRequestScreen(
                id = getCurrentUser()!!,
                navigationController
            )}

        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun PreviewNavBar() {
    Navbar()
}