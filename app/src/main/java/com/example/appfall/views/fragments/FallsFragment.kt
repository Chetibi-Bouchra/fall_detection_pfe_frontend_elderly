package com.example.appfall.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appfall.R
import com.example.appfall.adapters.FallsAdapter
import com.example.appfall.databinding.FragmentFallsBinding
import com.example.appfall.services.NetworkHelper
import com.example.appfall.viewModels.FallsViewModel

class FallsFragment : Fragment() {
    private var _binding: FragmentFallsBinding? = null
    private val binding get() = _binding!!
    private lateinit var fallsViewModel: FallsViewModel
    private lateinit var fallsAdapter: FallsAdapter
    private lateinit var networkHelper: NetworkHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fallsViewModel = ViewModelProvider(this).get(FallsViewModel::class.java)
        fallsAdapter = FallsAdapter(fallsViewModel)
        networkHelper = NetworkHelper(requireContext())
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

        binding.fallsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fallsAdapter
        }

        observeFalls()

        binding.btnAll.setOnClickListener {
            if(networkHelper.isInternetAvailable()) {
                setButtonState(binding.btnAll) { observeFalls() }
            }
            else {
                setButtonState(binding.btnAll) { observeOfflineFalls() }
            }
        }

        binding.btnActive.setOnClickListener {
            if(networkHelper.isInternetAvailable()) {
                setButtonState(binding.btnActive) { observeActiveFalls() }
            }
            else {
                binding.fallsList.visibility = View.GONE
                binding.noNetworkLayout.visibility = View.VISIBLE
            }
        }

        binding.btnRescued.setOnClickListener {
            if(networkHelper.isInternetAvailable()) {
                setButtonState(binding.btnRescued) { observeRescuedFalls() }
            }
            else {
                binding.fallsList.visibility = View.GONE
                binding.noNetworkLayout.visibility = View.VISIBLE
            }

        }

        binding.btnOffline.setOnClickListener {
            setButtonState(binding.btnOffline) { observeOfflineFalls() }
        }

        binding.btnFalse.setOnClickListener {
            if(networkHelper.isInternetAvailable()) {
                setButtonState(binding.btnFalse) { observeFalseFalls() }
            }
            else {
                binding.fallsList.visibility = View.GONE
                binding.noNetworkLayout.visibility = View.VISIBLE
            }

        }

    }


    private fun setButtonState(clickedButton: Button, observerFunction: () -> Unit) {
        // Reset background and text color for all buttons
        val buttons = listOf(binding.btnAll, binding.btnActive, binding.btnRescued, binding.btnOffline, binding.btnFalse)
        for (button in buttons) {
            button.setBackgroundResource(R.drawable.rounded_button_filter_empty)
            button.setTextColor(button.context.getColor(R.color.black))
        }

        // Set background and text color for the clicked button
        clickedButton.setBackgroundResource(R.drawable.rounded_button_filter)
        clickedButton.setTextColor(clickedButton.context.getColor(R.color.white))

        // Call the observer function associated with the clicked button
        observerFunction()
    }

    private fun observeFalls() {
        fallsViewModel.observeFallsList().observe(viewLifecycleOwner) { falls ->

            if (falls.isEmpty()) {
                View.VISIBLE
            } else {
                falls?.let {
                    fallsAdapter.setFalls(ArrayList(it))
                }
            }

        }
    }

    private fun observeActiveFalls() {
        fallsViewModel.getActiveFalls().observe(viewLifecycleOwner) { falls ->
            falls?.let {
                fallsAdapter.setFalls(ArrayList(it))
            }
        }
    }

    private fun observeRescuedFalls() {
        fallsViewModel.getRescuedFalls().observe(viewLifecycleOwner) { falls ->
            falls?.let {
                fallsAdapter.setFalls(ArrayList(it))
            }
        }
    }

    private fun observeFalseFalls() {
        fallsViewModel.getFalseFalls().observe(viewLifecycleOwner) { falls ->
            falls?.let {
                fallsAdapter.setFalls(ArrayList(it))
            }
        }
    }

    private fun observeOfflineFalls() {
        fallsViewModel.observeOfflineFalls().observe(viewLifecycleOwner) { falls ->
            falls?.let {
                fallsAdapter.setFalls(ArrayList(it))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
