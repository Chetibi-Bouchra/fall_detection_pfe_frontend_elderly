package com.example.appfall.fragments

import com.example.appfall.viewModels.ContactsViewModel
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.example.appfall.databinding.FragmentContactsBinding

class ContactsFragment : Fragment() {

    private lateinit var binding: FragmentContactsBinding
    private lateinit var contactsViewModel: ContactsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactsViewModel = ViewModelProviders.of(this)[ContactsViewModel::class.java]
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

        contactsViewModel = ViewModelProvider(this)[ContactsViewModel::class.java]

        binding.apply { apiTest.text = "Before Retrofit" }

        observeContacts()
    }

    private fun observeContacts() {
        contactsViewModel.observeContactsList().observe(viewLifecycleOwner
        ) { value -> binding.apply { apiTest.text = value[0].name } }
    }
}
