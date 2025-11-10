package com.example.tagmoa

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class AddListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_list)

        val editTitle = findViewById<EditText>(R.id.editTitle)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val title = editTitle.text.toString().trim()
            if (title.isNotEmpty()) {
                val dbRef = FirebaseDatabase.getInstance().getReference("lists")
                val id = dbRef.push().key!!
                dbRef.child(id).setValue(title)
                    .addOnSuccessListener {
                        Toast.makeText(this, "저장 완료!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
