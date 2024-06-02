package com.example.appfall.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appfall.R
import com.example.appfall.databinding.FallBinding
import com.example.appfall.data.models.Fall
import com.example.appfall.viewModels.FallsViewModel

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

        fun bind(fall: Fall) {
            binding.apply {

                fallDate.text = "30-05-2024"
                fallTitle.text = "Fall ${counter++}"
                fallStatus.text = fall.status
                fallLocation.text = "https://maps.google.com/?q=${fall.place.latitude},${fall.place.longitude}"
                fallTime.text = "09:06"

                val context = binding.root.context
                val backgroundColor = when (fall.status) {
                    "rescued" -> ContextCompat.getColor(context, R.color.colorRescued)
                    "active" -> ContextCompat.getColor(context, R.color.colorActive)
                    "false" -> ContextCompat.getColor(context, R.color.colorFalse)
                    else -> ContextCompat.getColor(context, R.color.white)
                }
                binding.root.setBackgroundColor(backgroundColor)

                expandArrow.setOnClickListener {
                    if (expandableView.visibility == View.GONE) {
                        expandableView.visibility = View.VISIBLE
                        expandArrow.setImageResource(R.drawable.ic_collapse_arrow)
                        if (fall.status == "active") {
                            btnRescued.visibility = View.VISIBLE
                            btnFalse.visibility = View.VISIBLE
                            binding.btnFalse.setOnClickListener {
                                fallsViewModel.updateFallStatus(fall._id, "false")
                                statusText.visibility = View.VISIBLE
                                statusText.text = "false"
                                btnRescued.visibility = View.GONE
                                btnFalse.visibility = View.GONE
                            }
                            binding.btnRescued.setOnClickListener {
                                fallsViewModel.updateFallStatus(fall._id, "rescued")
                                statusText.visibility = View.VISIBLE
                                statusText.text = "rescued"
                                btnRescued.visibility = View.GONE
                                btnFalse.visibility = View.GONE
                            }
                        }
                        else {
                            btnRescued.visibility = View.GONE
                            btnFalse.visibility = View.GONE
                            statusText.visibility = View.VISIBLE
                            statusText.text = fall.status
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
