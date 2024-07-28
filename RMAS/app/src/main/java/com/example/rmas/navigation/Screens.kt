package com.example.rmas.navigation

sealed class Screens(val screen: String) {
    data object Home: Screens("home")
    data object Search: Screens("search")
    data object Leaderboard: Screens("leaderboard")
    data object Profile: Screens("profile")
    data object EventInfo: Screens("eventinfo")
}