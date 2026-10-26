package com.example.diaryapp.di

import android.content.Context
import androidx.room.Room
import com.example.diaryapp.data.local.room.DiaryDao
import com.example.diaryapp.data.local.room.DiaryDatabase
import com.example.diaryapp.data.local.room.FolderDao
import com.example.diaryapp.data.local.room.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDiaryDatabase(
        @ApplicationContext context: Context
    ): DiaryDatabase =
        Room.databaseBuilder(
            context,
            DiaryDatabase::class.java,
            "diary_db"
        )
            // 스키마 변경 시 기존 일기를 지우지 않도록 버전별 Migration으로만 업그레이드한다
            .addMigrations(MIGRATION_2_3)
            .build()

    @Provides
    fun provideDiaryDao(
        db: DiaryDatabase
    ): DiaryDao = db.diaryDao()

    @Provides
    fun provideFolderDao(
        db: DiaryDatabase
    ): FolderDao = db.folderDao()

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext app: Context): Context = app
}