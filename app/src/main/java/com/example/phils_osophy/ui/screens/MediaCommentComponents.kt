package com.example.phils_osophy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    modifier: Modifier = Modifier
) {
    var comment by remember(mediaKey, savedComment) {
        mutableStateOf(savedComment)
    }

    Column(modifier = modifier.padding(24.dp)) {
        Text(
            text = "Your comment",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Comment") },
            minLines = 3,
            maxLines = 8
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val normalizedComment = comment.trim()
                comment = normalizedComment
                onSave(normalizedComment)
            },
            enabled = comment.trim() != savedComment
        ) {
            Text("Save comment")
        }
    }
}
