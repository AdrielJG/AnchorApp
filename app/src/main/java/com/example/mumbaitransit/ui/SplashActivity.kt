package com.example.mumbaitransit.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mumbaitransit.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val vm: TransitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vm.loadState.observe(this) { state ->
            when (state) {
                is LoadState.Loading -> {
                    binding.tvStatus.text = "Loading transit data…"
                    binding.progressBar.isIndeterminate = true
                }
                is LoadState.Ready -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is LoadState.Error -> {
                    binding.tvStatus.text = "Error: ${state.msg}"
                    binding.progressBar.isIndeterminate = false
                }
            }
        }
    }
}
