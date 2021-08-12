package com.example.testpos.database.transaction

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity
class SaleEntity(

        @PrimaryKey(autoGenerate = true)
        @NotNull
        var _id: Int?,

        @ColumnInfo(name = "iso_msg")
        val isoMsg: String,

        @ColumnInfo(name = "STAN")
        val STAN: Int?


    )
