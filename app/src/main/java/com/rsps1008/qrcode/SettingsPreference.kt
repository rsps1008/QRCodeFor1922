package com.rsps1008.qrcode

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.rsps1008.qrcode.backup.GoogleDriveService
import com.rsps1008.qrcode.backup.ScanResultBackup
import com.rsps1008.qrcode.ui.MainActivity.Companion.PREFKEY
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class SettingsPreference : PreferenceFragmentCompat() {
    private lateinit var googleDriveAccountPreference: Preference
    private lateinit var googleDriveBackupPreference: Preference
    private lateinit var googleDriveRestorePreference: Preference
    private lateinit var googleDriveSignOutPreference: Preference

    private var googleSignInAccount: GoogleSignInAccount? = null
    private var googleDriveSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient? = null
    private var lastBackupAtMillis: Long? = null
    private var cloudOperation: Job? = null
    private var isCloudOperationRunning = false

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleGoogleSignInResult(result.data)
        } else {
            showMessage(getString(R.string.google_drive_sign_in_failed, getString(R.string.google_drive_sign_in_cancelled)))
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = PREFKEY
        setPreferencesFromResource(R.xml.preference_main, rootKey)

        googleDriveAccountPreference = findPreference("google_drive_account")
            ?: error("Missing Google Drive account preference")
        googleDriveBackupPreference = findPreference("google_drive_backup")
            ?: error("Missing Google Drive backup preference")
        googleDriveRestorePreference = findPreference("google_drive_restore")
            ?: error("Missing Google Drive restore preference")
        googleDriveSignOutPreference = findPreference("google_drive_sign_out")
            ?: error("Missing Google Drive sign-out preference")

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        googleDriveSignInClient = GoogleSignIn.getClient(requireContext(), signInOptions)

        googleDriveAccountPreference.setOnPreferenceClickListener {
            if (googleSignInAccount == null) {
                googleDriveSignInClient?.signInIntent?.let(googleSignInLauncher::launch)
            } else {
                showMessage(googleSignInAccount?.email.orEmpty())
            }
            true
        }
        googleDriveBackupPreference.setOnPreferenceClickListener {
            backupToGoogleDrive()
            true
        }
        googleDriveRestorePreference.setOnPreferenceClickListener {
            confirmRestoreFromGoogleDrive()
            true
        }
        googleDriveSignOutPreference.setOnPreferenceClickListener {
            signOutFromGoogleDrive()
            true
        }

        findPreference<SwitchPreferenceCompat>("dark_mode")?.setOnPreferenceChangeListener { _, value ->
            AppCompatDelegate.setDefaultNightMode(
                if (value == true) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            true
        }
        updateGoogleDrivePreferences()
    }

    override fun onResume() {
        super.onResume()
        refreshGoogleDriveAccount()
    }

    override fun onDestroyView() {
        cloudOperation?.cancel()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundResource(R.color.preference_bg_color)
        view.findViewById<RecyclerView>(android.R.id.list)?.apply {
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, padding)
            clipToPadding = false
            setBackgroundResource(R.color.preference_bg_color)
        }
    }

    private fun refreshGoogleDriveAccount() {
        val context = context ?: return
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val driveScope = Scope(DriveScopes.DRIVE_APPDATA)
        if (account == null || !GoogleSignIn.hasPermissions(account, driveScope)) {
            googleSignInAccount = null
            lastBackupAtMillis = null
            updateGoogleDrivePreferences()
            return
        }

        googleSignInAccount = account
        updateGoogleDrivePreferences()
        viewLifecycleOwner.lifecycleScope.launch {
            lastBackupAtMillis = runCatching {
                GoogleDriveService(context, account)
                    .getBackupModifiedTime(ScanResultBackup.FILE_NAME)
                    .getOrThrow()
            }.getOrNull()
            updateGoogleDrivePreferences()
        }
    }

    private fun handleGoogleSignInResult(intent: Intent?) {
        if (intent == null) {
            showMessage(getString(R.string.google_drive_sign_in_failed, getString(R.string.google_drive_sign_in_missing_result)))
            return
        }

        try {
            val account = GoogleSignIn
                .getSignedInAccountFromIntent(intent)
                .getResult(ApiException::class.java)
            val hasDrivePermission = GoogleSignIn.hasPermissions(
                account,
                Scope(DriveScopes.DRIVE_APPDATA)
            )
            if (!hasDrivePermission) {
                googleSignInAccount = null
                updateGoogleDrivePreferences()
                showMessage(getString(R.string.google_drive_sign_in_failed, getString(R.string.google_drive_permission_missing)))
            } else {
                googleSignInAccount = account
                updateGoogleDrivePreferences()
                showMessage(getString(R.string.google_drive_sign_in_success))
                refreshGoogleDriveAccount()
            }
        } catch (e: ApiException) {
            showMessage(getString(R.string.google_drive_sign_in_failed, e.statusCode.toString()))
        }
    }

    private fun backupToGoogleDrive() {
        val account = googleSignInAccount ?: run {
            showMessage(getString(R.string.google_drive_not_signed_in))
            return
        }
        val context = requireContext()
        startCloudOperation(R.string.google_drive_backup_failed) {
            val results = withContext(Dispatchers.IO) {
                Utils.getDatabaseDao(context).getAll()
            }
            val content = withContext(Dispatchers.Default) {
                ScanResultBackup.encode(results)
            }
            GoogleDriveService(context, account)
                .uploadBackup(ScanResultBackup.FILE_NAME, content)
                .getOrThrow()
            lastBackupAtMillis = System.currentTimeMillis()
            updateGoogleDrivePreferences()
            showMessage(getString(R.string.google_drive_backup_success))
        }
    }

    private fun confirmRestoreFromGoogleDrive() {
        val account = googleSignInAccount ?: run {
            showMessage(getString(R.string.google_drive_not_signed_in))
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.google_drive_restore_confirm_title)
            .setMessage(R.string.google_drive_restore_confirm_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.google_drive_restore_confirm) { _, _ ->
                restoreFromGoogleDrive(account)
            }
            .show()
    }

    private fun restoreFromGoogleDrive(account: GoogleSignInAccount) {
        val context = requireContext()
        startCloudOperation(R.string.google_drive_restore_failed) {
            val backupFile = GoogleDriveService(context, account)
                .restoreBackupWithModifiedTime(ScanResultBackup.FILE_NAME)
                .getOrThrow()
                ?: run {
                    showMessage(getString(R.string.google_drive_no_backup))
                    return@startCloudOperation
                }
            val results = withContext(Dispatchers.Default) {
                ScanResultBackup.decode(backupFile.content)
            }
            withContext(Dispatchers.IO) {
                Utils.getDatabaseDao(context).replaceAll(results)
            }
            lastBackupAtMillis = backupFile.modifiedAtMillis
            updateGoogleDrivePreferences()
            showMessage(getString(R.string.google_drive_restore_success, results.size))
        }
    }

    private fun signOutFromGoogleDrive() {
        googleDriveSignInClient?.signOut()?.addOnCompleteListener {
            googleSignInAccount = null
            lastBackupAtMillis = null
            updateGoogleDrivePreferences()
            showMessage(getString(R.string.google_drive_signed_out))
        }
    }

    private fun startCloudOperation(failureMessageRes: Int, block: suspend () -> Unit) {
        cloudOperation?.cancel()
        isCloudOperationRunning = true
        updateGoogleDrivePreferences()
        cloudOperation = viewLifecycleOwner.lifecycleScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Drive operation failed", e)
                val message = e.message ?: e.javaClass.simpleName
                showMessage(getString(failureMessageRes, message))
            } finally {
                isCloudOperationRunning = false
                updateGoogleDrivePreferences()
            }
        }
    }

    private fun updateGoogleDrivePreferences() {
        if (!::googleDriveAccountPreference.isInitialized) return
        val signedIn = googleSignInAccount != null
        googleDriveAccountPreference.summary = if (signedIn) {
            val account = googleSignInAccount?.email.orEmpty()
            val lastBackup = lastBackupAtMillis?.let {
                DateFormat.getDateTimeInstance().format(Date(it))
            } ?: getString(R.string.google_drive_time_unknown)
            getString(R.string.google_drive_last_backup, "$account；$lastBackup")
        } else {
            getString(R.string.google_drive_not_signed_in)
        }
        googleDriveBackupPreference.isEnabled = signedIn && !isCloudOperationRunning
        googleDriveRestorePreference.isEnabled = signedIn && !isCloudOperationRunning
        googleDriveSignOutPreference.isEnabled = signedIn && !isCloudOperationRunning
    }

    private fun showMessage(message: String) {
        if (isAdded) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val TAG = "SettingsPreference"
    }
}
