package com.example.notewidget.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notewidget.data.NoteFileManager
import com.example.notewidget.widget.TodayNoteWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val fileManager = NoteFileManager(app)
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    private var saveJob: Job? = null
    private var pending: Pair<String, String>? = null // (dÃ¡tum, tartalom)

    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    var currentContent by mutableStateOf("")
        private set

    init {
        reload()
    }

    private suspend fun read(date: LocalDate): String =
        withContext(Dispatchers.IO) { fileManager.readNoteForDate(date.format(fmt)) }

    /** A kivÃ¡lasztott nap jegyzetÃ©nek ÃºjraolvasÃ¡sa (pl. mappavÃ¡lasztÃ¡s vagy onResume utÃ¡n). */
    fun reload() {
        viewModelScope.launch {
            if (pending == null) currentContent = read(selectedDate)
        }
    }

    fun selectDate(date: LocalDate) {
        viewModelScope.launch {
            saveJob?.cancelAndJoin()
            flush() // a fÃ¼ggÅ mentÃ©s ne vesszen el
            selectedDate = date
            currentContent = read(date)
        }
    }

    fun updateContent(text: String) {
        currentContent = text
        pending = selectedDate.format(fmt) to text
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500) // debounce: csak gÃ©pelÃ©s szÃ¼netÃ©ben mentÃ¼nk
            flush()
        }
    }

    /** Azonnali mentÃ©s (pl. az app hÃ¡ttÃ©rbe kerÃ¼lÃ©sekor). */
    fun flushNow() {
        viewModelScope.launch { flush() }
    }

    private suspend fun flush() {
        val (date, text) = pending ?: return
        pending = null
        withContext(Dispatchers.IO + NonCancellable) {
            fileManager.saveNoteForDate(date, text)
            if (date == LocalDate.now().format(fmt)) {
                TodayNoteWidget().updateAll(getApplication())
            }
        }
    }
}
