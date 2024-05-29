package com.example.appfall.views.fragments

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
    private lateinit var bouton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = view.findViewById(R.id.mapView)

        bouton = view.findViewById(R.id.bouton)
        bouton.setOnClickListener {
            if (viewModel.isConnected.value == false) {
                showConfirmationDialog()
            } else {
                viewModel.onButtonClick()
            }
        }

        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            updateBoutonText(isConnected)
        }

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

    // Fonction pour mettre à jour le texte du bouton en fonction de l'état actuel
    private fun updateBoutonText(isConnected: Boolean) {
        bouton.text = if (isConnected) "Connecter" else "Déconnecter"
    }

    // Affiche une boîte de dialogue de confirmation pour la déconnexion
    private fun showConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirmation, null)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Centrer le titre et ajuster la largeur de la boîte de dialogue
        dialogBuilder.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBuilder.show()
        dialogBuilder.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val confirmButton = dialogView.findViewById<Button>(R.id.dialogConfirmButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.dialogCancelButton)

        // Configurer les actions des boutons
        confirmButton.setOnClickListener {
            viewModel.onButtonClick()
            dialogBuilder.dismiss()
        }

        cancelButton.setOnClickListener {
            dialogBuilder.dismiss()
        }
    }


}
