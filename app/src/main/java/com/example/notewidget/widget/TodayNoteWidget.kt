package com.example.notewidget.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.notewidget.data.NoteFileManager
import com.example.notewidget.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TodayNoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Mindig a MAI fÃ¡jlt olvassuk, Ã­gy Ã©jfÃ©l utÃ¡n sem a tegnapi marad
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val lines = withContext(Dispatchers.IO) {
            NoteFileManager(context).readNoteForDate(today)
        }.lines().filter { it.isNotBlank() }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    Text(
                        text = "Mai teendÅk",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    if (lines.isEmpty()) {
                        Text(
                            text = "MÃ©g nincsenek mÃ¡ra teendÅk.",
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
}
