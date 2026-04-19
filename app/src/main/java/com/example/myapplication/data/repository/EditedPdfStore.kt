package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.domain.model.EditedPdfItem
import org.json.JSONArray
import org.json.JSONObject

object EditedPdfStore {

    private const val PREFS_NAME = "edited_pdf_store"
    private const val KEY_ITEMS = "items"

    fun saveOrUpdate(context: Context, item: EditedPdfItem) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = load(context).toMutableList()

        val index = current.indexOfFirst { it.originalPdfPath == item.originalPdfPath }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(0, item)
        }

        val array = JSONArray()
        current.sortedByDescending { it.lastEdited }.forEach { pdf ->
            val obj = JSONObject()
            obj.put("originalPdfPath", pdf.originalPdfPath)
            obj.put("displayName", pdf.displayName)
            obj.put("savedProjectPath", pdf.savedProjectPath)
            obj.put("lastEdited", pdf.lastEdited)
            array.put(obj)
        }

        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    fun load(context: Context): List<EditedPdfItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()

        return try {
            val array = JSONArray(raw)
            val out = mutableListOf<EditedPdfItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                out.add(
                    EditedPdfItem(
                        originalPdfPath = obj.optString("originalPdfPath"),
                        displayName = obj.optString("displayName"),
                        savedProjectPath = obj.optString("savedProjectPath"),
                        lastEdited = obj.optLong("lastEdited")
                    )
                )
            }
            out.sortedByDescending { it.lastEdited }
        } catch (_: Exception) {
            emptyList()
        }
    }
}