package com.example.appfall.views.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appfall.R
import com.example.appfall.adapters.FallsAdapter
import com.example.appfall.data.models.Fall
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.databinding.FragmentFallsBinding
import com.example.appfall.services.NetworkHelper
import com.example.appfall.services.SmsHelper
import com.example.appfall.viewModels.ContactsViewModel
import com.example.appfall.viewModels.FallsViewModel

class FallsFragment : Fragment() {
    private var _binding: FragmentFallsBinding? = null
    private val binding get() = _binding!!
    private lateinit var fallsViewModel: FallsViewModel
    private lateinit var fallsAdapter: FallsAdapter
    private lateinit var networkHelper: NetworkHelper
    private lateinit var smsHelper: SmsHelper
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize helpers and DAO
        networkHelper = NetworkHelper(requireContext())
        userDao = AppDatabase.getInstance(requireContext()).userDao()
        smsHelper = SmsHelper(requireContext())


        // Initialize ViewModels
        fallsViewModel = ViewModelProvider(this).get(FallsViewModel::class.java)
        contactsViewModel = ViewModelProvider(this).get(ContactsViewModel::class.java)

        // Initialize the adapter
        fallsAdapter = FallsAdapter(fallsViewModel, contactsViewModel, smsHelper, userDao, networkHelper, this, "all")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFallsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up RecyclerView
        binding.fallsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fallsAdapter
        }


        // Initialize data based on network availability
        showProgressBar() // Show the progress bar first

        if (networkHelper.isInternetAvailable()) {
            fallsViewModel.getFalls("all")
            fallsViewModel.observeFallsList().observe(viewLifecycleOwner) { falls ->
                hideProgressBar() // Hide the progress bar when data is received
                handleFalls(falls, "all")
            }
        } else {
            fallsViewModel.getOfflineFalls()
            fallsViewModel.observeOfflineFalls().observe(viewLifecycleOwner) { falls ->
                hideProgressBar() // Hide the progress bar when data is received
                handleFalls(falls, "all")
            }
        }

        // Set up filter button listeners
        binding.btnAll.setOnClickListener { handleFilterButtonClick("all") }
        binding.btnActive.setOnClickListener { handleFilterButtonClick("active") }
        binding.btnRescued.setOnClickListener { handleFilterButtonClick("rescued") }
        binding.btnFalse.setOnClickListener { handleFilterButtonClick("false") }
        binding.btnOffline.setOnClickListener { handleOfflineButtonClick() }
    }

    private fun handleFilterButtonClick(filter: String) {
        showProgressBar()
        binding.fallsList.visibility = View.GONE
        binding.noDataLayout.visibility = View.GONE
        binding.noNetworkLayout.visibility = View.GONE

        if (networkHelper.isInternetAvailable()) {
            fallsViewModel.getFalls(filter)
            setButtonState(findButtonByFilter(filter)) {
                observeFallsList(filter)
            }
        } else {
            if (filter == "all") {
                binding.progressBar.visibility = View.VISIBLE
                fallsViewModel.getOfflineFalls()
                setButtonState(findButtonByFilter(filter)) {
                    observeOfflineFalls()
                }
            } else {
                binding.fallsList.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.noNetworkLayout.visibility = View.VISIBLE
                setButtonState(findButtonByFilter(filter)) {}
            }
        }
    }

    private fun handleOfflineButtonClick() {
        showProgressBar()
        binding.fallsList.visibility = View.GONE
        binding.noDataLayout.visibility = View.GONE
        binding.noNetworkLayout.visibility = View.GONE
        fallsViewModel.getOfflineFalls()
        setButtonState(binding.btnOffline) {
            observeOfflineFalls()
        }
    }

    private fun setButtonState(clickedButton: Button, observerFunction: () -> Unit) {
        val buttons = listOf(binding.btnAll, binding.btnActive, binding.btnRescued, binding.btnFalse, binding.btnOffline)
        buttons.forEach { button ->
            button.setBackgroundResource(R.drawable.rounded_button_filter_empty)
            button.setTextColor(button.context.getColor(R.color.black))
        }

        clickedButton.setBackgroundResource(R.drawable.rounded_button_filter)
        clickedButton.setTextColor(clickedButton.context.getColor(R.color.white))

        observerFunction()
    }

    private fun findButtonByFilter(filter: String): Button {
        return when (filter) {
            "all" -> binding.btnAll
            "active" -> binding.btnActive
            "rescued" -> binding.btnRescued
            "false" -> binding.btnFalse
            else -> binding.btnAll
        }
    }

    private fun observeFallsList(filter: String) {
        fallsViewModel.observeFallsList().observe(viewLifecycleOwner) { falls ->
            handleFalls(falls, filter)
        }
    }

    private fun observeOfflineFalls() {
        fallsViewModel.observeOfflineFalls().observe(viewLifecycleOwner) { falls ->
            handleFalls(falls, "all")
        }
    }

    private fun handleFalls(falls: List<Fall>?, filter: String) {
        binding.progressBarLayout.visibility = View.GONE
        when {
            falls == null -> {
                binding.errorTextViewLayout.visibility = View.VISIBLE
                binding.fallsList.visibility = View.GONE
                binding.noDataLayout.visibility = View.GONE
                binding.noNetworkLayout.visibility = View.GONE
            }
            falls.isEmpty() -> {
                binding.errorTextViewLayout.visibility = View.GONE
                binding.fallsList.visibility = View.GONE
                binding.noDataLayout.visibility = View.VISIBLE
                binding.noNetworkLayout.visibility = View.GONE
            }
            else -> {
                fallsAdapter.setFalls(ArrayList(falls), filter)  // Update adapter with filtered list
                binding.noDataLayout.visibility = View.GONE
                binding.fallsList.visibility = View.VISIBLE
                binding.noNetworkLayout.visibility = View.GONE
            }
        }
    }

    private fun showProgressBar() {
        binding.progressBarLayout.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBarLayout.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
