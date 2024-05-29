package com.example.appfall.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appfall.data.models.Fall
import com.example.appfall.databinding.ContactBinding
import com.example.appfall.databinding.FallBinding


class FallAdapter : RecyclerView.Adapter<FallAdapter.FallViewHolder>() {
    private var fallsList = ArrayList<Fall>()

    fun setFalls(fallsList: List<Fall>) {
        this.fallsList = ArrayList(fallsList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FallViewHolder {
        val binding = FallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FallViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return fallsList.size
    }

    override fun onBindViewHolder(holder: FallViewHolder, position: Int) {
        val fall = fallsList[position]
        holder.bind(fall)
    }

    inner class FallViewHolder(private val binding: FallBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(fall: Fall) {
            binding.apply {
                // Example of how to bind the data to the views
                //fallId.text = fall._id
                //fallStatus.text = fall.status
                //fallDate.text = fall.date.toString() // Format the date as needed
                //fallPlace.text = fall.place
            }
        }
    }
}
