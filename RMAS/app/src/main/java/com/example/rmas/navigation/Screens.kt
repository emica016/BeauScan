package com.example.rmas.navigation

sealed class Screens(val screen: String) {
    data object Home: Screens("home")


    data object CreateRequest:Screens("create_request")
    data object LeaderboardScreen: Screens("leaderboard")
    data object Profile: Screens("profile")
    data object MapScreen: Screens("map")

    data object MyPlaces: Screens("my_places")
}