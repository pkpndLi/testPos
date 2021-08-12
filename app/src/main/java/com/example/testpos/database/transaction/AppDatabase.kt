package com.example.testpos.database.transaction

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = arrayOf(SaleEntity::class,ReversalEntity::class),version = 1)
abstract class AppDatabase : RoomDatabase()
{
    abstract  fun reversalDao():ReversalDao
    abstract  fun saleDao():SaleDao

    companion object{
        @Volatile private var instance : AppDatabase? = null

        fun getAppDatabase(context : Context) : AppDatabase?
        {
            if(instance == null){

                synchronized(AppDatabase::class)
                {

                    if(instance == null)
                    {
                        instance = Room.databaseBuilder(context.applicationContext,AppDatabase::class.java,"transactionDB").build()
                    }

                }
            }

        return instance
        }
    }


}