package com.darekbx.geotracker.utils

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity

class PermissionRequester(
    val activity: FragmentActivity?,
    val permission: String,
    val onDenied: () -> Unit = { },
    val onRationale: () -> Unit = { }
) {

    private var onGranted: () -> Unit = { }

    private val requestPermissionLauncher =
        activity?.registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            when {
                isGranted -> onGranted()
                activity?.shouldShowRequestPermissionRationale(permission) -> onRationale()
                else -> onDenied()
            }
        }

    fun runWithPermission(onGranted: () -> Unit) {
        this.onGranted = onGranted
        requestPermissionLauncher?.launch(permission)
    }
}