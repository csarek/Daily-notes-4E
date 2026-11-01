package com.example.notewidget.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

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

    /**
     * A mappa összes fájlja (név -> URI) EGYETLEN lekérdezéssel.
     * Ez nagyságrendekkel gyorsabb, mint fájlonként lekérdezni a nevet.
     */
    private fun childIndex(root: Uri): Map<String, Uri> {
        val result = HashMap<String, Uri>()
        try {
            val treeDocId = DocumentsContract.getTreeDocumentId(root)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(root, treeDocId)
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    result[name] = DocumentsContract.buildDocumentUriUsingTree(root, id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * Az adott nap fájlja. Sorrend: 2026_10_30.md, 2026_10_30.txt,
     * 2026-10-30.md, 2026-10-30.txt.
     */
    private fun findNoteUri(root: Uri, dateString: String): Uri? {
        val dashed = dateString.replace('_', '-')
        val candidates = listOf(
            "$dateString.md", "$dateString.txt",
            "$dashed.md", "$dashed.txt"
        )
        val index = childIndex(root)
        return candidates.firstNotNullOfOrNull { index[it] }
    }

    fun readNoteForDate(dateString: String): String {
        val root = getRootUri() ?: return ""
        val uri = findNoteUri(root, dateString) ?: return ""
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun saveNoteForDate(dateString: String, content: String) {
        val root = getRootUri() ?: return
        val uri = findNoteUri(root, dateString) ?: createNote(root, dateString) ?: return
        try {
            // "wt" = írás + a fájl tartalmának törlése előtte
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNote(root: Uri, dateString: String): Uri? {
        return try {
            val treeDocId = DocumentsContract.getTreeDocumentId(root)
            val parent = DocumentsContract.buildDocumentUriUsingTree(root, treeDocId)
            DocumentsContract.createDocument(
                context.contentResolver,
                parent,
                "text/markdown",
                "$dateString.${NoteDate.EXTENSION}"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
