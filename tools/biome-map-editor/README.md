# Biome map editor

Requires Python 3.10+, Pillow, PyYAML and Tkinter (included in the standard Windows
Python installer; on Linux use the distribution's `python3-tk` package).

```bash
python -m pip install -r tools/biome-map-editor/requirements.txt
python tools/biome-map-editor/biome_map_editor.py
```

Generate organic island masks, distribute biome regions, then paint exact RGB
colors. Select a biome color and drag the left mouse button; right-drag erases to
void. Brush size is in blocks. Ctrl+wheel zooms, middle-drag pans; Ctrl+Z/Ctrl+Y
undo/redo. The white cross marks the texture center, and the status line shows
Minecraft X/Z coordinates. Scroll the settings panel if controls extend below the
window. Palette colors can be added or removed; colors still painted cannot be
removed until erased or repainted. Choose biome names from `biomes.json`.

Export saves two files, `<name>.png` and `<name>.yml`, including the palette, selected
biomes, center and fixed/seeded dispatch. Rename the YAML to `floating-islands.yml`
when installing it. Reopening YAML restores the image and terrain profile settings.
Advanced terrain settings such as altitude, bevel and depth are editable in that
YAML and preserved when reopening and exporting. The procedural texture seed and
the server's dispatch seed are separate: the first changes the map, the second
changes which biome each color represents in seeded mode.

Without opening a GUI:

```bash
python tools/biome-map-editor/biome_map_editor.py \
  --generate biomes.png --width 1000 --height 600 --seed 42 \
  --islands 18 --radius 45 --spawn-radius 36 --regions 24 \
  --center-x 100 --center-z -50 --dispatch seeded

python tools/biome-map-editor/biome_map_editor.py \
  --validate biomes.png --palette biomes.yml
```

`--palette profile.yml` also selects which biomes/colors are used for generation.
Procedural regions are computed on an 8-block grid; manual painting and the
exported mask remain exact at one pixel per block. A central land disc with a bevel
margin is reserved whenever `--spawn-radius` is positive. `0` removes that guarantee.
To avoid intersecting a moved spawn, paint its core disc as land in the editor.

Textures support up to 16,777,216 pixels. Do not use antialiasing, JPEG compression,
partial transparency or colors outside the palette. Undo history and zoom sizes
are bounded to limit memory use on large maps.
