package com.example.testpos.database.transaction

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity
data class ReversalEntity(
    @PrimaryKey(autoGenerate = true)
    @NotNull
    val _id: Int?,

    @ColumnInfo(name = "iso_msg")
    val isoMsg: String
)

