from pathlib import Path
import re
import subprocess
import textwrap

SOURCE_COMMIT = "276d5ffdbf51dd341d30fb706b67bde1f6f160e8"
SOURCE_PATH = ".github/workflows/apply-recipe-image-search-v2.yml"

workflow_text = subprocess.run(
    ["git", "show", f"{SOURCE_COMMIT}:{SOURCE_PATH}"],
    check=True,
    capture_output=True,
    text=True
).stdout
workflow = workflow_text.splitlines()
step_index = workflow.index("      - name: Apply Recipe UI integration")
run_index = workflow.index("        run: |", step_index)
end_index = next(
    index for index in range(run_index + 1, len(workflow))
    if workflow[index].startswith("      - name:")
)
script = textwrap.dedent("\n".join(workflow[run_index + 1:end_index]))

section_replacement = '''section_pattern = re.compile(
    r'(?P<indent>[ \\t]+)onRecipeClick = onRecipeClick,\\n'
    r'(?P=indent)onFavoriteClick = onFavoriteClick\\n'
    r'(?P<close>[ \\t]+)\\)'
)
recipe, section_count = section_pattern.subn(
    lambda match: (
        f"{match.group('indent')}onRecipeClick = onRecipeClick,\\n"
        f"{match.group('indent')}onFavoriteClick = onFavoriteClick,\\n"
        f"{match.group('indent')}imageUrls = recipeImageUrls,\\n"
        f"{match.group('indent')}imageLookupSemaphore = imageLookupSemaphore\\n"
        f"{match.group('close')})"
    ),
    recipe
)
if section_count != 4:
    raise RuntimeError(f"Recipe section calls: expected four, found {section_count}")
'''
script, count = re.subn(
    r'''old_section_call = \(.*?recipe = recipe\.replace\(old_section_call, new_section_call\)\n''',
    section_replacement,
    script,
    count=1,
    flags=re.DOTALL
)
if count != 1:
    raise RuntimeError(f"Could not correct section insertion: {count}")

card_replacement = '''card_pattern = re.compile(
    r'(?P<indent>[ \\t]+)RecipeCard\\(\\n'
    r'(?P<arg>[ \\t]+)recipe = recipe,\\n'
)
recipe, card_count = card_pattern.subn(
    lambda match: (
        f"{match.group('indent')}RecipeCard(\\n"
        f"{match.group('arg')}recipe = recipe,\\n"
        f"{match.group('arg')}imageUrls = imageUrls,\\n"
        f"{match.group('arg')}imageLookupSemaphore = imageLookupSemaphore,\\n"
    ),
    recipe
)
if card_count != 2:
    raise RuntimeError(f"Recipe card calls: expected two, found {card_count}")
'''
script, count = re.subn(
    r'''card_call = \(.*?recipe = recipe\.replace\(card_call, card_call_updated\)\n''',
    card_replacement,
    script,
    count=1,
    flags=re.DOTALL
)
if count != 1:
    raise RuntimeError(f"Could not correct card insertion: {count}")

patch_script = Path("/tmp/apply_recipe_image_patch.py")
patch_script.write_text(script)
subprocess.run(["python", str(patch_script)], check=True)
