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

    /** Igaz, ha van mentett mappa ÉS az engedély még érvényes. */
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

    /**
     * Megkeresi az adott nap fájlját. Sorrend: 2026_10_30.md, 2026_10_30.txt,
     * 2026-10-30.md, 2026-10-30.txt. Egyetlen mappa-beolvasással dolgozik.
     */
    private fun findNoteFile(dir: DocumentFile, dateString: String): DocumentFile? {
        val dashed = dateString.replace('_', '-')
        val candidates = listOf(
            "$dateString.md", "$dateString.txt",
            "$dashed.md", "$dashed.txt"
        )
        val files = dir.listFiles().filter { it.name in candidates }
        return candidates.firstNotNullOfOrNull { c -> files.firstOrNull { it.name == c } }
    }

    fun readNoteForDate(dateString: String): String {
        val dir = rootDir() ?: return ""
        val file = findNoteFile(dir, dateString) ?: return ""
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
        val name = "$dateString.${NoteDate.EXTENSION}"
        val file = findNoteFile(dir, dateString) ?: dir.createFile("text/markdown", name) ?: return
        try {
            // "wt" = írás + a fájl tartalmának törlése előtte
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
