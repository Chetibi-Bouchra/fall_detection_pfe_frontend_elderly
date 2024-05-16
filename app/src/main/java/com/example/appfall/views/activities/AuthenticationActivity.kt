package com.example.appfall.views.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.appfall.R
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthenticationActivity : AppCompatActivity() {
    private val loggedIn = false
    private lateinit var userDao: UserDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_authentication)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userDao = AppDatabase.getInstance(this).userDao()

        /*GlobalScope.launch(Dispatchers.Main) {
            val user = withContext(Dispatchers.IO) {
                userDao.getUser()
            }
            if (user != null) {
                // User exists, navigate to MainActivity
                startActivity(Intent(this@MainActivity, MainActivity::class.java))
                finish()
            } else {
                // User does not exist, navigate to AuthenticationActivity
                startActivity(Intent(this@MainActivity, AuthenticationActivity::class.java))
                finish()
            }
        }*/

        // Navigate to MainActivity if user is logged in, otherwise navigate to AuthenticationActivity
        if (loggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, AuthenticationActivity::class.java))
        }
        finish() // Finish current activity to prevent going back
    }
}