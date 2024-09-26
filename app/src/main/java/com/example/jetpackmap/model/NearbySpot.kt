package com.example.jetpackmap.model

data class NearbySpot(
    val placeId: String,
    val mainText: String,
    val secondaryText: String,
    val distanceMeters: Int
)

data class Prediction(
    val place_id: String,
    val structured_formatting: StructuredFormatting,
    val geometry: Geometry
)

data class StructuredFormatting(
    val main_text: String,
    val secondary_text: String
)

data class Geometry(
    val location: Location
)

data class Location(
    val lat: Double,
    val lng: Double
)

data class AutocompleteResponse(
    val predictions: List<Prediction>
)


data class NearbySpotResponse(
    val predictions: List<Prediction2>,
    val info_messages: List<String>?,
    val error_message: String?,
    val status: String
)

data class Prediction2(
    val description: String,
    val matched_substrings: List<Any>, // Assuming it's an empty array, but can be adjusted if data is available
    val place_id: String,
    val reference: String,
    val structured_formatting: StructuredFormatting,
    val terms: List<Term>,
    val types: List<String>,
    val layer: List<String>,
    val distance_meters: Int
)

data class Term(
    val offset: Int,
    val value: String
)


