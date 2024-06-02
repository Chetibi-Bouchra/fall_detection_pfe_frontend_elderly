package com.example.appfall.views.activities

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.example.appfall.R
import com.example.appfall.databinding.ActivityParametersBinding

class ParametersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityParametersBinding
    private var headerText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParametersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        headerText = binding.textHeader

        val navController = Navigation.findNavController(this, R.id.parameters_fragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Update header title based on destination
            when (destination.id) {
                R.id.nameFragment -> updateHeaderTitle("Changer le nom")
                R.id.passwordFragment -> updateHeaderTitle("Changer le mot de passe")
                R.id.fallsFragment -> updateHeaderTitle("Visualiser les chutes")
                R.id.parametersMainFragment -> updateHeaderTitle("Paramètres")
            }
        }

        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Handle back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navController.popBackStack()) {
                    finish()
                }
            }
        })
    }

    private fun updateHeaderTitle(title: String) {
        headerText?.text = title
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.parameters_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
