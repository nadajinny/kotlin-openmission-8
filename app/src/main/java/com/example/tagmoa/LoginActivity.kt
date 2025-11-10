package com.example.tagmoa

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInButton: SignInButton
    private lateinit var progressBar: ProgressBar
    private lateinit var googleSignInClient: GoogleSignInClient

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken)
            } catch (error: ApiException) {
                showSignInError(getString(R.string.message_google_sign_in_failed))
            }
        } else {
            toggleLoading(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        googleSignInButton = findViewById(R.id.btn_google_login)
        progressBar = findViewById(R.id.progressLogin)

        googleSignInButton.setSize(SignInButton.SIZE_WIDE)

        googleSignInClient = GoogleSignIn.getClient(this, buildSignInOptions())

        googleSignInButton.setOnClickListener {
            launchGoogleSignIn()
        }
    }

    override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser != null) {
            firebaseAuth.currentUser?.let { UserDatabase.upsertUserProfile(it) }
            navigateToMain()
        }
    }

    private fun launchGoogleSignIn() {
        toggleLoading(true)
        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        if (idToken.isNullOrBlank()) {
            showSignInError(getString(R.string.message_google_sign_in_failed))
            return
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    firebaseAuth.currentUser?.let { UserDatabase.upsertUserProfile(it) }
                    navigateToMain()
                } else {
                    showSignInError(getString(R.string.message_google_auth_failed))
                }
            }
    }

    private fun showSignInError(message: String) {
        toggleLoading(false)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun toggleLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        googleSignInButton.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        toggleLoading(false)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun buildSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
}
