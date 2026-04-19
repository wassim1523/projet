package com.example.myapplication.data.local.recentpdf

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecentPdfDao {

    @Query("SELECT * FROM recent_pdfs ORDER BY lastOpened DESC")
    fun getRecentPdfs(): LiveData<List<RecentPdfEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentPdf(pdf: RecentPdfEntity)

    @Query("DELETE FROM recent_pdfs WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM recent_pdfs")
    suspend fun clearAll()

    @Query("""
        DELETE FROM recent_pdfs
        WHERE uri NOT IN (
            SELECT uri FROM recent_pdfs
            ORDER BY lastOpened DESC
            LIMIT 10
        )
    """)
    suspend fun keepOnlyLatest10()
}