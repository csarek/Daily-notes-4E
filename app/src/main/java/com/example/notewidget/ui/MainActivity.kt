package com.example.notewidget.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.notewidget.data.NoteFileManager
import com.example.notewidget.ui.components.CompactWeekCalendar
import com.example.notewidget.ui.components.NoteEditorScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val fileManager = remember { NoteFileManager(context) }
                    var hasRoot by remember { mutableStateOf(fileManager.hasValidRoot()) }

                    if (!hasRoot) {
                        FolderSelectorScreen(fileManager) {
                            hasRoot = true
                            viewModel.reload()
                        }
                    } else {
                        MainScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ha kÃ¶zben mÃ¡s app mÃ³dosÃ­totta a fÃ¡jlt
        viewModel.reload()
    }

    override fun onStop() {
        super.onStop()
        viewModel.flushNow()
    }
}

@Composable
fun FolderSelectorScreen(fileManager: NoteFileManager, onFolderSelected: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            fileManager.saveRootUri(it)
            onFolderSelected()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "VÃ¡lassz egy mappÃ¡t a teendÅket tartalmazÃ³ txt fÃ¡jlokhoz:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { launcher.launch(null) }) {
            Text("Mappa kivÃ¡lasztÃ¡sa")
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        CompactWeekCalendar(
            selectedDate = viewModel.selectedDate,
            onDateSelected = { viewModel.selectDate(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "DÃ¡tum: ${viewModel.selectedDate}",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            NoteEditorScreen(
                content = viewModel.currentContent,
                onContentChanged = { viewModel.updateContent(it) }
            )
        }
    }
}
