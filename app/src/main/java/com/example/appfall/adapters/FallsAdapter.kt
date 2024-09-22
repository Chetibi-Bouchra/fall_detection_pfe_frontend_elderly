package com.example.appfall.adapters

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.appfall.R
import com.example.appfall.databinding.FallBinding
import com.example.appfall.data.models.Fall
import com.example.appfall.data.models.Notification
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.services.NetworkHelper
import com.example.appfall.services.SmsHelper
import com.example.appfall.viewModels.ContactsViewModel
import com.example.appfall.viewModels.FallsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class FallsAdapter(
    private val fallsViewModel: FallsViewModel,
    private val contactsViewModel: ContactsViewModel,
    private val smsHelper: SmsHelper,
    private val userDao: UserDao,
    private val networkHelper: NetworkHelper,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<FallsAdapter.FallsViewHolder>() {

    private var fallsList = ArrayList<Fall>()
    private var counter = 1

    fun setFalls(fallsList: ArrayList<Fall>) {
        this.fallsList = fallsList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FallsViewHolder {
        val binding = FallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FallsViewHolder(binding)
    }

    override fun getItemCount(): Int = fallsList.size

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: FallsViewHolder, position: Int) {
        val fall = fallsList[position]
        holder.bind(fall)
    }

    inner class FallsViewHolder(private val binding: FallBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                // Handle click event here if needed
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(fall: Fall) {
            binding.apply {
                // Extract date and time
                val dateTimeString = fall.dateTime
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

                try {
                    val dateTime = ZonedDateTime.parse(dateTimeString)
                    fallDate.text = dateTime.toLocalDate().format(dateFormatter)
                    fallTime.text = dateTime.toLocalTime().format(timeFormatter)
                } catch (e: DateTimeParseException) {
                    fallDate.text = "Invalid Date"
                    fallTime.text = "Invalid Time"
                }

                // Update title and status
                fallTitle.text = "Chute ${counter++}"
                fallStatus.text = when (fall.status) {
                    "rescued" -> "traitée"
                    "active" -> "active"
                    "false" -> "fausse"
                    else -> fall.status
                }

                // Update location link
                fallLocation.text = "https://maps.google.com/?q=${fall.place.latitude},${fall.place.longitude}"

                // Set background color based on status
                val context = binding.root.context
                val backgroundColor = when (fall.status) {
                    "rescued" -> ContextCompat.getColor(context, R.color.colorRescued)
                    "active" -> ContextCompat.getColor(context, R.color.colorActive)
                    "false" -> ContextCompat.getColor(context, R.color.colorFalse)
                    else -> ContextCompat.getColor(context, R.color.white)
                }
                binding.constraintLayout.setBackgroundColor(backgroundColor)

                // Expand/Collapse functionality
                expandArrow.setOnClickListener {
                    if (expandableView.visibility == View.GONE) {
                        expandableView.visibility = View.VISIBLE
                        expandArrow.setImageResource(R.drawable.ic_collapse_arrow)
                        handleExpandCollapseButtons(fall)
                    } else {
                        expandableView.visibility = View.GONE
                        expandArrow.setImageResource(R.drawable.ic_expand_arrow)
                    }
                }

                // Set click listener for location to open in browser
                fallLocation.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fallLocation.text.toString()))
                    it.context.startActivity(intent)
                }
            }
        }

        private fun handleExpandCollapseButtons(fall: Fall) {
            binding.apply {
                if (fall.status == "active") {
                    btnRescued.visibility = View.VISIBLE
                    btnFalse.visibility = View.VISIBLE

                    btnFalse.setOnClickListener {
                        fallsViewModel.updateFallStatus(fall._id, "false")
                        updateFallStatusText("fausse")
                        sendNotification("Fausse")
                    }
                    btnRescued.setOnClickListener {
                        fallsViewModel.updateFallStatus(fall._id, "rescued")
                        updateFallStatusText("traitée")
                        sendNotification("Traitée")
                    }
                } else {
                    btnRescued.visibility = View.GONE
                    btnFalse.visibility = View.GONE
                    updateFallStatusText(fall.status)
                }
            }
        }

        private fun updateFallStatusText(status: String) {
            binding.statusText.visibility = View.VISIBLE
            binding.statusText.text = when (status) {
                "rescued" -> "traitée"
                "active" -> "active"
                "false" -> "fausse"
                else -> status
            }
        }
    }

    private fun sendNotification(message: String) {
        if (networkHelper.isInternetAvailable()) {
            sendPushNotification(message)
        } else {
            //sendSMS(message)
        }
    }

    private fun sendPushNotification(message: String) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val user = userDao.getUser()
            val notification = user?.let {
                Notification(
                    topic = "news",
                    title = "Notification de chute",
                    message = message
                )
            }
            notification?.let {
                withContext(Dispatchers.Main) {
                    fallsViewModel.sendNotification(it)
                }
            }
        }
    }

    private fun sendSMS(message: String) {
        Log.d("SMS1", "Starting SMS sending process")

        lifecycleOwner.lifecycleScope.launch {
            contactsViewModel.getContacts()
            contactsViewModel.observeContactsList().observe(lifecycleOwner, Observer { contacts ->
                contacts?.let { contactList ->
                    if (contactList.isEmpty()) {
                        Log.d("SMS_Sender", "No contacts available")
                        return@let
                    }

                    contactList.forEach { contact ->
                        if (contact.phone.isNotEmpty()) {
                            smsHelper.sendSMS(contact.phone, message)
                            Log.d("SMS_Sender", "SMS sent to: ${contact.phone}")
                        } else {
                            Log.d("SMS_Sender", "Skipping empty phone number")
                        }
                    }
                } ?: run {
                    Log.d("SMS_Sender", "No contacts available")
                }
            })
        }
    }
}
