package com.example.testpos.database.transaction

import android.database.Cursor
import androidx.room.*

@Dao
interface ReversalDao {
    @Query("SELECT * FROM ReversalEntity ORDER BY _id DESC LIMIT 1")
    fun getReversal():ReversalEntity

    @Query("SELECT * FROM ReversalEntity")
    fun getAllReversal() : Cursor

    @Insert
    fun insertReversal(reversalEntity: ReversalEntity)

    @Delete
    fun deleteReversal(reversalEntity: ReversalEntity)

    @Update
    fun updateReversal(reversalEntity: ReversalEntity)
}
