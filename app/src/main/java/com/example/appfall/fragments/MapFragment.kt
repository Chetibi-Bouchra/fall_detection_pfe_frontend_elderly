package com.example.appfall.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.appfall.R
import com.example.appfall.viewModels.MapViewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource


class MapFragment : Fragment() {
    private lateinit var mapView: MapView
    private val viewModel: MapViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = view.findViewById(R.id.mapView)

        viewModel.mapData.observe(viewLifecycleOwner) { mapData ->
            // Update UI with map data
            // For example: add marker, move camera, etc.
            mapView.mapboxMap.getStyle { style ->
                style.addImage(mapData.iconId, BitmapFactory.decodeResource(resources, mapData.iconDrawable))
                style.addSource(
                    geoJsonSource(mapData.sourceId) {
                        featureCollection(FeatureCollection.fromFeature(Feature.fromGeometry(mapData.point)))
                    }
                )
                style.addLayer(
                    symbolLayer(mapData.layerId, mapData.sourceId) {
                        iconImage(mapData.iconId)
                        iconAnchor(IconAnchor.BOTTOM)
                        iconAllowOverlap(true)
                        iconOffset(listOf(0.0, -9.0))
                        iconSize(0.05)
                        iconOpacity(1.0)
                        visibility(Visibility.VISIBLE)
                    }
                )
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(mapData.point)
                        .zoom(12.0)
                        .build()
                )
            }
        }

        viewModel.initializeMap()

        return view
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
