package com.example.appfall.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appfall.databinding.ContactBinding
import com.example.appfall.models.ConnectedSupervisor

class ContactsAdapter():RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder>() {
    private var contactsList = ArrayList<ConnectedSupervisor>()

    fun setContacts(contactsList: ArrayList<ConnectedSupervisor>){
        this.contactsList = contactsList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        return ContactsViewHolder(ContactBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun getItemCount(): Int {
        return contactsList.size
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        val contact = contactsList[position]
        holder.binding.apply {
            /*Glide.with(holder.itemView)
                .load(contact.urlImage)
                .into(contactImage)*/
            contactName.text = contact.name
            //contactPhone.text = contact.phone
        }

    }

    class ContactsViewHolder(internal val binding: ContactBinding):RecyclerView.ViewHolder(binding.root)
}