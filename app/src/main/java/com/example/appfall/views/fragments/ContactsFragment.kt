package com.example.appfall.views.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appfall.adapters.ContactsAdapter
import com.example.appfall.databinding.FragmentContactsBinding
import com.example.appfall.services.NetworkHelper
import com.example.appfall.viewModels.ContactsViewModel

class ContactsFragment : Fragment() {

    private lateinit var binding: FragmentContactsBinding
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var networkHelper: NetworkHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize ViewModel and other dependencies
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

        setupRecyclerView()
        observeContacts()
        loadContacts()
    }

    private fun setupRecyclerView() {
        binding.contactsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contactsAdapter
        }
    }

    private fun loadContacts() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contactsList.visibility = View.GONE

        if (networkHelper.isInternetAvailable()) {
            binding.noNetworkLayout.visibility = View.GONE
            Log.d("ContactsFragment", "Loading contacts from network")
            contactsViewModel.getContactsNetwork()
        } else {
            binding.noNetworkLayout.visibility = View.VISIBLE
            Log.d("ContactsFragment", "Loading contacts from local storage")
            contactsViewModel.getContactsOffline()
        }
    }

    private fun observeContacts() {
        contactsViewModel.observeContactsList().observe(viewLifecycleOwner) { contacts ->
            binding.progressBar.visibility = View.GONE

            if (contacts == null) {
                Log.d("ContactsFragment", "Contacts are null")
                binding.noContactsText.visibility = View.GONE
                binding.contactsList.visibility = View.GONE
                binding.errorTextViewLayout.visibility = View.VISIBLE
            } else if (contacts.isEmpty()) {
                Log.d("ContactsFragment", "Contacts are empty")
                binding.noContactsText.visibility = View.VISIBLE
                binding.contactsList.visibility = View.GONE
                binding.errorTextViewLayout.visibility = View.GONE
            } else {
                Log.d("ContactsFragment", "Contacts are loaded: $contacts")
                binding.noContactsText.visibility = View.GONE
                binding.contactsList.visibility = View.VISIBLE
                binding.errorTextViewLayout.visibility = View.GONE
                contactsAdapter.setContacts(contacts)
            }
        }
    }
}
