package com.example.tagmoa

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {
    private lateinit var dbRef: DatabaseReference
    private lateinit var listView: ListView
    private lateinit var titles: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnAddList = findViewById<Button>(R.id.btnAddList)
        listView = findViewById(R.id.listView)
        titles = mutableListOf()

        dbRef = FirebaseDatabase.getInstance().getReference("lists")

        btnAddList.setOnClickListener {
            startActivity(Intent(this, AddListActivity::class.java))
        }

        // Firebase에서 리스트 불러오기
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                titles.clear()
                for (listSnapshot in snapshot.children) {
                    val title = listSnapshot.getValue(String::class.java)
                    if (title != null) titles.add(title)
                }
                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, titles)
                listView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
