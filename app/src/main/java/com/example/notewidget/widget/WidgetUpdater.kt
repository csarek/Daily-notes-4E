package com.example.notewidget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.notewidget.data.NoteFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object WidgetUpdater {

    /** A mai nap jegyzetének beolvasása a fájlból. */
    suspend fun readToday(context: Context): String = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        NoteFileManager(context).readNoteForDate(today)
    }

    /** A szöveget beírja minden widget állapotába, és újrarajzoltatja őket. */
    suspend fun push(context: Context, text: String) {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(TodayNoteWidget::class.java)
        ids.forEach { id ->
            updateAppWidgetState(context, id) { prefs -> prefs[NOTE_KEY] = text }
            TodayNoteWidget().update(context, id)
        }
    }

    suspend fun refreshFromFile(context: Context) {
        push(context, readToday(context))
    }
}

/** A widgeten lévő "Frissítés" gomb. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WidgetUpdater.refreshFromFile(context)
    }
}

/** Háttérfeladat: ~15 percenként frissíti a widgetet (éjfél után a új napra vált). */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        WidgetUpdater.refreshFromFile(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "widget_refresh",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
