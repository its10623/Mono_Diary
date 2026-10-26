package com.example.diaryapp.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DiaryEntity::class,FolderEntity::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun folderDao(): FolderDao
}