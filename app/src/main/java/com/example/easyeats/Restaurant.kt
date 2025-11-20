package com.example.easyeats

import java.io.Serializable

data class Restaurant(
    val name: String,
    val rating: Double?,
    val photoUrl: String?,
    val address: String,
    val lat: Double,
    val lng: Double
    //val distanceFromMe: Double
) : Serializable