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
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.profile.domain.vo.NidProfile
import com.navercorp.nid.profile.util.NidProfileCallback

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInButton: SignInButton
    private lateinit var kakaoLoginButton: MaterialButton
    private lateinit var naverLoginButton: MaterialButton
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

    private val naverLoginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchNaverProfile()
        } else {
            val errorDesc = NidOAuth.getLastErrorDescription()
            showSignInError(errorDesc ?: getString(R.string.error_generic))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        googleSignInButton = findViewById(R.id.btn_google_login)
        kakaoLoginButton = findViewById(R.id.btn_kakao_login)
        naverLoginButton = findViewById(R.id.btn_naver_login)
        progressBar = findViewById(R.id.progressLogin)

        googleSignInButton.setSize(SignInButton.SIZE_WIDE)

        googleSignInClient = GoogleSignInHelper.getClient(this)

        googleSignInButton.setOnClickListener {
            launchGoogleSignIn()
        }

        kakaoLoginButton.setOnClickListener {
            startKakaoLogin()
        }

        naverLoginButton.setOnClickListener {
            startNaverLogin()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val session = UserSession(
                provider = AuthProvider.GOOGLE,
                uid = currentUser.uid,
                displayName = currentUser.displayName,
                email = currentUser.email
            )
            SessionManager.setSession(session)
            UserDatabase.upsertUserProfile(session)
            navigateToMain()
            return
        }

        val savedSession = SessionManager.currentSession
        if (savedSession?.provider == AuthProvider.KAKAO) {
            toggleLoading(true)
            UserApiClient.instance.accessTokenInfo { _, error ->
                if (error != null) {
                    SessionManager.clearSession()
                    toggleLoading(false)
                } else {
                    fetchKakaoUser()
                }
            }
        } else if (savedSession?.provider == AuthProvider.NAVER) {
            toggleLoading(true)
            if (NidOAuth.getAccessToken().isNullOrBlank()) {
                SessionManager.clearSession()
                toggleLoading(false)
            } else {
                fetchNaverProfile()
            }
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
                    firebaseAuth.currentUser?.let { user ->
                        val session = UserSession(
                            provider = AuthProvider.GOOGLE,
                            uid = user.uid,
                            displayName = user.displayName,
                            email = user.email
                        )
                        SessionManager.setSession(session)
                        UserDatabase.upsertUserProfile(session)
                        navigateToMain()
                    } ?: run {
                        showSignInError(getString(R.string.message_google_auth_failed))
                    }
                } else {
                    showSignInError(getString(R.string.message_google_auth_failed))
                }
            }
    }

    private fun startKakaoLogin() {
        toggleLoading(true)
        val callback: (OAuthToken?, Throwable?) -> Unit = { _, error ->
            if (error != null) {
                showSignInError(error.localizedMessage ?: getString(R.string.error_generic))
            } else {
                fetchKakaoUser()
            }
        }
        val kakaoClient = UserApiClient.instance
        if (kakaoClient.isKakaoTalkLoginAvailable(this)) {
            kakaoClient.loginWithKakaoTalk(this) { token, error ->
                if (error != null && error is com.kakao.sdk.common.model.ClientError &&
                    error.reason == com.kakao.sdk.common.model.ClientErrorCause.Cancelled
                ) {
                    toggleLoading(false)
                } else if (error != null) {
                    kakaoClient.loginWithKakaoAccount(this, callback = callback)
                } else if (token != null) {
                    fetchKakaoUser()
                }
            }
        } else {
            kakaoClient.loginWithKakaoAccount(this, callback = callback)
        }
    }

    private fun fetchKakaoUser() {
        UserApiClient.instance.me { user, error ->
            if (error != null || user == null) {
                showSignInError(error?.localizedMessage ?: getString(R.string.error_generic))
                return@me
            }
            val email = user.kakaoAccount?.email
            if (email.isNullOrBlank()) {
                showSignInError(getString(R.string.error_kakao_email_required))
                return@me
            }
            val displayName = user.kakaoAccount?.profile?.nickname
                ?: email.substringBefore("@", email)
            val session = UserSession(
                provider = AuthProvider.KAKAO,
                uid = "KAKAO-${user.id}",
                displayName = displayName,
                email = email
            )
            SessionManager.setSession(session)
            UserDatabase.upsertUserProfile(session)
            navigateToMain()
        }
    }

    private fun startNaverLogin() {
        toggleLoading(true)
        NidOAuth.requestLogin(this, naverLoginLauncher)
    }

    private fun fetchNaverProfile() {
        NidOAuth.getUserProfile(object : NidProfileCallback<NidProfile> {
            override fun onSuccess(result: NidProfile) {
                val detail = result.profile
                val email = detail?.email
                if (email.isNullOrBlank()) {
                    showSignInError(getString(R.string.error_naver_email_required))
                    return
                }
                val safeEmail = email
                val displayName = detail.name ?: detail.nickname ?: safeEmail.substringBefore("@")
                val session = UserSession(
                    provider = AuthProvider.NAVER,
                    uid = "NAVER-${detail.id}",
                    displayName = displayName,
                    email = safeEmail
                )
                SessionManager.setSession(session)
                UserDatabase.upsertUserProfile(session)
                navigateToMain()
            }

            override fun onFailure(errorCode: String, errorDesc: String) {
                showSignInError("errorCode:$errorCode, errorDesc:$errorDesc")
            }
        })
    }

    private fun showSignInError(message: String) {
        toggleLoading(false)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun toggleLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        googleSignInButton.isEnabled = !isLoading
        kakaoLoginButton.isEnabled = !isLoading
        naverLoginButton.isEnabled = !isLoading
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
}
