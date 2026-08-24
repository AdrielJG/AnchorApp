package com.example.mumbaitransit.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.example.mumbaitransit.databinding.BottomSheetAuthBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAuthBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: AuthRepository
    private var isLoginMode = true

    var onAuthSuccess: ((username: String) -> Unit)? = null

    companion object {
        const val TAG = "AuthBottomSheet"
        fun newInstance() = AuthBottomSheet()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = AuthRepository(requireContext())
        renderMode()

        binding.btnToggleMode.setOnClickListener {
            isLoginMode = !isLoginMode
            renderMode()
        }

        binding.btnSubmit.setOnClickListener { submit() }

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
        }
    }

    private fun renderMode() {
        if (isLoginMode) {
            binding.tvTitle.text        = "Welcome back"
            binding.tvSubtitle.text     = "Sign in to your Anchor account"
            binding.tilUsername.visibility = View.GONE
            binding.btnSubmit.text      = "Sign In"
            binding.btnToggleMode.text  = "Don't have an account? Sign up"
        } else {
            binding.tvTitle.text        = "Create account"
            binding.tvSubtitle.text     = "Join Anchor to save your journeys"
            binding.tilUsername.visibility = View.VISIBLE
            binding.btnSubmit.text      = "Create Account"
            binding.btnToggleMode.text  = "Already have an account? Sign in"
        }
        clearErrors()
    }

    private fun clearErrors() {
        binding.tilEmail.error    = null
        binding.tilPassword.error = null
        binding.tilUsername.error = null
    }

    private fun submit() {
        val email    = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""
        val username = binding.etUsername.text?.toString()?.trim() ?: ""

        // Basic local validation before hitting DB
        if (email.isBlank()) { binding.tilEmail.error = "Required"; return }
        if (password.isBlank()) { binding.tilPassword.error = "Required"; return }
        if (!isLoginMode && username.isBlank()) { binding.tilUsername.error = "Required"; return }

        setLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            val result = if (isLoginMode)
                repo.logIn(email, password)
            else
                repo.signUp(username, email, password)

            withContext(Dispatchers.Main) {
                setLoading(false)
                when (result) {
                    is AuthResult.Success -> {
                        onAuthSuccess?.invoke(result.user.username)
                        dismiss()
                    }
                    is AuthResult.Error -> {
                        // Route error to the most relevant field
                        val msg = result.message
                        when {
                            msg.contains("email", ignoreCase = true) ->
                                binding.tilEmail.error = msg
                            msg.contains("password", ignoreCase = true) ->
                                binding.tilPassword.error = msg
                            msg.contains("username", ignoreCase = true) ->
                                binding.tilUsername.error = msg
                            else ->
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSubmit.isEnabled    = !loading
        binding.btnToggleMode.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
