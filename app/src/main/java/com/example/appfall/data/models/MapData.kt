package com.example.appfall.data.models

import com.mapbox.geojson.Point

data class MapData(
    val point: Point,
    val iconId: String,
    val iconDrawable: Int,
    val sourceId: String,
    val layerId: String
)
