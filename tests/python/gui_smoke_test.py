"""Run under xvfb-run on CI; exercises the actual Tk widgets and image editing."""
import importlib.util
from pathlib import Path
import tkinter as tk
from types import SimpleNamespace

path = Path(__file__).resolve().parents[2] / "tools/biome-map-editor/biome_map_editor.py"
spec = importlib.util.spec_from_file_location("biome_map_editor", path)
editor = importlib.util.module_from_spec(spec)
spec.loader.exec_module(editor)
root = tk.Tk()
app = editor.Editor(root)
root.update()
original = app.image.tobytes()
app.listbox.selection_clear(0, "end")
app.listbox.selection_set(3)
app.begin_stroke(SimpleNamespace(x=250, y=250), 1)
root.update()
assert app.image.tobytes() != original
app.undo()
assert app.image.tobytes() == original
app.redo()
assert app.image.tobytes() != original
app.change_zoom(1.25)
app.palette["#FF6633"] = "PLAINS"
app.refresh_palette()
app.listbox.selection_clear(0, "end")
app.listbox.selection_set(len(app.palette))
app.remove_color()
assert "#FF6633" not in app.palette
app.regenerate()
root.update()
assert app.image.size == (512, 512)
root.destroy()
print("Tk editor smoke test passed")
