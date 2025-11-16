package com.ndjinny.tagmoa.controller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    private var suppressSwitchCallbacks = false
    private var pendingPermissionSwitch: SwitchMaterial? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingPermissionSwitch?.let { switch ->
                    suppressSwitchUpdate {
                        switch.isChecked = true
                    }
                    applySwitchState(switch, true)
                }
            } else {
                Toast.makeText(this, R.string.alarm_permission_denied, Toast.LENGTH_SHORT).show()
                pendingPermissionSwitch?.let { switch ->
                    suppressSwitchUpdate {
                        switch.isChecked = false
                    }
                }
            }
            pendingPermissionSwitch = null
        }

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

        suppressSwitchUpdate {
            switchMajorAlarm.isChecked = AlarmPreferences.isMajorAlarmEnabled(this)
            switchSubAlarm.isChecked = AlarmPreferences.isSubAlarmEnabled(this)
        }
        textMajorTime.text = AlarmPreferences.getMajorAlarmTime(this)
        textSubTime.text = AlarmPreferences.getSubAlarmTime(this)

        switchMajorAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            handleSwitchToggle(switchMajorAlarm, isChecked)
        }

        switchSubAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            handleSwitchToggle(switchSubAlarm, isChecked)
        }

        rowMajorTime.setOnClickListener { showComingSoonToast() }
        rowSubTime.setOnClickListener { showComingSoonToast() }
    }

    private fun handleSwitchToggle(targetSwitch: SwitchMaterial, isChecked: Boolean) {
        if (isChecked && !hasNotificationPermission()) {
            pendingPermissionSwitch = targetSwitch
            Toast.makeText(this, R.string.alarm_permission_rationale, Toast.LENGTH_SHORT).show()
            requestNotificationPermission()
            suppressSwitchUpdate {
                targetSwitch.isChecked = false
            }
            return
        }
        applySwitchState(targetSwitch, isChecked)
    }

    private fun applySwitchState(targetSwitch: SwitchMaterial, isChecked: Boolean) {
        when (targetSwitch.id) {
            R.id.switchMajorAlarm -> AlarmPreferences.setMajorAlarmEnabled(this, isChecked)
            R.id.switchSubAlarm -> AlarmPreferences.setSubAlarmEnabled(this, isChecked)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private inline fun suppressSwitchUpdate(block: () -> Unit) {
        suppressSwitchCallbacks = true
        block()
        suppressSwitchCallbacks = false
    }

    private fun showComingSoonToast() {
        Toast.makeText(this, R.string.alarm_time_setting_placeholder, Toast.LENGTH_SHORT).show()
    }
}
