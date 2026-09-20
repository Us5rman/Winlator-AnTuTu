package com.winlator.star.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.winlator.star.MainActivity
import com.winlator.star.core.BcnLayerInstaller
import com.winlator.star.xenvironment.ImageFs
import com.winlator.star.xenvironment.ImageFsInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashViewModel(app: Application) : AndroidViewModel(app) {

    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    /** True once installation reaches 100% — show the Proceed button. */
    private val _showProceed = MutableStateFlow(false)
    val showProceed: StateFlow<Boolean> = _showProceed

    /**
     * Check whether the system image needs (re)installation.
     * Returns true if an install was triggered; the caller should show the install overlay.
     */
    fun installIfNeeded(activity: MainActivity): Boolean {
        if (_isInstalling.value) return true   // already running

        val imageFs = ImageFs.find(activity)
        if (imageFs.isValid && imageFs.version >= ImageFsInstaller.LATEST_VERSION) return false

        _isInstalling.value = true
        _progress.value = 0

        ImageFsInstaller.installFromAssetsWithCallback(
            activity,
            { pct ->
                // Cap the imagefs extraction's own reported progress at 95%, reserving the
                // last stretch for the BCn layer install step below — so the bar doesn't
                // read "100%" before everything is actually done.
                _progress.value = (pct * 0.95f).toInt()
            },
            {
                // imagefs is now extracted and valid, so Z:/usr/lib exists — safe to drop
                // libbcn_layer.so in right after it, as one continuous install sequence
                // rather than a separate silent step the user never sees finish.
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        BcnLayerInstaller.installToDriveZ(activity)
                    }
                    _progress.value = 100
                    _showProceed.value = true
                }
            },
        )
        return true
    }

    /** Called when user taps Proceed; hides the splash overlay. */
    fun dismissSplash() {
        _showProceed.value = false
        _isInstalling.value = false
    }
}
