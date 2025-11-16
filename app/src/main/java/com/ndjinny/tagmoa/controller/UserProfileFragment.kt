package com.ndjinny.tagmoa.controller

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.AuthProvider
import com.ndjinny.tagmoa.model.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.oauth.util.NidOAuthCallback
import android.content.pm.PackageManager
import android.os.Build

class UserProfileFragment : Fragment(R.layout.fragment_user_profile) {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val googleClient by lazy { GoogleSignInHelper.getClient(requireContext()) }
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            openAlarmManagement()
        } else {
            Toast.makeText(requireContext(), getString(R.string.alarm_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var rowPickBackground: View
    private lateinit var rowOnboardingGuide: View
    private lateinit var rowContactSupport: View
    private lateinit var rowAlarmManagement: View
    private lateinit var rowLogout: View
    private lateinit var rowDisconnect: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textName = view.findViewById(R.id.textProfileName)
        textEmail = view.findViewById(R.id.textProfileEmail)
        rowPickBackground = view.findViewById(R.id.rowPickHomeBackground)
        rowOnboardingGuide = view.findViewById(R.id.rowViewOnboarding)
        rowContactSupport = view.findViewById(R.id.rowContactSupport)
        rowAlarmManagement = view.findViewById(R.id.rowAlarmManagement)
        rowLogout = view.findViewById(R.id.rowLogout)
        rowDisconnect = view.findViewById(R.id.rowDisconnect)

        val session = SessionManager.currentSession
        if (session == null) {
            redirectToLogin()
            return
        }

        textName.text = getString(R.string.profile_name_format, session.displayName ?: "-")
        textEmail.text = getString(R.string.profile_email_format, session.email ?: "-")

        rowPickBackground.setOnClickListener { openHomeBackgroundSettings() }

        rowAlarmManagement.setOnClickListener {
            ensureNotificationPermissionAndOpen()
        }

        rowOnboardingGuide.setOnClickListener {
            val intent = Intent(requireContext(), OnboardingActivity::class.java).apply {
                putExtra(OnboardingActivity.EXTRA_FORCE_SHOW, true)
            }
            startActivity(intent)
        }

        rowContactSupport.setOnClickListener {
            openSupportEmail()
        }

        rowLogout.setOnClickListener {
            when (session.provider) {
                AuthProvider.GOOGLE -> logoutGoogle()
                AuthProvider.KAKAO -> logoutKakao()
                AuthProvider.NAVER -> logoutNaver()
            }
        }

        rowDisconnect.setOnClickListener {
            when (session.provider) {
                AuthProvider.GOOGLE -> disconnectGoogle()
                AuthProvider.KAKAO -> disconnectKakao()
                AuthProvider.NAVER -> disconnectNaver()
            }
        }
    }

    private fun ensureNotificationPermissionAndOpen() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            openAlarmManagement()
            return
        }
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                openAlarmManagement()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                Toast.makeText(requireContext(), getString(R.string.alarm_permission_rationale), Toast.LENGTH_SHORT).show()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun logoutGoogle() {
        firebaseAuth.signOut()
        googleClient.signOut().addOnCompleteListener {
            SessionManager.clearSession()
            Toast.makeText(requireContext(), getString(R.string.message_signed_out), Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }
    }

    private fun logoutKakao() {
        UserApiClient.instance.logout { error ->
            SessionManager.clearSession()
            if (error != null) {
                Toast.makeText(requireContext(), error.localizedMessage ?: getString(R.string.error_generic), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.message_signed_out), Toast.LENGTH_SHORT).show()
            }
            redirectToLogin()
        }
    }

    private fun logoutNaver() {
        NidOAuth.logout(object : NidOAuthCallback {
            override fun onSuccess() {
                SessionManager.clearSession()
                Toast.makeText(requireContext(), getString(R.string.message_signed_out), Toast.LENGTH_SHORT).show()
                redirectToLogin()
            }

            override fun onFailure(errorCode: String, errorDesc: String) {
                Toast.makeText(
                    requireContext(),
                    "errorCode:$errorCode, errorDesc:$errorDesc",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun disconnectGoogle() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            redirectToLogin()
            return
        }
        currentUser.delete().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                firebaseAuth.signOut()
                googleClient.revokeAccess().addOnCompleteListener {
                    SessionManager.clearSession()
                    Toast.makeText(requireContext(), getString(R.string.message_account_deleted), Toast.LENGTH_SHORT).show()
                    redirectToLogin()
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.message_account_delete_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun disconnectKakao() {
        UserApiClient.instance.unlink { error ->
            if (error != null) {
                Toast.makeText(requireContext(), getString(R.string.message_account_delete_failed), Toast.LENGTH_LONG).show()
            } else {
                SessionManager.clearSession()
                Toast.makeText(requireContext(), getString(R.string.message_account_deleted), Toast.LENGTH_SHORT).show()
                redirectToLogin()
            }
        }
    }

    private fun disconnectNaver() {
        NidOAuth.disconnect(object : NidOAuthCallback {
            override fun onSuccess() {
                SessionManager.clearSession()
                Toast.makeText(requireContext(), getString(R.string.message_account_deleted), Toast.LENGTH_SHORT).show()
                redirectToLogin()
            }

            override fun onFailure(errorCode: String, errorDesc: String) {
                Toast.makeText(
                    requireContext(),
                    "errorCode:$errorCode, errorDesc:$errorDesc",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun openSupportEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("nadajinny@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.profile_contact_support_subject))
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), getString(R.string.profile_contact_support_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    private fun openHomeBackgroundSettings() {
        val intent = Intent(requireContext(), HomeBackgroundSettingsActivity::class.java)
        startActivity(intent)
    }

    private fun openAlarmManagement() {
        val intent = Intent(requireContext(), AlarmManagementActivity::class.java)
        startActivity(intent)
    }
}
