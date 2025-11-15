package com.ndjinny.tagmoa.view.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.ndjinny.tagmoa.R

class TwoButtonDialogFragment(
    private val title: String,
    private val message: String,
    private val primaryText: String,
    private val secondaryText: String,
    private val onPrimaryClick: () -> Unit,
    private val onSecondaryClick: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_two_button, null, false)

        val titleView = view.findViewById<TextView>(R.id.textTitle)
        val messageView = view.findViewById<TextView>(R.id.textMessage)
        val primaryButton = view.findViewById<MaterialButton>(R.id.btnPrimary)
        val secondaryButton = view.findViewById<MaterialButton>(R.id.btnSecondary)

        titleView.text = title
        messageView.text = message

        primaryButton.text = primaryText
        secondaryButton.text = secondaryText

        primaryButton.setOnClickListener {
            onPrimaryClick()
            dismiss()
        }
        secondaryButton.setOnClickListener {
            onSecondaryClick()
            dismiss()
        }

        return Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(view)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window?.setGravity(Gravity.CENTER)
        }
    }
}
