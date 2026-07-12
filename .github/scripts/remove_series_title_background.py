from pathlib import Path

path = Path("app/src/main/java/com/example/phils_osophy/ui/screens/SeriesLibraryScreen.kt")
text = path.read_text(encoding="utf-8")

old_start = '''    Card(
        modifier = Modifier
            .width(104.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
'''
new_start = '''    Column(modifier = Modifier.width(104.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Box(
'''

old_end = '''            }

            MediaCardTitle(title = series.name)
        }
    }
}

@Composable
private fun LibrarySearchResult(
'''
new_end = '''            }
        }

        MediaCardTitle(title = series.name)
    }
}

@Composable
private fun LibrarySearchResult(
'''

if text.count(old_start) != 1:
    raise SystemExit(f"Expected one series card start, found {text.count(old_start)}")
if text.count(old_end) != 1:
    raise SystemExit(f"Expected one series card end, found {text.count(old_end)}")

text = text.replace(old_start, new_start, 1)
text = text.replace(old_end, new_end, 1)
path.write_text(text, encoding="utf-8")
