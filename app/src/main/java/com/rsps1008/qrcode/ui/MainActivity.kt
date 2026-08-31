package com.rsps1008.qrcode.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.res.Configuration
import android.util.Log
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.barcode.common.Barcode
import com.rsps1008.qrcode.QRCodeAnalyzer
import com.rsps1008.qrcode.R
import com.rsps1008.qrcode.SettingsPreference
import com.rsps1008.qrcode.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias QRCodeListener = (barcodes: List<Barcode>) -> Unit

class MainActivity : AppCompatActivity() {
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    private val viewModel: MainViewModel by viewModels()
    private val scaleGestureDetector by lazy {
        ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                cameraControl?.let { control ->
                    camera?.cameraInfo?.zoomState?.value?.let { zoomState ->
                        val currentZoom = zoomState.zoomRatio
                        val newZoom = currentZoom * detector.scaleFactor
                        val clampedZoom = newZoom.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                        control.setZoomRatio(clampedZoom)
                    }
                }
                return true
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = getSharedPreferences(PREFKEY, MODE_PRIVATE)
        if (!preferences.contains(PREF_DARK_MODE)) {
            val followsSystemTheme = (resources.configuration.uiMode
                and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            preferences.edit().putBoolean(PREF_DARK_MODE, followsSystemTheme).apply()
        }
        val darkMode = preferences.getBoolean(PREF_DARK_MODE, false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentPref) { fragmentContainer, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            fragmentContainer.setPadding(
                fragmentContainer.paddingLeft,
                systemBars.top,
                fragmentContainer.paddingRight,
                systemBars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.fragmentPref)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.findFragmentById(R.id.fragment_pref) != null) {
                    returnToScanner()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        cameraExecutor = Executors.newSingleThreadExecutor()
        viewModel.showAgreement.observe(this) {
            it?.let {
                if (it) {
                    showAgreementDialog()
                }
            }
        }
        viewModel.vibrate.observe(this) {
            if (it == true) {
                vibrate()
            }
        }
        viewModel.startCamera.observe(this) {
            if (it == true) {
                startCamera()
            }
        }
        viewModel.copyAlready.observe(this) {
            if (it.isNotEmpty()) {
                Toast.makeText(
                    this,
                    String.format(getString(R.string.copy_already), it),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        viewModel.startActivity.observe(this) {
            it?.let {
                try {
                    startActivity(it)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        getString(R.string.nothing_happen),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        viewModel.finishActivity.observe(this) {
            if (it == true) {
                finish()
            }
        }
        viewModel.showDetectOtherDialog.observe(this) {
            it?.let {
                val dialog = MaterialAlertDialogBuilder(this@MainActivity)
                dialog.setTitle(getString(R.string.detect_content))
                dialog.setMessage(
                    String.format(
                        getString(R.string.confirm_open_content),
                        it.rawValue
                    )
                )
                dialog.setPositiveButton(
                    getString(android.R.string.ok)
                ) { dialog, which ->
                    viewModel.resetRedirectDialog()
                    viewModel.activeIntent()
                }
                dialog.setNeutralButton(
                    getString(R.string.copy_to_clipboard)
                ) { dialog, which ->
                    viewModel.copyToClipboard(it.rawValue ?: "")
                }
                dialog.setNegativeButton(
                    android.R.string.cancel
                ) { _, _ -> viewModel.resetRedirectDialog() }
                dialog.setOnCancelListener {
                    viewModel.resetRedirectDialog()
                }
                dialog.show()
            }
        }
        viewModel.showHistoryPrompt.observe(this) {
            if (it == true) {
                val builder = AlertDialog.Builder(this).apply {
                    setTitle(getString(R.string.new_feature))
                    setMessage(getString(R.string.history_feature_mes))
                    setPositiveButton(getText(R.string.dialog_confirm)
                    ) { _, _ -> viewModel.confirmNewFeature() }
                    setOnCancelListener { viewModel.confirmNewFeature() }
                }
                builder.show()
            }
        }

        binding.viewFinder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
        viewModel.ready()
    }

    private fun showAgreementDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
            .setTitle(getString(R.string.claim_title))
            .setMessage(getString(R.string.claim_mes))
            .setPositiveButton(
                getString(R.string.agree)
            ) { _, _ ->
                viewModel.userAgree()
                viewModel.resetAgreement()
            }
            .setNegativeButton(
                getString(R.string.not_agree)
            ) { _, _ ->
                viewModel.resetAgreement()
                finish()
            }
            .show()
    }

    private fun startCamera() {
        if (allPermissionsGranted()) {
            startCameraLock()
        } else {
            ActivityCompat.requestPermissions(
                this@MainActivity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        viewModel.resetStartCamera()
    }

    private fun startCameraLock() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1024, 768),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer(callback))
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                // 修改這裡，移除局部變數，直接賦值給全域的 camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
                cameraControl = camera?.cameraControl
                cameraInfo = camera?.cameraInfo

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }



    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCameraLock()
            } else {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.hint_permission_title))
                    .setMessage(getString(R.string.hint_permission_mes))
                    .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                        finish()
                    }.show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                showSettings()
            }
            R.id.history -> {
                showHistory(showFavoritesOnly = false)
            }
            R.id.show_favorites -> {
                showHistory(showFavoritesOnly = true)
            }
        }
        return true
    }

    private fun returnToScanner() {
        supportFragmentManager.popBackStackImmediate(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_pref)
        if (currentFragment != null) {
            supportFragmentManager.beginTransaction().remove(currentFragment).commit()
        }
        viewModel.isSettingsShowing(false)
        invalidateOptionsMenu()
    }

    private fun showHistory(showFavoritesOnly: Boolean) {
        val currentHistory = supportFragmentManager.findFragmentById(R.id.fragment_pref)
            as? ScanResultFragment
        if (currentHistory?.isShowingFavorites == showFavoritesOnly) {
            return
        }

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragment_pref,
                ScanResultFragment.newInstance(showFavoritesOnly),
                FRAGMENT_TAG_HISTORY
            )
            .addToBackStack(FRAGMENT_TAG_HISTORY)
            .commit()
        viewModel.isSettingsShowing(true)
        invalidateOptionsMenu()
    }

    private fun showSettings() {
        if (supportFragmentManager.findFragmentById(R.id.fragment_pref) is SettingsPreference) {
            return
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_pref, SettingsPreference(), FRAGMENT_TAG_SETTINGS)
            .addToBackStack(FRAGMENT_TAG_SETTINGS)
            .commit()
        viewModel.isSettingsShowing(true)
        invalidateOptionsMenu()
    }

    private val callback = object : QRCodeListener {
        override fun invoke(barcodes: List<Barcode>) {
            val isSettingsShow = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_SETTINGS)
                .takeIf { it != null }?.isVisible ?: false
            val isHistoryShow = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_HISTORY)
                .takeIf { it != null }?.isVisible ?: false
            viewModel.isSettingsShowing(isSettingsShow || isHistoryShow)
            viewModel.newBarcodes(barcodes)
        }
    }

    private fun vibrate() {
        val v = getSystemService(Vibrator::class.java)
        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        viewModel.resetVibrate()
    }

    companion object {
        const val PREFKEY = "1922qrcode"
        private const val PREF_DARK_MODE = "dark_mode"
        private const val TAG = "QRCodeScanner"
        private const val FRAGMENT_TAG_SETTINGS = "settings"
        private const val FRAGMENT_TAG_HISTORY = "history"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
