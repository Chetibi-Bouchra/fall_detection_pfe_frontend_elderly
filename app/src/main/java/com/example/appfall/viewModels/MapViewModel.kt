package com.example.appfall.viewModels


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appfall.R
import com.example.appfall.data.models.MapData
import com.mapbox.geojson.Point



class MapViewModel : ViewModel() {
    private val _mapData = MutableLiveData<MapData>()
    val mapData: LiveData<MapData> = _mapData

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    init {
        _isConnected.value = false // Initial state: not connected
    }

    fun initializeMap() {
        // Perform data retrieval and manipulation here
        val point = Point.fromLngLat(-122.4194 , 37.7749)
        val mapData = MapData(
            point,
            "marker-icon-id",
            R.drawable.localisation_icon,
            "marker-source-id",
            "marker-layer-id"
        )
        _mapData.postValue(mapData)
    }

    fun onButtonClick() {
        _isConnected.value = !(_isConnected.value ?: false)
    }
}



