package com.example.tagmoa

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.core.data.datastore.NidOAuthInitializingCallback

class TagmoaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        SessionManager.init(this)
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
}
