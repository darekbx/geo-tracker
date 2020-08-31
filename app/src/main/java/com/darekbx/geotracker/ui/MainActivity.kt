package com.darekbx.geotracker.ui

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.darekbx.geotracker.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private var menu: Menu? = null

    private val listener = NavController.OnDestinationChangedListener { controller, destination, arguments ->
        if (destination.id == R.id.tracksFragment) {
            showSettingsButton()
        }
    }

    override fun onResume() {
        super.onResume()
        findNavController(R.id.fragment_container).addOnDestinationChangedListener(listener)
    }

    override fun onPause() {
        findNavController(R.id.fragment_container).removeOnDestinationChangedListener(listener)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                findNavController(R.id.fragment_container)
                    .navigate(R.id.action_tracksFragment_to_settingsFragment)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSettingsButton() {
        menu?.getItem(0)?.setVisible(true)
    }
}