package com.example.tagmoa.controller

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tagmoa.R
import com.example.tagmoa.model.AuthProvider
import com.example.tagmoa.model.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.oauth.util.NidOAuthCallback

class UserProfileFragment : Fragment(R.layout.fragment_user_profile) {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val googleClient by lazy { GoogleSignInHelper.getClient(requireContext()) }

    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnDisconnect: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textName = view.findViewById(R.id.textProfileName)
        textEmail = view.findViewById(R.id.textProfileEmail)
        btnLogout = view.findViewById(R.id.btnProfileLogout)
        btnDisconnect = view.findViewById(R.id.btnProfileDisconnect)

        val session = SessionManager.currentSession
        if (session == null) {
            redirectToLogin()
            return
        }

        textName.text = getString(R.string.profile_name_format, session.displayName ?: "-")
        textEmail.text = getString(R.string.profile_email_format, session.email ?: "-")

        btnLogout.setOnClickListener {
            when (session.provider) {
                AuthProvider.GOOGLE -> logoutGoogle()
                AuthProvider.KAKAO -> logoutKakao()
                AuthProvider.NAVER -> logoutNaver()
            }
        }

        btnDisconnect.setOnClickListener {
            when (session.provider) {
                AuthProvider.GOOGLE -> disconnectGoogle()
                AuthProvider.KAKAO -> disconnectKakao()
                AuthProvider.NAVER -> disconnectNaver()
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

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }
}
