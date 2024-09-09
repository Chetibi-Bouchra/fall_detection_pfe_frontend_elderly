package com.example.appfall.adapters

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.appfall.R
import com.example.appfall.databinding.ContactBinding
import com.example.appfall.data.models.ConnectedSupervisor
import kotlin.random.Random

class ContactsAdapter : RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder>() {
    private var contactsList = ArrayList<ConnectedSupervisor>()
    private val imageResources = arrayOf(
        R.drawable.image1,
        R.drawable.image2,

    )

    fun setContacts(contactsList: List<ConnectedSupervisor>?) {
        Log.d("ContactsAdapter", "Received contacts: $contactsList")
        this.contactsList = contactsList?.toCollection(ArrayList()) ?: arrayListOf()
        Log.d("ContactsAdapter", "Updated contactsList: $contactsList")
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val binding = ContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactsViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return contactsList.size
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        val contact = contactsList[position]
        Log.d("ContactsAdapterContact", "Contact: $contact")
        holder.bind(contact)
    }

    inner class ContactsViewHolder(private val binding: ContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Set OnClickListener to the phone number TextView
            binding.contactPhone.setOnClickListener {
                val phoneNumber = binding.contactPhone.text.toString()
                // Call the phone number
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                binding.root.context.startActivity(intent)
            }
        }

        fun bind(contact: ConnectedSupervisor) {
            binding.apply {
                Log.d("ContactsAdapterContact", "Contact: ${contact}")
                contactName.text = contact.name
                contactPhone.text = contact.phone

                // Select a random image from the predefined set and load it using Glide
                val randomImageResId = imageResources[Random.nextInt(imageResources.size)]
                Glide.with(itemView.context).load(randomImageResId).into(contactImage)
            }
        }
    }
}
