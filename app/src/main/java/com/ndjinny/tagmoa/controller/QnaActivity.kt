package com.ndjinny.tagmoa.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.QnaComment
import com.ndjinny.tagmoa.model.QnaEntry
import com.ndjinny.tagmoa.model.QnaRepository
import com.ndjinny.tagmoa.model.SessionManager
import com.ndjinny.tagmoa.view.QnaAdapter
import com.ndjinny.tagmoa.view.asDateLabel
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class QnaActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var fabAdd: ExtendedFloatingActionButton
    private lateinit var adapter: QnaAdapter

    private var qnaListener: ValueEventListener? = null
    private var toolbarBasePaddingTop = 0
    private var recyclerBasePaddingBottom = 0
    private var fabBaseMarginBottom = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val session = SessionManager.currentSession
        if (session == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_qna)

        rootView = findViewById(R.id.rootQna)
        toolbar = findViewById(R.id.toolbarQna)
        recyclerView = findViewById(R.id.recyclerQna)
        emptyView = findViewById(R.id.textQnaEmpty)
        fabAdd = findViewById(R.id.fabAddQna)

        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        toolbarBasePaddingTop = toolbar.paddingTop
        recyclerBasePaddingBottom = recyclerView.paddingBottom
        fabBaseMarginBottom = (fabAdd.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin

        applyWindowInsets()

        adapter = QnaAdapter { showEntryDetail(it) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener { showCreateDialog() }

        observeEntries()
    }

    override fun onDestroy() {
        super.onDestroy()
        qnaListener?.let { QnaRepository.entriesRef().removeEventListener(it) }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar.updatePadding(top = toolbarBasePaddingTop + systemBars.top)
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                recyclerView.paddingTop,
                recyclerView.paddingRight,
                recyclerBasePaddingBottom + systemBars.bottom
            )
            val fabLp = fabAdd.layoutParams as ViewGroup.MarginLayoutParams
            fabLp.bottomMargin = fabBaseMarginBottom + systemBars.bottom
            fabAdd.layoutParams = fabLp
            insets
        }
        ViewCompat.requestApplyInsets(rootView)
    }

    private fun observeEntries() {
        val session = SessionManager.currentSession ?: return
        qnaListener?.let { QnaRepository.entriesRef().removeEventListener(it) }
        val admin = isAdmin(session.email)

        qnaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = snapshot.children.mapNotNull { child ->
                    child.getValue(QnaEntry::class.java)?.apply {
                        if (id.isNullOrEmpty()) id = child.key
                    }
                }
                val filtered = entries.filter { entry ->
                    admin || entry.isPublic || entry.authorUid == session.uid
                }.sortedByDescending { it.createdAt }
                adapter.submitList(filtered)
                emptyView.isVisible = filtered.isEmpty()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@QnaActivity, getString(R.string.qna_toast_error), Toast.LENGTH_LONG).show()
            }
        }
        QnaRepository.entriesRef().addValueEventListener(qnaListener as ValueEventListener)
    }

    private fun showCreateDialog() {
        val session = SessionManager.currentSession ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_qna_submit, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.inputQnaTitle)
        val contentInput = dialogView.findViewById<TextInputEditText>(R.id.inputQnaContent)
        val visibilitySwitch = dialogView.findViewById<SwitchMaterial>(R.id.switchQnaVisibility)

        updateVisibilityLabel(visibilitySwitch, visibilitySwitch.isChecked)
        visibilitySwitch.setOnCheckedChangeListener { _, isChecked ->
            updateVisibilityLabel(visibilitySwitch, isChecked)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.qna_dialog_title)
            .setView(dialogView)
            .setPositiveButton(R.string.qna_submit, null)
            .setNegativeButton(R.string.qna_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleInput.text?.toString()?.trim().orEmpty()
                val content = contentInput.text?.toString()?.trim().orEmpty()
                if (title.isBlank() || content.isBlank()) {
                    Toast.makeText(this, getString(R.string.qna_toast_validation), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                submitEntry(session.uid, session.displayName, session.email, title, content, visibilitySwitch.isChecked)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showEntryDetail(entry: QnaEntry) {
        val session = SessionManager.currentSession ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_qna_detail, null)
        val title = dialogView.findViewById<TextView>(R.id.textQnaDetailTitle)
        val meta = dialogView.findViewById<TextView>(R.id.textQnaDetailMeta)
        val content = dialogView.findViewById<TextView>(R.id.textQnaDetailContent)
        val commentsLabel = dialogView.findViewById<TextView>(R.id.textQnaCommentsLabel)
        val commentsContainer = dialogView.findViewById<LinearLayout>(R.id.containerQnaComments)
        val commentsEmpty = dialogView.findViewById<TextView>(R.id.textQnaNoComments)
        val commentInputLayout = dialogView.findViewById<TextInputLayout>(R.id.layoutQnaCommentInput)
        val commentInput = dialogView.findViewById<TextInputEditText>(R.id.inputQnaComment)
        val restrictionText = dialogView.findViewById<TextView>(R.id.textQnaCommentRestriction)

        title.text = entry.title.orEmpty()
        val authorName = entry.authorName?.takeIf { it.isNotBlank() }
            ?: getString(R.string.qna_meta_unknown_author)
        val dateLabel = entry.createdAt.takeIf { it > 0 }?.asDateLabel().orEmpty()
        meta.text = if (dateLabel.isNotBlank()) {
            getString(R.string.qna_meta_format, authorName, dateLabel)
        } else {
            authorName
        }
        content.text = entry.content.orEmpty()
        commentsLabel.isVisible = true

        val comments = entry.comments?.values?.sortedBy { it.createdAt } ?: emptyList()
        populateComments(commentsContainer, comments)
        commentsEmpty.isVisible = comments.isEmpty()

        val canComment = canCommentOnEntry(session.uid, session.email, entry)
        commentInputLayout.isVisible = canComment
        restrictionText.isVisible = !canComment

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(entry.title)
            .setView(dialogView)

        if (canComment) {
            builder.setPositiveButton(R.string.qna_submit, null)
                .setNegativeButton(R.string.qna_cancel, null)
        } else {
            builder.setPositiveButton(android.R.string.ok, null)
        }

        val dialog = builder.create()
        dialog.setOnShowListener {
            if (canComment) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val text = commentInput.text?.toString()?.trim().orEmpty()
                    if (text.isBlank()) {
                        Toast.makeText(this, getString(R.string.qna_toast_comment_validation), Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    submitComment(entry.id, session.uid, session.displayName, session.email, text)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun submitEntry(
        authorUid: String,
        authorName: String?,
        authorEmail: String?,
        title: String,
        content: String,
        isPublic: Boolean
    ) {
        val newRef = QnaRepository.entriesRef().push()
        val entry = QnaEntry(
            id = newRef.key,
            title = title,
            content = content,
            authorUid = authorUid,
            authorName = authorName,
            authorEmail = authorEmail,
            isPublic = isPublic,
            createdAt = System.currentTimeMillis()
        )
        newRef.setValue(entry).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, getString(R.string.qna_toast_saved), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.qna_toast_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun submitComment(
        entryId: String?,
        authorUid: String,
        authorName: String?,
        authorEmail: String?,
        content: String
    ) {
        if (entryId == null) return
        val ref = QnaRepository.entriesRef().child(entryId).child("comments").push()
        val comment = QnaComment(
            id = ref.key,
            authorUid = authorUid,
            authorName = authorName,
            authorEmail = authorEmail,
            content = content,
            createdAt = System.currentTimeMillis()
        )
        ref.setValue(comment).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, getString(R.string.qna_toast_comment_saved), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.qna_toast_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun populateComments(container: LinearLayout, comments: List<QnaComment>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        comments.forEach { comment ->
            val view = inflater.inflate(R.layout.item_qna_comment, container, false)
            val author = view.findViewById<TextView>(R.id.textCommentAuthor)
            val body = view.findViewById<TextView>(R.id.textCommentContent)
            val name = comment.authorName?.takeIf { it.isNotBlank() }
                ?: comment.authorEmail?.takeIf { it.isNotBlank() }
                ?: getString(R.string.qna_meta_unknown_author)
            val dateLabel = comment.createdAt.takeIf { it > 0 }?.asDateLabel().orEmpty()
            author.text = if (dateLabel.isNotBlank()) {
                getString(R.string.qna_meta_format, name, dateLabel)
            } else {
                name
            }
            body.text = comment.content.orEmpty()
            container.addView(view)
        }
    }

    private fun updateVisibilityLabel(switch: SwitchMaterial, isPublic: Boolean) {
        val textRes = if (isPublic) {
            R.string.qna_visibility_public_option
        } else {
            R.string.qna_visibility_private_option
        }
        switch.setText(textRes)
    }

    private fun isAdmin(email: String?): Boolean {
        return email.equals(ADMIN_EMAIL, ignoreCase = true)
    }

    private fun canCommentOnEntry(userUid: String, email: String?, entry: QnaEntry): Boolean {
        return isAdmin(email) || entry.authorUid == userUid
    }

    companion object {
        private const val ADMIN_EMAIL = "nadajinny@gmail.com"
    }
}
