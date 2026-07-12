from pathlib import Path

root = Path(__file__).resolve().parents[2]
path = root / "app/src/main/java/com/example/phils_osophy/ui/screens/MovieListScreen.kt"
text = path.read_text(encoding="utf-8")

function_start = text.index("private fun MoviePosterTile(")
card_start = text.index("        Card(\n", function_start)
title_start = text.index("\n\n        MediaCardTitle(title = movie.title)", card_start)
card_block = text[card_start:title_start]
lines = card_block.splitlines()

if len(lines) < 4:
    raise RuntimeError("Movie poster Card block was unexpectedly short")
if lines[0] != "        Card(":
    raise RuntimeError("Unexpected Movie poster Card opening")
if lines[1] != "            onClick = onClick,":
    raise RuntimeError("Unexpected Movie poster onClick line")
if lines[2] != "            modifier = Modifier":
    raise RuntimeError("Unexpected Movie poster modifier line")

formatted_lines = lines[:3] + [
    ("    " + line) if line else line
    for line in lines[3:]
]
formatted_block = "\n".join(formatted_lines)
path.write_text(
    text[:card_start] + formatted_block + text[title_start:],
    encoding="utf-8",
)
