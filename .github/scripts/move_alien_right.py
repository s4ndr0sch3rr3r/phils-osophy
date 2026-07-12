from pathlib import Path

root = Path(__file__).resolve().parents[2]

scaffold = root / "app/src/main/java/com/example/phils_osophy/ui/components/AppScaffold.kt"
text = scaffold.read_text(encoding="utf-8")
start = text.index("    val profileEndPadding = when (selectedCategory) {")
end = text.index("\n\n    Scaffold(", start)
text = text[:start] + text[end + 2:]
text = text.replace(".padding(top = 16.dp, end = profileEndPadding)", ".padding(top = 16.dp, end = 16.dp)")
scaffold.write_text(text, encoding="utf-8")

for relative in [
    "app/src/main/java/com/example/phils_osophy/ui/screens/MovieSearchScreen.kt",
    "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesLibraryScreen.kt",
    "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt",
]:
    path = root / relative
    text = path.read_text(encoding="utf-8")
    old = "        Row(\n            modifier = Modifier.fillMaxWidth(),\n            verticalAlignment = Alignment.CenterVertically\n        ) {"
    new = "        Row(\n            modifier = Modifier\n                .fillMaxWidth()\n                .padding(end = 66.dp),\n            verticalAlignment = Alignment.CenterVertically\n        ) {"
    if old not in text:
        raise RuntimeError(f"Header row not found in {relative}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
