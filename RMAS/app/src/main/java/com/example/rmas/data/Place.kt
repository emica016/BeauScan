package com.example.rmas.data

//import org.osmdroid.util.GeoPoint
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import org.osmdroid.views.overlay.OverlayItem

data class Place(
    @DocumentId var id: String = "",
    var name: String = "",
    var type: String = PlaceType.Store.toString(),
    var description: String="",
    var purpose: String= PlacePurpose.Service.toString(),
    var creatorID: String?="",
    var location: GeoPoint = GeoPoint(0.0,0.0),
    var dateCreated: String="",
    var timeCreated : String="",
    var comments: HashMap<String,String> = HashMap(),
    var ratingNum: Int = 0,
    var rating: Double=0.0,
    var imageUrl: String? = null
)


// Enum class for Place types
enum class PlaceType {
    Cosmetics,
    Hairdresser,
    Store,
    ParfumeStore

}

// Enum class for Place purposes
enum class PlacePurpose {
    Treatmant,
    Service,
    Shopping,
    Product_review
}

data class FilterOptions(
    //val selectedBrands: List<String>,
    val selectedTypes: List<String>,
    val selectedPurposes: List<String>,
    val startDate: String?,
    val endDate: String?,
    val startTime: String?,
    val endTime: String?,
    val radius: Double,
    val isLastInteractionSelected: Boolean
)