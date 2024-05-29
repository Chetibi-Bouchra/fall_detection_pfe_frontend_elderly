package com.example.appfall.views.fragments

import androidx.fragment.app.Fragment

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.example.appfall.R
import com.example.appfall.data.models.User
import com.example.appfall.databinding.FragmentSignUpBinding
import com.example.appfall.viewModels.UserViewModel
import com.example.appfall.views.activities.MainActivity

class SignUpFragment : Fragment() {

    private lateinit var binding: FragmentSignUpBinding
    private val viewModel: UserViewModel by viewModels()
    private lateinit var progressBar: ProgressBar
    private lateinit var incorrectDrawable: Drawable
    private lateinit var correctDrawable: Drawable

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

        val passwordEditText = binding.editTextPassword
        val phoneEditText = binding.editTextPhone
        val nameEditText = binding.editTextName
        val emailEditText = binding.editTextEmail
        val confirmPasswordEditText = binding.editTextConfirmPassword
        incorrectDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.text_field_incorrect)!!
        correctDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.text_field)!!

        setListeners(passwordEditText, phoneEditText, confirmPasswordEditText)
        binding.buttonSignup.setOnClickListener {
            if (validateInputs()) {
                progressBar.visibility = View.VISIBLE

                val name = binding.editTextName.text.toString()
                val password = binding.editTextPassword.text.toString()
                val phone = binding.editTextPhone.text.toString()
                val email = binding.editTextEmail.text.toString()

                val user = User(name, password, phone, email)
                binding.container.visibility = View.GONE
                viewModel.addUser(user)
            } else {
                Toast.makeText(requireContext(), "Vérifiez les informations fournies", Toast.LENGTH_SHORT).show()
            }
        }
        binding.textLoginAction.setOnClickListener {
            it.findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        }
        observeLoginResponse()
        observeAddUserError()
    }

    private fun setListeners(passwordEditText: EditText, phoneEditText: EditText, confirmEditText: EditText) {

        phoneEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validatePhoneNumber(phoneEditText)
            }
        })

        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validatePassword(passwordEditText)
                validateConfirmPassword(confirmEditText)  // Validate confirm password when password changes
            }
        })

        confirmEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validateConfirmPassword(confirmEditText)
            }
        })
    }

    private fun validateInputs(): Boolean {
        val phoneEditText = binding.editTextPhone
        val passwordEditText = binding.editTextPassword
        val nameEditText = binding.editTextName
        val confirmEditText = binding.editTextConfirmPassword
        val isPhoneValid = validatePhoneNumber(phoneEditText)
        val isPasswordValid = validatePassword(passwordEditText)
        val isNameValid = validateName(nameEditText)
        val isConfirmPasswordValid = validateConfirmPassword(confirmEditText)
        return isPhoneValid && isPasswordValid && isNameValid && isConfirmPasswordValid
    }

    private fun validatePhoneNumber(phoneEditText: EditText): Boolean {
        val phoneWarning = binding.phoneWarning
        val phoneNumber = phoneEditText.text.toString()
        return if (phoneNumber.isNotEmpty() && phoneNumber.length == 10 && phoneNumber[0] == '0' && (phoneNumber[1] == '5' || phoneNumber[1] == '6' || phoneNumber[1] == '7')) {
            phoneEditText.background = correctDrawable
            phoneWarning.visibility = View.GONE
            true
        } else {
            phoneEditText.background = incorrectDrawable
            phoneWarning.visibility = View.VISIBLE
            false
        }
    }

    private fun validatePassword(passwordEditText: EditText): Boolean {
        val passwordWarning = binding.passwordWarning
        val password = passwordEditText.text.toString()
        return if (password.isNotEmpty() && password.length >= 8) {
            passwordEditText.background = correctDrawable
            passwordWarning.visibility = View.GONE
            true
        } else {
            passwordEditText.background = incorrectDrawable
            passwordWarning.visibility = View.VISIBLE
            false
        }
    }

    private fun validateConfirmPassword(confirmEditText: EditText): Boolean {
        val confirmPasswordWarning = binding.confirmPasswordWarning
        val confirmPassword = confirmEditText.text.toString()
        val password = binding.editTextPassword.text.toString()
        return if (confirmPassword == password && confirmPassword.isNotEmpty()) {
            confirmEditText.background = correctDrawable
            confirmPasswordWarning.visibility = View.GONE
            true
        } else {
            confirmEditText.background = incorrectDrawable
            confirmPasswordWarning.visibility = View.VISIBLE
            false
        }
    }

    private fun validateName(nameEditText: EditText): Boolean {
        val name = nameEditText.text.toString()
        return name.isNotEmpty()
    }

    private fun observeLoginResponse() {
        viewModel.loginResponse.observe(viewLifecycleOwner) { loginResponse ->
            progressBar.visibility = View.GONE
            binding.container.visibility = View.VISIBLE

            Log.d("login", loginResponse.toString())
            if (loginResponse != null && loginResponse.status == "success") {
                val intent = Intent(requireActivity(), MainActivity::class.java)
                startActivity(intent)
            } else {
                val errorMessage = "An error occurred. Please try again."
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeAddUserError() {
        viewModel.addErrorStatus.observe(viewLifecycleOwner) { errorMessage ->
            progressBar.visibility = View.GONE
            binding.container.visibility = View.VISIBLE
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
}