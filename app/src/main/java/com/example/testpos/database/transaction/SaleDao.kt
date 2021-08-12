package com.example.testpos.database.transaction

import android.database.Cursor
import androidx.room.*

@Dao
interface SaleDao {

    @Query("SELECT * FROM SaleEntity ORDER BY _id DESC LIMIT 1")
    fun getSale(): SaleEntity

    @Query("SELECT * FROM SaleEntity")
    fun getAllSale() : Cursor

    @Insert
    fun insertSale(saleEntity: SaleEntity)

    @Delete
    fun deleteSale(saleEntity: SaleEntity)

    @Update
    fun updateSale(saleEntity: SaleEntity)
}