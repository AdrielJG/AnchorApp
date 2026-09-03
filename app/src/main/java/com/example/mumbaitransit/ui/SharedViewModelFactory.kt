package com.example.mumbaitransit.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SharedViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransitViewModel::class.java)) {
            if (sharedViewModel == null) {
                sharedViewModel = TransitViewModel(application)
            }
            return sharedViewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    companion object {
        @Volatile
        private var sharedViewModel: TransitViewModel? = null

        fun getInstance(application: Application): TransitViewModel {
            return sharedViewModel ?: synchronized(this) {
                sharedViewModel ?: TransitViewModel(application).also { sharedViewModel = it }
            }
        }
    }
}

fun Application.getSharedViewModel(): TransitViewModel {
    return SharedViewModelFactory.getInstance(this)
}
