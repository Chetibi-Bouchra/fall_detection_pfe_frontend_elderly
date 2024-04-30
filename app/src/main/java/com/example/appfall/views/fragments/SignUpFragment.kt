package com.example.appfall.views.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.example.appfall.R
import com.example.appfall.data.models.User
import com.example.appfall.databinding.FragmentSignUpBinding
import com.example.appfall.viewModels.UserViewModel

class SignUpFragment : Fragment() {

    private lateinit var binding: FragmentSignUpBinding
    private val viewModel: UserViewModel by viewModels()
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = binding.loginProgressBar
        binding.buttonSignup.setOnClickListener {
            progressBar.visibility = View.VISIBLE

            val name = binding.editTextName.text.toString()
            val password = binding.editTextPassword.text.toString()
            val phone = binding.editTextPhone.text.toString()

            val user = User(name, password, phone)
            binding.container.visibility = View.GONE
            viewModel.addUser(user)
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
