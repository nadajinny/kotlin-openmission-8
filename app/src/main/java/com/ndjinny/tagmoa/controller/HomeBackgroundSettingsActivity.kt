package com.ndjinny.tagmoa.controller

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ndjinny.tagmoa.R

class HomeBackgroundSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_background_settings)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.containerHomeBackgroundSettings, HomeBackgroundSettingsFragment())
                .commit()
        }
    }
}
