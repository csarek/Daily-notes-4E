package com.example.notewidget.data

import java.time.format.DateTimeFormatter

/** A jegyzetfájlok névformátuma és kiterjesztése (ezt egy helyen lehet átállítani). */
object NoteDate {
    /** Fájlnév: 2026_10_30 */
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd")

    /** Új fájlok kiterjesztése (Obsidian-kompatibilis). */
    const val EXTENSION = "md"
}
