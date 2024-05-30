package com.example.appfall.views.fragments

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appfall.adapters.FallAdapter
import com.example.appfall.databinding.FragmentFallsBinding
import com.example.appfall.viewModels.FallsViewModel

class FallsFragment : Fragment() {

    private lateinit var binding: FragmentFallsBinding
    private lateinit var fallViewModel: FallsViewModel
    private lateinit var fallAdapter: FallAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fallViewModel = ViewModelProvider(this)[FallsViewModel::class.java]
        fallAdapter = FallAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFallsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.fallsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fallAdapter
        }

        val userId = "66573f5e99546d34666ac005"
        //val userId = arguments?.getString("userId") ?: return
        val id = arguments?.getString("userId") ?: return
        Log.d("FallsFragmentUserId","$id")
        Log.d("FallsFragmentUserId","*********************************")

        fallViewModel.getFalls(userId)


        observeFalls()
    }

    private fun observeFalls() {
        fallViewModel.observeFallsList().observe(viewLifecycleOwner) { falls ->
            falls?.let {
                fallAdapter.setFalls(ArrayList(it))
            }
        }
    }
}

