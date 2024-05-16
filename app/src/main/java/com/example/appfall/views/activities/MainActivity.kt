package com.example.appfall.views.activities


import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.example.appfall.R
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.databinding.ActivityMainBinding
import com.example.appfall.views.fragments.CodeFragment
import com.example.appfall.views.fragments.ContactsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var headerText: TextView? = null
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        headerText = binding.textHeader
        

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.btm_nav)
        val navController = Navigation.findNavController(this, R.id.host_fragment)

        // Setup bottom navigation with NavController
        NavigationUI.setupWithNavController(bottomNavigation, navController)

        // Set up destination changed listener to update header title
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Update header title based on destination
            when (destination.id) {
                R.id.contactsFragment -> {
                    // Update header title to "Contacts" when ContactsFragment is displayed
                    updateHeaderTitle("Contacts")
                }
                R.id.codeFragment -> {
                    // Update header title to "Other" when OtherFragment is displayed
                    updateHeaderTitle("QR Code")
                }
                // Add more cases for other fragments as needed
            }
        }
    }

    private fun updateHeaderTitle(title: String) {
        // Find the TextView for the header
        val headerTextView = binding.textHeader
        // Update the header title
        headerTextView.text = title
    }

}