package com.darekbx.geotracker.ui.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.darekbx.geotracker.R
import kotlinx.coroutines.*

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            withContext(Dispatchers.Main) {
                findNavController()
                    .navigate(R.id.action_splashFragment_to_tracksFragment)
            }
        }
    }
}