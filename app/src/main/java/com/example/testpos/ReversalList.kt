package com.example.testpos

import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import com.example.testpos.database.transaction.ReversalEntity

class ReversalList : AppCompatActivity() {

    var main : MainActivity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reversal_list)

        val reversalList : ListView = findViewById(R.id.reverselList)

        main = MainActivity()

        Thread{

            main!!.accessDatabase()

            var cursor : Cursor? = main!!.reversalDAO?.getAllReversal()

            runOnUiThread{
                var adapter : SimpleCursorAdapter = SimpleCursorAdapter(

                    this,
                    R.layout.reversal_item,
                    cursor,
                    arrayOf(cursor?.getColumnName(0),
                    cursor?.getColumnName(1)),
                    intArrayOf(R.id.id,R.id.isoMsg),
                    1

                )
                reversalList.setAdapter(adapter)
            }

        }.start()
    }
}