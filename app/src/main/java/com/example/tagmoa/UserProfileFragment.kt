package com.example.tagmoa

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

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

        val user = firebaseAuth.currentUser
        if (user == null) {
            redirectToLogin()
            return
        }

        textName.text = getString(R.string.profile_name_format, user.displayName ?: "-")
        textEmail.text = getString(R.string.profile_email_format, user.email ?: "-")

        btnLogout.setOnClickListener {
            firebaseAuth.signOut()
            googleClient.signOut().addOnCompleteListener {
                Toast.makeText(requireContext(), getString(R.string.message_signed_out), Toast.LENGTH_SHORT).show()
                redirectToLogin()
            }
        }

        btnDisconnect.setOnClickListener {
            performAccountDisconnect()
        }
    }

    private fun performAccountDisconnect() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            redirectToLogin()
            return
        }
        currentUser.delete().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                firebaseAuth.signOut()
                googleClient.revokeAccess().addOnCompleteListener {
                    Toast.makeText(requireContext(), getString(R.string.message_account_deleted), Toast.LENGTH_SHORT).show()
                    redirectToLogin()
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.message_account_delete_failed), Toast.LENGTH_LONG).show()
            }
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
}
