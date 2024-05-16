package com.example.appfall.views.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.example.appfall.R
import com.example.appfall.data.models.UserCredential
import com.example.appfall.databinding.FragmentSignInBinding
import com.example.appfall.viewModels.UserViewModel

class SignInFragment : Fragment() {

    private val viewModel: UserViewModel by viewModels()
    private lateinit var progressBar: ProgressBar
    private lateinit var binding: FragmentSignInBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = binding.loginProgressBar
        binding.buttonLogin.setOnClickListener {
            progressBar.visibility = View.VISIBLE

            val password = binding.editTextPassword.text.toString()
            val phone = binding.editTextPhone.text.toString()

            val user = UserCredential(phone,password)
            binding.container.visibility = View.GONE
            viewModel.loginUser(user)
        }

        binding.textRegisterAction.setOnClickListener {
            it.findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }

        observeLoginResponse()
    }

    private fun observeLoginResponse() {
        viewModel.loginResponse.observe(viewLifecycleOwner) { loginResponse ->
            progressBar.visibility = View.GONE
            binding.container.visibility = View.VISIBLE

            Log.d("login",loginResponse.toString())
            if ((loginResponse != null) && (loginResponse.status == "true")) {
                val errorMessage = "Very good"
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            } else {
                val errorMessage = "An error occurred. Please try again."
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
