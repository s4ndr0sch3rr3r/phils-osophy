from pathlib import Path

# One-shot branch repair, removed after execution.
root = Path(__file__).resolve().parents[2]
path = root / "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt"
text = path.read_text(encoding="utf-8")

old = '''    if (showManageDialog) {
        BookManageDialog(
            book = book,
            onSave = { status, progress ->
                onSaveReadingState(status, progress)
                showManageDialog = false
            },
            onRemove = {
                onRemove()
                showManageDialog = false
            },
            onCancel = {
                showManageDialog = false
            }
        )
    }
}

@Composable
private fun ReadingProgressBar'''
new = '''    if (showManageDialog) {
        BookManageDialog(
            book = book,
            onSave = { status, progress ->
                onSaveReadingState(status, progress)
                showManageDialog = false
            },
            onRemove = {
                onRemove()
                showManageDialog = false
            },
            onCancel = {
                showManageDialog = false
            }
        )
    }

    if (showRatingDialog) {
        UserRatingDialog(
            title = "Rate ${book.title}",
            initialRating = book.userRating,
            onSave = { rating ->
                onChangeRating(rating)
                showRatingDialog = false
            },
            onCancel = { showRatingDialog = false }
        )
    }
}

@Composable
private fun ReadingProgressBar'''
if text.count(old) != 1:
    raise RuntimeError(f"BookDetailScreen close match count: {text.count(old)}")
text = text.replace(old, new, 1)

stray = '''

    if (showRatingDialog) {
        UserRatingDialog(
            title = "Rate ${book.title}",
            initialRating = book.userRating,
            onSave = { rating ->
                onChangeRating(rating)
                showRatingDialog = false
            },
            onCancel = { showRatingDialog = false }
        )
    }
}
'''
if text.count(stray) != 1:
    raise RuntimeError(f"Stray rating block match count: {text.count(stray)}")
text = text.replace(stray, "\n", 1)
path.write_text(text, encoding="utf-8")

(root / ".github/workflows/fix-book-rating-dialog.yml").unlink(missing_ok=True)
Path(__file__).unlink(missing_ok=True)
