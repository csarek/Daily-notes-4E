package com.example.notewidget.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class NoteFileManager(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("note_prefs", Context.MODE_PRIVATE)
    }

    fun saveRootUri(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        prefs.edit().putString("root_folder_uri", uri.toString()).apply()
    }

    fun getRootUri(): Uri? {
        val s = prefs.getString("root_folder_uri", null) ?: return null
        return Uri.parse(s)
    }

    /** Igaz, ha van mentett mappa ÃS az engedÃ©ly mÃ©g Ã©rvÃ©nyes. */
    fun hasValidRoot(): Boolean {
        val root = getRootUri() ?: return false
        return context.contentResolver.persistedUriPermissions.any {
            it.uri == root && it.isReadPermission && it.isWritePermission
        }
    }

    private fun rootDir(): DocumentFile? {
        val root = getRootUri() ?: return null
        return DocumentFile.fromTreeUri(context, root)
    }

    fun readNoteForDate(dateString: String): String {
        val dir = rootDir() ?: return ""
        val file = dir.findFile("$dateString.txt") ?: return ""
        return try {
            context.contentResolver.openInputStream(file.uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun saveNoteForDate(dateString: String, content: String) {
        val dir = rootDir() ?: return
        val name = "$dateString.txt"
        val file = dir.findFile(name) ?: dir.createFile("text/plain", name) ?: return
        try {
            // "wt" = Ã­rÃ¡s + a fÃ¡jl tartalmÃ¡nak tÃ¶rlÃ©se elÅtte
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
