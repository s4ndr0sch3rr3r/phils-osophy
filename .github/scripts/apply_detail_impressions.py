from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


comment_components = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/MediaCommentComponents.kt"
replace_once(
    comment_components,
    "import androidx.compose.foundation.layout.height\nimport androidx.compose.foundation.layout.padding\n",
    "import androidx.compose.foundation.layout.height\nimport androidx.compose.foundation.layout.imePadding\nimport androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.relocation.BringIntoViewRequester\nimport androidx.compose.foundation.relocation.bringIntoViewRequester\n",
    "comment layout imports",
)
replace_once(
    comment_components,
    "import androidx.compose.runtime.remember\nimport androidx.compose.runtime.setValue\nimport androidx.compose.ui.Modifier\nimport androidx.compose.ui.text.font.FontWeight\n",
    "import androidx.compose.runtime.remember\nimport androidx.compose.runtime.rememberCoroutineScope\nimport androidx.compose.runtime.setValue\nimport androidx.compose.ui.Modifier\nimport androidx.compose.ui.focus.onFocusChanged\nimport androidx.compose.ui.platform.LocalFocusManager\nimport androidx.compose.ui.text.font.FontWeight\n",
    "comment focus imports",
)
replace_once(
    comment_components,
    "import androidx.compose.ui.unit.dp\n",
    "import androidx.compose.ui.unit.dp\nimport kotlinx.coroutines.delay\nimport kotlinx.coroutines.launch\n",
    "comment coroutine imports",
)
replace_once(
    comment_components,
    '''@Composable
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

    Column(modifier = modifier.padding(contentPadding)) {
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
''',
    '''@Composable
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
        Text(
            text = "Mes impressions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

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
            placeholder = { Text("...") },
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
''',
    "shared detail comment editor",
)

movie_detail = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/MovieDetailScreen.kt"
replace_once(
    movie_detail,
    "import androidx.compose.material3.Button\n",
    "",
    "movie button import",
)
replace_once(
    movie_detail,
    "import androidx.compose.material3.OutlinedTextField\n",
    "",
    "movie text field import",
)
replace_once(
    movie_detail,
    '''    var comment by remember(movie.id, movie.note) {
        mutableStateOf(movie.note)
    }

''',
    "",
    "movie local comment state",
)
replace_once(
    movie_detail,
    '''        Column(
            modifier = Modifier.padding(24.dp)
        ) {
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
                    val savedComment = comment.trim()
                    comment = savedComment
                    coroutineScope.launch {
                        PhilsOsophyDatabase
                            .getInstance(applicationContext)
                            .savedMovieDao()
                            .updateNote(
                                movieId = movie.id,
                                note = savedComment
                            )
                    }
                },
                enabled = comment.trim() != movie.note
            ) {
                Text("Save comment")
            }
        }
''',
    '''        EditableMediaCommentSection(
            mediaKey = movie.id,
            savedComment = movie.note,
            onSave = { savedComment ->
                coroutineScope.launch {
                    PhilsOsophyDatabase
                        .getInstance(applicationContext)
                        .savedMovieDao()
                        .updateNote(
                            movieId = movie.id,
                            note = savedComment
                        )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
''',
    "movie shared detail comment editor",
)
