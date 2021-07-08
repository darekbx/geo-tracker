package com.darekbx.geotracker.utils

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity

class PermissionRequester(
    val activity: FragmentActivity?,
    private val permissions: Array<String>,
    val onDenied: () -> Unit = { }
) {

    private var onGranted: () -> Unit = { }

    private val requestPermissionLauncher =
        activity?.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            when {
                permissions.all { it.value } -> onGranted()
                else -> onDenied()
            }
        }

    fun runWithPermission(onGranted: () -> Unit) {
        this.onGranted = onGranted
        requestPermissionLauncher?.launch(permissions)
    }
}
