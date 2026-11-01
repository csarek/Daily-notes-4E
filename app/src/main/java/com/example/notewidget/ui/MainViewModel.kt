package com.example.notewidget.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notewidget.data.NoteDate
import com.example.notewidget.data.NoteFileManager
import com.example.notewidget.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val fileManager = NoteFileManager(app)
    private val fmt = NoteDate.formatter

    private var saveJob: Job? = null
    private var loadJob: Job? = null
    private var pending: Pair<String, String>? = null // (dátum, tartalom)

    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    var currentContent by mutableStateOf("")
        private set

    /** Igaz, amíg az új nap jegyzete töltődik (ekkor a szerkesztő csak olvasható). */
    var isLoading by mutableStateOf(false)
        private set

    init {
        load()
    }

    private suspend fun read(date: LocalDate): String =
        withContext(Dispatchers.IO) { fileManager.readNoteForDate(date.format(fmt)) }

    /**
     * A kiválasztott nap betöltése. Előbb elmenti a függő változtatást,
     * és ha közben új betöltés indul, a régit eldobja.
     */
    private fun load() {
        loadJob?.cancel()
        isLoading = true
        val date = selectedDate
        loadJob = viewModelScope.launch {
            saveJob?.cancelAndJoin()
            flush()
            currentContent = read(date)
            isLoading = false
        }
    }

    /** Újraolvasás (pl. mappaválasztás vagy onResume után). */
    fun reload() = load()

    fun selectDate(date: LocalDate) {
        if (date == selectedDate) return
        selectedDate = date // a kijelölés azonnal látszik
        load()
    }

    fun updateContent(text: String) {
        currentContent = text
        pending = selectedDate.format(fmt) to text
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500) // debounce: csak gépelés szünetében mentünk
            flush()
        }
    }

    /** Azonnali mentés (pl. az app háttérbe kerülésekor). */
    fun flushNow() {
        viewModelScope.launch { flush() }
    }

    private suspend fun flush() {
        val (date, text) = pending ?: return
        pending = null
        withContext(Dispatchers.IO + NonCancellable) {
            fileManager.saveNoteForDate(date, text)
            if (date == LocalDate.now().format(fmt)) {
                WidgetUpdater.push(getApplication(), text)
            }
        }
    }
}
