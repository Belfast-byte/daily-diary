package com.example.dailydiary.core.export

import android.content.Context
import com.example.dailydiary.domain.model.DiaryEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

class DiaryExporter(private val context: Context) {

    fun exportJson(entries: List<DiaryEntry>, tagsProvider: (Long) -> List<String>): File {
        val arr = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("date", entry.entryDate.toString())
            obj.put("mood", entry.moodId)
            obj.put("content", entry.content)
            obj.put("tags", JSONArray(tagsProvider(entry.id)))
            arr.put(obj)
        }
        val file = File(context.cacheDir, "daily_diary_export_${LocalDate.now()}.json")
        file.writeText(arr.toString(2))
        return file
    }

    fun exportCsv(entries: List<DiaryEntry>, tagsProvider: (Long) -> List<String>): File {
        val sb = StringBuilder()
        sb.appendLine("date,mood,content,tags")
        entries.forEach { entry ->
            val tags = tagsProvider(entry.id).joinToString(";")
            val content = entry.content.replace("\"", "\"\"")
            sb.appendLine("${entry.entryDate},${entry.moodId},\"$content\",\"$tags\"")
        }
        val file = File(context.cacheDir, "daily_diary_export_${LocalDate.now()}.csv")
        file.writeText(sb.toString())
        return file
    }
}
