package com.example.appfall.views.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appfall.R
import com.example.appfall.databinding.FragmentParametersMainBinding
import com.example.appfall.viewModels.UserViewModel
import com.example.appfall.views.activities.AuthenticationActivity
import com.example.appfall.views.activities.MainActivity

class ParametersMainFragment : Fragment() {

    private var _binding: FragmentParametersMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParametersMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        binding.layoutChangeProfileName.setOnClickListener {
            navigateToChangeProfileName()
        }

        binding.layoutChangePassword.setOnClickListener {
            navigateToChangePassword()
        }

        binding.layoutViewFalls.setOnClickListener {
            navigateToViewFalls()
        }

        binding.layoutLogout.setOnClickListener {
            println("hellooo")
            logoutUser()
        }
    }

    private fun navigateToChangeProfileName() {
        findNavController().navigate(R.id.action_parametersMainFragment_to_nameFragment)
    }

    private fun navigateToChangePassword() {
        findNavController().navigate(R.id.action_parametersMainFragment_to_passwordFragment)
    }

    private fun navigateToViewFalls() {
        findNavController().navigate(R.id.action_parametersMainFragment_to_fallsFragment)
    }

    private fun logoutUser() {
        userViewModel.logout()
        val intent = Intent(requireContext(), AuthenticationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
