package com.example.appfall.views.fragments

import com.example.appfall.viewModels.ContactsViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appfall.adapters.ContactsAdapter
import com.example.appfall.databinding.FragmentContactsBinding
import com.example.appfall.services.NetworkHelper
import com.example.appfall.websockets.WebSocketManager

class ContactsFragment : Fragment() {

    private lateinit var binding: FragmentContactsBinding
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var networkHelper: NetworkHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactsViewModel = ViewModelProvider(this)[ContactsViewModel::class.java]
        contactsAdapter = ContactsAdapter()
        networkHelper = NetworkHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (networkHelper.isInternetAvailable()) {

            binding.noNetworkLayout.visibility = View.GONE
            binding.contactsList.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = contactsAdapter
            }

            observeContacts()
            loadContacts()

        } else {
            binding.noNetworkLayout.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.contactsList.visibility = View.VISIBLE
            binding.noContactsText.visibility = View.VISIBLE
        }

    }

    private fun loadContacts() {
        binding.progressBar.visibility = View.VISIBLE
        contactsViewModel.getContacts()
    }

    private fun observeContacts() {
        contactsViewModel.observeContactsList().observe(viewLifecycleOwner) { contacts ->
            binding.progressBar.visibility = View.GONE
            if (contacts.isNullOrEmpty()) {
                binding.contactsList.visibility = View.GONE
                binding.noContactsText.visibility = View.VISIBLE
            } else {
                binding.noContactsText.visibility = View.GONE
                binding.contactsList.visibility = View.VISIBLE
                contactsAdapter.setContacts(ArrayList(contacts))
            }
        }
    }
}
