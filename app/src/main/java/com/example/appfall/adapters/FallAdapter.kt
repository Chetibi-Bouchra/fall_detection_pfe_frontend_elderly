package com.example.appfall.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appfall.R
import com.example.appfall.data.models.Fall
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

        private var isExpanded = false

        init {
            binding.root.setOnClickListener {
                isExpanded = !isExpanded
                binding.expandedLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
                binding.expandIcon.setImageResource(
                    if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                )
            }
        }

        fun bind(fall: Fall) {
            binding.fall = fall // This binds the 'fall' variable to the layout
            binding.executePendingBindings()
            binding.expandedLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.expandIcon.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }
    }
}
