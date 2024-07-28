package com.example.rmas.navigation

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rmas.LoginActivity
import com.example.rmas.Profile
import com.example.rmas.database.FirebaseDatabase.getCurrentUser
import com.google.android.gms.games.leaderboard.Leaderboard


@Composable
fun Navbar() {
    val navigationController = rememberNavController()
    val context = LocalContext.current.applicationContext
    val selected = remember { mutableStateOf(Icons.Default.Person) }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(48.dp),
                containerColor = MaterialTheme.colorScheme.background
            ) {
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
        NavHost(navController = navigationController, startDestination = Screens.Profile.screen,
            modifier = Modifier.padding(paddingValues)) {
            composable(Screens.Profile.screen) { Profile(getCurrentUser()!!) }

        }
    }
}

@Preview
@Composable
fun PreviewNavBar() {
    Navbar()
}