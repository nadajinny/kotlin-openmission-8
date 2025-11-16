package com.ndjinny.tagmoa.controller

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.ndjinny.tagmoa.R

class HomeBackgroundSettingsFragment : Fragment(R.layout.fragment_home_background_settings) {

    private val selectBackgroundLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                persistUriPermission(uri)
                selectedBackgroundUri = uri
                previewHint.setText(R.string.home_background_preview_hint)
                updatePreviewBackground()
            }
        }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var previewImage: ImageView
    private lateinit var previewDate: TextView
    private lateinit var previewHint: TextView
    private lateinit var groupDateColor: MaterialButtonToggleGroup
    private lateinit var buttonUpload: MaterialButton
    private lateinit var buttonUseDefault: MaterialButton
    private lateinit var buttonApply: MaterialButton

    private var selectedBackgroundUri: Uri? = null
    private var selectedDateColor: HomeDateTextColor = HomeDateTextColor.DARK

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbarHomeBackground)
        previewImage = view.findViewById(R.id.imageBackgroundPreview)
        previewDate = view.findViewById(R.id.textPreviewDate)
        previewHint = view.findViewById(R.id.textPreviewHint)
        groupDateColor = view.findViewById(R.id.groupDateColor)
        buttonUpload = view.findViewById(R.id.buttonUploadBackground)
        buttonUseDefault = view.findViewById(R.id.buttonUseDefaultBackground)
        buttonApply = view.findViewById(R.id.buttonApplyBackground)

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        selectedBackgroundUri = HomeBackgroundManager.getBackgroundUri(requireContext())
        selectedDateColor = HomeBackgroundManager.getDateTextColor(requireContext())

        val initialSelection = if (selectedDateColor == HomeDateTextColor.LIGHT) {
            R.id.buttonDateColorLight
        } else {
            R.id.buttonDateColorDark
        }
        groupDateColor.check(initialSelection)
        applyDateColorToPreview()

        updatePreviewBackground()

        buttonUpload.setOnClickListener {
            selectBackgroundLauncher.launch(arrayOf("image/*"))
        }
        buttonUseDefault.setOnClickListener {
            selectedBackgroundUri = null
            previewHint.setText(R.string.home_background_preview_hint)
            updatePreviewBackground()
        }
        groupDateColor.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedDateColor = if (checkedId == R.id.buttonDateColorLight) {
                HomeDateTextColor.LIGHT
            } else {
                HomeDateTextColor.DARK
            }
            applyDateColorToPreview()
        }
        buttonApply.setOnClickListener {
            applySelection()
        }
    }

    private fun updatePreviewBackground() {
        val uri = selectedBackgroundUri
        if (uri == null) {
            previewImage.setImageResource(R.drawable.bg_home_preview_placeholder)
            previewHint.visibility = View.VISIBLE
            return
        }
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    previewImage.setImageBitmap(bitmap)
                    previewHint.visibility = View.GONE
                } else {
                    throw IllegalArgumentException("Bitmap decode failed")
                }
            } ?: throw IllegalArgumentException("Unable to open stream")
        } catch (_: Exception) {
            previewImage.setImageResource(R.drawable.bg_home_preview_placeholder)
            previewHint.setText(R.string.home_background_preview_error)
            previewHint.visibility = View.VISIBLE
        }
    }

    private fun applyDateColorToPreview() {
        val colorInt = ContextCompat.getColor(requireContext(), selectedDateColor.colorRes)
        previewDate.setTextColor(colorInt)
        previewDate.setShadowLayer(0f, 0f, 0f, 0)
    }

    private fun applySelection() {
        val context = requireContext()
        val uri = selectedBackgroundUri
        if (uri != null) {
            persistUriPermission(uri)
            HomeBackgroundManager.saveBackgroundUri(context, uri)
        } else {
            HomeBackgroundManager.clearBackgroundUri(context)
        }
        HomeBackgroundManager.saveDateTextColor(context, selectedDateColor)
        Toast.makeText(
            context,
            getString(R.string.home_background_settings_saved),
            Toast.LENGTH_SHORT
        ).show()
        requireActivity().finish()
    }

    private fun persistUriPermission(uri: Uri) {
        val resolver = requireContext().contentResolver
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            resolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: SecurityException) {
            // Already granted or not needed
        }
    }
}
