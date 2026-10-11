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

/**
 * The only account form in the app.
 *
 * Sign-up collects four fields: name, username, phone, password. Sign-in needs
 * two: phone and password. Nothing else is asked for at any point — home, work,
 * addresses and routes are added later from the profile screen.
 */
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
        val signUpOnly = if (isLoginMode) View.GONE else View.VISIBLE
        binding.tilName.visibility = signUpOnly
        binding.tilUsername.visibility = signUpOnly

        if (isLoginMode) {
            binding.tvTitle.text       = "Welcome back"
            binding.tvSubtitle.text    = "Sign in with your phone number"
            binding.btnSubmit.text     = "Sign In"
            binding.btnToggleMode.text = "Don't have an account? Sign up"
        } else {
            binding.tvTitle.text       = "Create account"
            binding.tvSubtitle.text    = "Four details and you're in"
            binding.btnSubmit.text     = "Create Account"
            binding.btnToggleMode.text = "Already have an account? Sign in"
        }
        clearErrors()
    }

    private fun clearErrors() {
        binding.tilName.error     = null
        binding.tilUsername.error = null
        binding.tilPhone.error    = null
        binding.tilPassword.error = null
    }

    private fun submit() {
        clearErrors()

        val name     = binding.etName.text?.toString()?.trim() ?: ""
        val username = binding.etUsername.text?.toString()?.trim() ?: ""
        val phone    = binding.etPhone.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""

        if (phone.isBlank())    { binding.tilPhone.error = "Required"; return }
        if (password.isBlank()) { binding.tilPassword.error = "Required"; return }
        if (!isLoginMode) {
            if (name.isBlank())     { binding.tilName.error = "Required"; return }
            if (username.isBlank()) { binding.tilUsername.error = "Required"; return }
        }

        setLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            val result = if (isLoginMode)
                repo.logIn(phone, password)
            else
                repo.signUp(name, username, phone, password)

            withContext(Dispatchers.Main) {
                if (_binding == null) return@withContext
                setLoading(false)
                when (result) {
                    is AuthResult.Success -> {
                        onAuthSuccess?.invoke(result.username)
                        dismiss()
                    }
                    is AuthResult.Error -> {
                        // Route the message to the field it is about.
                        val msg = result.message
                        when {
                            msg.contains("username", true) ->
                                binding.tilUsername.error = msg
                            msg.contains("phone", true) ->
                                binding.tilPhone.error = msg
                            msg.contains("password", true) ->
                                binding.tilPassword.error = msg
                            msg.contains("your name", true) ->
                                binding.tilName.error = msg
                            else ->
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSubmit.isEnabled     = !loading
        binding.btnToggleMode.isEnabled = !loading
        binding.progressBar.visibility  = if (loading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
