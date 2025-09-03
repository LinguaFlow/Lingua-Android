package com.yju.presentation.module

import android.content.Context
import androidx.room.Room
import com.yju.data.auth.api.AuthService
import com.yju.data.auth.remote.AuthRemoteDataSource
import com.yju.data.auth.remote.AuthRemoteDataSourceImpl
import com.yju.data.auth.remote.AuthRepositoryImpl
import com.yju.data.kanji.dao.KanjiDatabase
import com.yju.data.kanji.remote.KanjiLocalDataSource
import com.yju.data.kanji.remote.KanjiLocalDataSourceImpl
import com.yju.data.kanji.remote.KanjiLocalRepositoryImpl
import com.yju.data.kanji.remote.KanjiRemoteDataSourceImpl
import com.yju.data.kanji.remote.KanjiRemoteRepositoryImpl
import com.yju.data.pdf.remote.PdfRemoteDataSourceImpl
import com.yju.data.pdf.remote.PdfRepositoryImpl
import com.yju.data.known.remote.KnownWordKanjiLocalDataSource
import com.yju.data.known.remote.KnownWordKanjiLocalDataSourceImpl
import com.yju.data.known.remote.KnownWordKanjiRepositoryImpl
import com.yju.domain.auth.repository.AuthRepository
import com.yju.domain.auth.util.SharedPreferenceUtil
import com.yju.domain.kanji.repository.KanjiLocalRepository
import com.yju.domain.known.repository.KanjiRemoteRepository
import com.yju.domain.pdf.repository.PdfRepository
import com.yju.domain.known.repository.KnownWordKanjiRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /* ──────── Room DB ──────── */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): KanjiDatabase =
        Room.databaseBuilder(
            context,
            KanjiDatabase::class.java,
            "kanji_database"
        ).fallbackToDestructiveMigration()
            .build()

    /* ──────── LocalDataSources ──────── */
    @Provides
    @Singleton
    fun provideKanjiLocalDataSource(db: KanjiDatabase): KanjiLocalDataSource =
        KanjiLocalDataSourceImpl(db.kanjiDao())

    @Provides
    @Singleton
    fun provideUnknownKanjiLocalDataSource(db: KanjiDatabase): KnownWordKanjiLocalDataSource =
        KnownWordKanjiLocalDataSourceImpl(db.unknownKanjiDao())

    /* ──────── Repositories ──────── */
    @Provides
    @Singleton
    fun provideKanjiRepository(local: KanjiLocalDataSource): KanjiLocalRepository =
        KanjiLocalRepositoryImpl(local)

    @Provides
    @Singleton
    fun provideUnknownKanjiRepository(local: KnownWordKanjiLocalDataSource): KnownWordKanjiRepository =
        KnownWordKanjiRepositoryImpl(local)

    @Provides
    @Singleton
    fun providePdfRepository(remote: PdfRemoteDataSourceImpl): PdfRepository =
        PdfRepositoryImpl(remote)

    @Provides
    @Singleton
    fun provideKanjiRemoteRepository(remote: KanjiRemoteDataSourceImpl): KanjiRemoteRepository =
        KanjiRemoteRepositoryImpl(remote)


    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(authService: AuthService): AuthRemoteDataSource =
        AuthRemoteDataSourceImpl(authService)

    @Singleton
    @Provides
    fun provideSharedPreferenceUtil(@ApplicationContext context: Context): SharedPreferenceUtil = SharedPreferenceUtil(context)

    /* ──────── Repositories ──────── */
    @Provides
    @Singleton
    fun provideAuthRepository(remote: AuthRemoteDataSource): AuthRepository =
        AuthRepositoryImpl(remote)

}
