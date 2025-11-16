package com.ndjinny.tagmoa.controller

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.AlarmPreferences

class AlarmManagementActivity : AppCompatActivity() {

    private lateinit var switchMajorAlarm: SwitchMaterial
    private lateinit var switchSubAlarm: SwitchMaterial
    private lateinit var textMajorTime: TextView
    private lateinit var textSubTime: TextView
    private lateinit var rowMajorTime: View
    private lateinit var rowSubTime: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_management)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbarAlarmManagement)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        switchMajorAlarm = findViewById(R.id.switchMajorAlarm)
        switchSubAlarm = findViewById(R.id.switchSubAlarm)
        textMajorTime = findViewById(R.id.textMajorAlarmTime)
        textSubTime = findViewById(R.id.textSubAlarmTime)
        rowMajorTime = findViewById(R.id.rowMajorAlarmTime)
        rowSubTime = findViewById(R.id.rowSubAlarmTime)

        switchMajorAlarm.isChecked = AlarmPreferences.isMajorAlarmEnabled(this)
        switchSubAlarm.isChecked = AlarmPreferences.isSubAlarmEnabled(this)
        textMajorTime.text = AlarmPreferences.getMajorAlarmTime(this)
        textSubTime.text = AlarmPreferences.getSubAlarmTime(this)

        switchMajorAlarm.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferences.setMajorAlarmEnabled(this, isChecked)
        }

        switchSubAlarm.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferences.setSubAlarmEnabled(this, isChecked)
        }

        rowMajorTime.setOnClickListener { showComingSoonToast() }
        rowSubTime.setOnClickListener { showComingSoonToast() }
    }

    private fun showComingSoonToast() {
        Toast.makeText(this, R.string.alarm_time_setting_placeholder, Toast.LENGTH_SHORT).show()
    }
}
