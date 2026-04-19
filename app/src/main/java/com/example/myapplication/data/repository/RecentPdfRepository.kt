package com.example.myapplication.data.repository

import androidx.lifecycle.LiveData
import com.example.myapplication.data.local.recentpdf.RecentPdfDao
import com.example.myapplication.data.local.recentpdf.RecentPdfEntity

class RecentPdfRepository(
    private val recentPdfDao: RecentPdfDao
) {

    fun getRecentPdfs(): LiveData<List<RecentPdfEntity>> {
        return recentPdfDao.getRecentPdfs()
    }

    suspend fun addRecentPdf(uri: String, name: String) {
        recentPdfDao.insertRecentPdf(
            RecentPdfEntity(
                uri = uri,
                name = name,
                lastOpened = System.currentTimeMillis()
            )
        )
        recentPdfDao.keepOnlyLatest10()
    }

    suspend fun clearAll() {
        recentPdfDao.clearAll()
    }

    suspend fun deleteByUri(uri: String) {
        recentPdfDao.deleteByUri(uri)
    }
}