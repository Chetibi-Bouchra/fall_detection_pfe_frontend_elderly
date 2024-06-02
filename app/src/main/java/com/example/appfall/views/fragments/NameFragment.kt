package com.example.appfall.views.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.appfall.R
import com.example.appfall.databinding.FragmentNameBinding
import com.example.appfall.viewModels.UserViewModel

class NameFragment : Fragment() {
    private var _binding: FragmentNameBinding? = null
    private val binding get() = _binding!!
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe LiveData for update responses and errors
        observeUpdateResponse()
        observeUpdateError()

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_nameFragment_to_parametersMainFragment)
        }

        binding.btnConfirm.setOnClickListener {
            val newName = binding.editTextName.text.toString()
            if (newName.isNotEmpty()) {
                userViewModel.updateName(newName)
            } else {
                binding.nameWarning.visibility = View.VISIBLE
            }
        }

        binding.editTextName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().isNotEmpty()) {
                    binding.nameWarning.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeUpdateResponse() {
        userViewModel.updateNameResponse.observe(viewLifecycleOwner, Observer { updateResponse ->
            if (updateResponse != null) {
                Toast.makeText(requireContext(), "Mise à jour réussite", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_nameFragment_to_parametersMainFragment)
            }
        })

        userViewModel.updatePasswordResponse.observe(viewLifecycleOwner, Observer { updateResponse ->
            if (updateResponse != null) {
                Toast.makeText(requireContext(), "Mise à jour réussite", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_nameFragment_to_parametersMainFragment)
            }
        })
    }

    private fun observeUpdateError() {
        userViewModel.addErrorStatus.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Une erreur est survenue lors de la mise à jour", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
