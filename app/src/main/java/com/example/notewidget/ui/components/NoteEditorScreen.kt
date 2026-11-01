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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.notewidget.R

/**
 * Minden sor "- "-vel kezdődik:
 * - az első karakter begépelésekor az üres mezőben automatikusan megjelenik a "- ",
 * - Enter után az új sor is "- "-vel indul,
 * - ha az előző sor üres pont volt ("- "), az Enter kilép a listából (a gondolatjel törlődik).
 */
fun handleBulletPointInput(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    // 1) Üres mezőbe az első karakter: tegyünk elé gondolatjelet
    if (old.text.isEmpty() && new.text.isNotEmpty() && !new.text.startsWith("-")) {
        return TextFieldValue("- " + new.text, TextRange(new.selection.start + 2))
    }

    // 2) Enter lenyomása
    val cursor = new.selection.start
    val typedEnter = new.text.length == old.text.length + 1 &&
        new.selection.collapsed &&
        cursor > 0 &&
        new.text[cursor - 1] == '\n'
    if (!typedEnter) return new

    val lineStart = new.text.lastIndexOf('\n', cursor - 2) + 1
    val prevLine = new.text.substring(lineStart, cursor - 1)

    return if (prevLine.trim() == "-") {
        // üres pont: kilépés a listából
        TextFieldValue(new.text.removeRange(lineStart, cursor), TextRange(lineStart))
    } else {
        TextFieldValue(
            new.text.substring(0, cursor) + "- " + new.text.substring(cursor),
            TextRange(cursor + 2)
        )
    }
}

@Composable
fun NoteEditorScreen(
    content: String,
    onContentChanged: (String) -> Unit,
    readOnly: Boolean = false
) {
    var value by remember { mutableStateOf(TextFieldValue(content)) }

    // Ha kívülről változik a tartalom (másik nap), frissítjük a mezőt
    LaunchedEffect(content) {
        if (value.text != content) {
            value = TextFieldValue(content, TextRange(content.length))
        }
    }

    OutlinedTextField(
        value = value,
        readOnly = readOnly,
        onValueChange = { new ->
            val processed = handleBulletPointInput(value, new)
            value = processed
            onContentChanged(processed.text)
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.note_placeholder)) }
    )
}
