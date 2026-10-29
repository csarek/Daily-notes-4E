package com.example.notewidget.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.notewidget.R
import com.example.notewidget.ui.MainActivity

val NOTE_KEY = stringPreferencesKey("today_note")

class TodayNoteWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Induláskor / időzített frissítéskor a mai fájlból töltjük a widget állapotát
        val text = WidgetUpdater.readToday(context)
        updateAppWidgetState(context, id) { prefs -> prefs[NOTE_KEY] = text }

        val title = context.getString(R.string.widget_title)
        val emptyText = context.getString(R.string.widget_empty)
        val refreshText = context.getString(R.string.widget_refresh)

        provideContent {
            // A kirajzolás az állapotból olvas, így minden állapotváltozásra frissül
            val prefs = currentState<Preferences>()
            val lines = (prefs[NOTE_KEY] ?: "").lines().filter { it.isNotBlank() }

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = refreshText,
                            modifier = GlanceModifier
                                .padding(4.dp)
                                .clickable(actionRunCallback<RefreshAction>()),
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 12.sp
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    if (lines.isEmpty()) {
                        Text(
                            text = emptyText,
                            style = TextStyle(color = GlanceTheme.colors.onSurface)
                        )
                    } else {
                        LazyColumn {
                            items(lines.size) { i ->
                                Text(
                                    text = lines[i],
                                    style = TextStyle(color = GlanceTheme.colors.onSurface)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class TodayNoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayNoteWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshWorker.schedule(context)
    }
}
