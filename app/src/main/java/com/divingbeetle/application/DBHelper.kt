package com.divingbeetle.application

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DBHelper(val context: Context) {
    companion object {
        const val dbName = "data"
    }

    val database: SQLiteDatabase

    init {
        database = open()
    }

    private fun open(): SQLiteDatabase {
        val dbFile = context.getDatabasePath("$dbName.db")
        if (!dbFile.exists()) {
            try {
                val checkDB = context.openOrCreateDatabase("$dbName.db", Context.MODE_PRIVATE, null)
                checkDB.close()
                copyDatabase(dbFile)
            } catch (e: IOException) {
                throw RuntimeException("Error opening db")
            }
        }
        return SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
    }

    private fun copyDatabase(dbFile: File) {
        val iss = context.assets.open("$dbName.db")
        val os = FileOutputStream(dbFile)

        val buffer = ByteArray(1024)
        while (iss.read(buffer) > 0) {
            os.write(buffer)
        }
        os.flush()
        os.close()
        iss.close()
    }

    fun close() {
        database.close()
    }
}