package com.example.rmas.data
class User {
    var id: String = ""
    var email: String = ""
    var username: String = ""
    var fullName: String = ""
    var phoneNumber: String = ""
    var image: String = ""
    var points: Int = 0 // povecava se za 3 kad mu neko lajkuje korisnu dodelu, oduzima mu 2 kad nego dislajkuje
}