package com.example.phils_osophy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MediaCommentDialog(
    mediaKey: Any,
    initialComment: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var comment by remember(mediaKey, initialComment) {
        mutableStateOf(initialComment)
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (initialComment.isBlank()) {
                    "Add comment"
                } else {
                    "Edit comment"
                }
            )
        },
        text = {
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Comment") },
                minLines = 3,
                maxLines = 6
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(comment.trim()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditableMediaCommentSection(
    mediaKey: Any,
    savedComment: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 24.dp
) {
    var comment by remember(mediaKey, savedComment) {
        mutableStateOf(savedComment)
    }
    val saveButtonBringIntoViewRequester = remember {
        BringIntoViewRequester()
    }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .padding(contentPadding)
            .imePadding()
    ) {
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            delay(300)
                            saveButtonBringIntoViewRequester.bringIntoView()
                        }
                    }
                },
            placeholder = { Text("Mes impressions") },
            minLines = 3,
            maxLines = 8
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val normalizedComment = comment.trim()
                comment = normalizedComment
                focusManager.clearFocus()
                onSave(normalizedComment)
            },
            modifier = Modifier.bringIntoViewRequester(
                saveButtonBringIntoViewRequester
            ),
            enabled = comment.trim() != savedComment
        ) {
            Text("Save")
        }
    }
}
