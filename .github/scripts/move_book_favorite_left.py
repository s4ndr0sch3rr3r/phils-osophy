from pathlib import Path

root = Path(__file__).resolve().parents[2]
path = root / "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt"
text = path.read_text(encoding="utf-8")

function_start = text.index("private fun SavedBookCard(")
function_end = text.index("\n@Composable\nprivate fun BookSearchResult", function_start)
section = text[function_start:function_end]
old = ".align(Alignment.TopEnd)"
new = ".align(Alignment.TopStart)"

if section.count(old) != 1:
    raise RuntimeError("Expected exactly one top-end favorite alignment in SavedBookCard")

updated_section = section.replace(old, new, 1)
path.write_text(text[:function_start] + updated_section + text[function_end:], encoding="utf-8")
