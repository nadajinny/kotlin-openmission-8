package com.ndjinny.tagmoa.controller

import android.app.Application
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.SessionManager
import com.ndjinny.tagmoa.model.TaskCompletionSyncManager
import com.kakao.sdk.common.KakaoSdk
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.core.data.datastore.NidOAuthInitializingCallback
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseException

class TagmoaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        SessionManager.init(this)
        TaskCompletionSyncManager.init(this)
        enableFirebasePersistence()
        KakaoSdk.init(this, getString(R.string.kakao_app_key))
        initializeNaverSdk()
    }

    companion object {
        lateinit var instance: TagmoaApp
            private set
    }

    private fun initializeNaverSdk() {
        NidOAuth.initialize(
            this,
            getString(R.string.naver_client_id),
            getString(R.string.naver_client_secret),
            getString(R.string.naver_client_name),
            object : NidOAuthInitializingCallback {
                override fun onSuccess() {}
                override fun onFailure(e: Exception) {
                    e.printStackTrace()
                }
            }
        )
    }

    private fun enableFirebasePersistence() {
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (_: DatabaseException) {
            // Already enabled elsewhere; ignore.
        }
    }
}
