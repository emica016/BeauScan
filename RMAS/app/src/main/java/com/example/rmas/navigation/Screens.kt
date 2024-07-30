package com.example.rmas.navigation

sealed class Screens(val screen: String) {
    data object Home: Screens("home")
    data object RequestDetails: Screens("request_details")

    data object CreateRequest:Screens("create_request")
    data object Leaderboard: Screens("leaderboard")
    data object Profile: Screens("profile")
    data object PlaceInfo: Screens("eventinfo")
}