package com.example.jetpackmap.api

import com.example.jetpackmap.model.AutocompleteResponse
import com.example.jetpackmap.model.NearbySpotResponse
import com.example.jetpackmap.utils.Constants
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET(Constants.NEARBY_SEARCH)
    suspend fun fetchNearbySpots(
        @Query(Constants.LAYERS) layers: String,
        @Query(Constants.TYPES) types: String,
        @Query(Constants.LOCATION) location: String,
        @Query(Constants.API_KEY) apiKey: String
    ): NearbySpotResponse

    @GET(Constants.AUTO_COMPLETE)
    suspend fun getAutocomplete(
        @Query(Constants.LOCATION) location: String,
        @Query(Constants.INPUT) input: String,
        @Query(Constants.API_KEY) apiKey: String
    ): AutocompleteResponse
}