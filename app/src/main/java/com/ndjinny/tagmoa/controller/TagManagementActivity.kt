package com.ndjinny.tagmoa.controller

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.Tag
import com.ndjinny.tagmoa.model.UserDatabase
import com.google.firebase.database.DatabaseReference

class TagManagementActivity : AppCompatActivity() {

    private lateinit var tagsRef: DatabaseReference
    private lateinit var userId: String
    private lateinit var inputTagName: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uid = requireUserIdOrRedirect() ?: return
        userId = uid
        tagsRef = UserDatabase.tagsRef(userId)
        setContentView(R.layout.activity_tag_management)

        inputTagName = findViewById(R.id.editTagName)

        val btnAddTag = findViewById<Button>(R.id.btnAddTag)

        btnAddTag.setOnClickListener { handleCreateTag() }
    }

    private fun handleCreateTag() {
        val name = inputTagName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_tag_name, Toast.LENGTH_SHORT).show()
            return
        }
        tagsRef.get()
            .addOnSuccessListener { snapshot ->
                val exists = snapshot.children.any { child ->
                    val tag = child.getValue(Tag::class.java)
                    val existingName = tag?.name ?: return@any false
                    existingName.equals(name, ignoreCase = true)
                }
                if (exists) {
                    Toast.makeText(this, R.string.error_duplicate_tag_name, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val newId = tagsRef.push().key ?: return@addOnSuccessListener
                val newTag = Tag(id = newId, name = name, hidden = false)
                tagsRef.child(newId).setValue(newTag)
                    .addOnSuccessListener {
                        inputTagName.text.clear()
                        Toast.makeText(this, R.string.message_tag_created, Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(
                            this,
                            error.message ?: getString(R.string.error_generic),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    error.message ?: getString(R.string.error_generic),
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
