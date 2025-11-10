package com.example.tagmoa

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class TagmoaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        SessionManager.init(this)
        KakaoSdk.init(this, getString(R.string.kakao_app_key))
    }

    companion object {
        lateinit var instance: TagmoaApp
            private set
    }
}
