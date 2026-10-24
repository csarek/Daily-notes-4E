package com.example.notewidget.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Ha Entert nyomtak egy "- " kezdetÅ± sor vÃ©gÃ©n, az Ãºj sor is "- "-vel indul.
 * Ha az elÅzÅ sor Ã¼res pont volt ("- "), akkor kilÃ©p a listÃ¡bÃ³l.
 */
fun handleBulletPointInput(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val cursor = new.selection.start
    val typedEnter = new.text.length == old.text.length + 1 &&
        new.selection.collapsed &&
        cursor > 0 &&
        new.text[cursor - 1] == '\n'
    if (!typedEnter) return new

    val lineStart = new.text.lastIndexOf('\n', cursor - 2) + 1
    val prevLine = new.text.substring(lineStart, cursor - 1)

    return when {
        prevLine.trim() == "-" ->
            TextFieldValue(new.text.removeRange(lineStart, cursor), TextRange(lineStart))
        prevLine.startsWith("- ") ->
            TextFieldValue(
                new.text.substring(0, cursor) + "- " + new.text.substring(cursor),
                TextRange(cursor + 2)
            )
        else -> new
    }
}

@Composable
fun NoteEditorScreen(content: String, onContentChanged: (String) -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue(content)) }

    // Ha kÃ­vÃ¼lrÅl vÃ¡ltozik a tartalom (mÃ¡sik nap), frissÃ­tjÃ¼k a mezÅt
    LaunchedEffect(content) {
        if (value.text != content) {
            value = TextFieldValue(content, TextRange(content.length))
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            val processed = handleBulletPointInput(value, new)
            value = processed
            onContentChanged(processed.text)
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        placeholder = { Text("Ãrd ide a teendÅket...") }
    )
}
