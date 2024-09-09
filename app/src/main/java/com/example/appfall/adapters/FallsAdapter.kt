package com.example.appfall.adapters

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appfall.R
import com.example.appfall.databinding.FallBinding
import com.example.appfall.data.models.Fall
import com.example.appfall.viewModels.FallsViewModel
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class FallsAdapter(private val fallsViewModel: FallsViewModel) : RecyclerView.Adapter<FallsAdapter.FallsViewHolder>() {
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

    override fun getItemCount(): Int {
        return fallsList.size
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: FallsViewHolder, position: Int) {
        val fall = fallsList[position]
        holder.bind(fall)
    }

    inner class FallsViewHolder(private val binding: FallBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Set OnClickListener for the entire item view if needed
            binding.root.setOnClickListener {
                // Handle click event here, e.g., navigate to fall details
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
                    val dateTime = ZonedDateTime.parse(dateTimeString) // Parses the ISO 8601 format including timezone
                    val date = dateTime.toLocalDate().format(dateFormatter)
                    val time = dateTime.toLocalTime().format(timeFormatter)
                    fallDate.text = date
                    fallTime.text = time
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
                    else -> fall.status // Default case
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
                        if (fall.status == "active") {
                            btnRescued.visibility = View.VISIBLE
                            btnFalse.visibility = View.VISIBLE
                            btnFalse.setOnClickListener {
                                fallsViewModel.updateFallStatus(fall._id, "false")
                                btnRescued.visibility = View.GONE
                                btnFalse.visibility = View.GONE
                                statusText.visibility = View.VISIBLE
                                statusText.text = "fausse"
                            }
                            btnRescued.setOnClickListener {
                                fallsViewModel.updateFallStatus(fall._id, "rescued")
                                btnRescued.visibility = View.GONE
                                btnFalse.visibility = View.GONE
                                statusText.visibility = View.VISIBLE
                                statusText.text = "traitée"

                            }
                        } else {
                            btnRescued.visibility = View.GONE
                            btnFalse.visibility = View.GONE
                            statusText.visibility = View.VISIBLE
                            statusText.text = when (fall.status) {
                                "rescued" -> "traitée"
                                "active" -> "active"
                                "false" -> "fausse"
                                else -> fall.status // Default case
                            }
                        }
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
    }
}
