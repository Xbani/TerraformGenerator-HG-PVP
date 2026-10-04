#!/usr/bin/env python3
"""Pixel-exact floating island texture generator and Tk editor. CLI works without a display."""
from __future__ import annotations

import argparse
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw
import yaml

DEFAULT_PALETTE = {"#78B84A": "PLAINS", "#287A38": "FOREST", "#E6CB73": "DESERT", "#6BA5A0": "TAIGA"}
MAX_PIXELS = 16_777_216


def rgb(color: str) -> tuple[int, int, int]:
    color = color.lstrip("#")
    if len(color) != 6:
        raise ValueError("Use six hexadecimal RGB digits")
    return tuple(int(color[i:i + 2], 16) for i in (0, 2, 4))


def validate(image: Image.Image, palette: dict[str, str]) -> None:
    if image.width * image.height > MAX_PIXELS:
        raise ValueError(f"Texture exceeds {MAX_PIXELS:,} pixels")
    allowed = {rgb(color) for color in palette} | {(0, 0, 0)}
    unknown = set()
    land = False
    rgba = image.convert("RGBA")
    pixels = rgba.get_flattened_data() if hasattr(rgba, "get_flattened_data") else rgba.getdata()
    for r, g, b, a in pixels:
        if a not in (0, 255):
            raise ValueError("Partial transparency is not supported; disable antialiasing")
        if a == 0:
            continue
        if (r, g, b) not in allowed:
            unknown.add(f"#{r:02X}{g:02X}{b:02X}")
        land |= (r, g, b) != (0, 0, 0)
    if unknown:
        raise ValueError("Colors missing from palette: " + ", ".join(sorted(unknown)[:12]))
    if not land:
        raise ValueError("Texture contains no land")


def normalize(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    result = Image.new("RGB", rgba.size, "black")
    result.paste(rgba.convert("RGB"), mask=rgba.getchannel("A"))
    return result


def generate(width: int, height: int, palette: dict[str, str], seed: int = 0,
             islands: int = 12, radius: int = 45, spawn_radius: int = 36,
             regions: int = 16) -> Image.Image:
    if width < 1 or height < 1 or width * height > MAX_PIXELS:
        raise ValueError("Invalid texture dimensions")
    if not palette or "#000000" in palette or any(rgb(c) == (0, 0, 0) for c in palette):
        raise ValueError("Choose at least one non-black biome color")
    if radius < 1 or spawn_radius < 0 or spawn_radius * 2 + 1 > min(width, height):
        raise ValueError("Spawn disc must fit inside the texture; island radius must be positive")
    if islands < 0 or regions < 1:
        raise ValueError("Invalid island or region count")
    rng = random.Random(seed)
    mask = Image.new("L", (width, height))
    draw = ImageDraw.Draw(mask)
    for _ in range(islands):
        cx, cz = rng.randrange(width), rng.randrange(height)
        rx, rz = radius * rng.uniform(0.65, 1.35), radius * rng.uniform(0.65, 1.35)
        phases = [rng.random() * math.tau for _ in range(3)]
        points = []
        for step in range(96):
            angle = step * math.tau / 96
            wave = 1 + 0.10 * math.sin(3 * angle + phases[0]) + 0.08 * math.sin(5 * angle + phases[1])
            points.append((round(cx + math.cos(angle) * rx * wave), round(cz + math.sin(angle) * rz * wave)))
        draw.polygon(points, fill=255)
    # Central island includes a margin for bevel and the default spawn core.
    if spawn_radius:
        margin = min(16, max(0, (min(width, height) - 1) // 2 - spawn_radius))
        r = spawn_radius + margin
        draw.ellipse((width // 2 - r, height // 2 - r, width // 2 + r, height // 2 + r), fill=255)
    colors = [rgb(c) for c in palette]
    sites = [(rng.randrange(width), rng.randrange(height), colors[i % len(colors)]) for i in range(regions)]
    rng.shuffle(sites)
    # Compute region dispatch on an 8-block grid; edges remain pixel-exact. This bounds
    # procedural generation cost while manual painting retains full block resolution.
    result = Image.new("RGB", (width, height), "black")
    rd = ImageDraw.Draw(result)
    for z in range(0, height, 8):
        for x in range(0, width, 8):
            site = min(sites, key=lambda s: (s[0] - x - 4) ** 2 + (s[1] - z - 4) ** 2)
            rd.rectangle((x, z, min(x + 7, width - 1), min(z + 7, height - 1)), fill=site[2])
    black = Image.new("RGB", result.size, "black")
    return Image.composite(result, black, mask)


def save_project(image: Image.Image, palette: dict[str, str], target: Path,
                 dispatch: str = "fixed", center_x: int = 0, center_z: int = 0,
                 profile: dict | None = None) -> Path:
    validate(image, palette)
    target = target.with_suffix(".png")
    target.parent.mkdir(parents=True, exist_ok=True)
    normalize(image).save(target)
    cfg = dict(profile or {})
    cfg.update({"texture": target.name, "center-x": center_x, "center-z": center_z,
                "dispatch": dispatch, "allowed-biomes": list(dict.fromkeys(palette.values())), "palette": palette})
    for key, value in {"altitude": 100, "relief": 2.0, "min-thickness": 4, "max-thickness": 18,
                       "bevel-width": 8, "bevel-depth": 3, "tree-chance": 0.08, "dispatch-seed": 0}.items():
        cfg.setdefault(key, value)
    config_path = target.with_suffix(".yml")
    config_path.write_text(yaml.safe_dump(cfg, sort_keys=False), encoding="utf-8")
    return config_path


class Editor:
    def __init__(self, root, image: Image.Image | None = None, profile: dict | None = None):
        import tkinter as tk
        from tkinter import ttk
        self.tk, self.ttk, self.root = tk, ttk, root
        root.title("TerraformGenerator — Biome Texture Editor")
        root.geometry("1250x850")
        self.profile = dict(profile or {})
        self.palette = dict(self.profile.get("palette", DEFAULT_PALETTE))
        self.image = normalize(image) if image else generate(512, 512, self.palette)
        self.undo_stack, self.redo_stack = [], []
        self.zoom, self.last = 1.0, None
        panel = ttk.Frame(root)
        panel.pack(side="left", fill="y")
        panel_canvas = tk.Canvas(panel, width=285, highlightthickness=0)
        panel_scroll = ttk.Scrollbar(panel, command=panel_canvas.yview)
        panel_canvas.configure(yscrollcommand=panel_scroll.set)
        panel_scroll.pack(side="right", fill="y")
        panel_canvas.pack(side="left", fill="y")
        sidebar = ttk.Frame(panel_canvas, padding=12)
        panel_canvas.create_window(0, 0, anchor="nw", window=sidebar)
        sidebar.bind("<Configure>", lambda e: panel_canvas.configure(scrollregion=panel_canvas.bbox("all")))
        self.width = self.field(sidebar, "Width (blocks)", self.image.width)
        self.height = self.field(sidebar, "Height (blocks)", self.image.height)
        self.seed = self.field(sidebar, "Texture seed", 0)
        self.count = self.field(sidebar, "Island count", 12)
        self.radius = self.field(sidebar, "Island radius", 45)
        self.spawn_radius = self.field(sidebar, "Spawn disc radius", 36)
        self.regions = self.field(sidebar, "Biome region count", 16)
        self.center_x = self.field(sidebar, "World center X", self.profile.get("center-x", 0))
        self.center_z = self.field(sidebar, "World center Z", self.profile.get("center-z", 0))
        ttk.Label(sidebar, text="Biome dispatch").pack(anchor="w")
        self.dispatch = tk.StringVar(value=self.profile.get("dispatch", "fixed"))
        ttk.Combobox(sidebar, textvariable=self.dispatch, values=("fixed", "seeded"), state="readonly", width=22).pack(fill="x")
        ttk.Button(sidebar, text="Generate islands", command=lambda: self.guard(self.regenerate)).pack(fill="x", pady=8)
        self.listbox = tk.Listbox(sidebar, height=7, exportselection=False, width=30)
        self.listbox.pack(fill="x")
        self.refresh_palette()
        biome_file = Path(__file__).with_name("biomes.json")
        biome_names = json.loads(biome_file.read_text()) if biome_file.exists() else list(DEFAULT_PALETTE.values())
        self.biome_name = tk.StringVar(value="PLAINS")
        ttk.Combobox(sidebar, textvariable=self.biome_name, values=biome_names, width=25).pack(fill="x", pady=4)
        ttk.Button(sidebar, text="Add biome / choose color", command=lambda: self.guard(self.add_color)).pack(fill="x")
        ttk.Button(sidebar, text="Remove selected color", command=lambda: self.guard(self.remove_color)).pack(fill="x")
        self.brush = self.field(sidebar, "Brush radius (blocks)", 8)
        ttk.Label(sidebar, text="Left: paint • Right: erase\nCtrl+wheel: zoom • Middle: pan\nCtrl+Z / Ctrl+Y: undo / redo\nRows correspond to world Z").pack(anchor="w", pady=8)
        for title, action in [("Open PNG / YAML", self.open), ("Save PNG + YAML", self.save),
                              ("Undo", self.undo), ("Redo", self.redo)]:
            ttk.Button(sidebar, text=title, command=lambda a=action: self.guard(a)).pack(fill="x")
        view = ttk.Frame(root)
        view.pack(side="right", fill="both", expand=True)
        self.status = tk.StringVar()
        ttk.Label(view, textvariable=self.status).pack(side="bottom", fill="x")
        self.canvas = tk.Canvas(view, background="#20232B", highlightthickness=0)
        sx, sy = ttk.Scrollbar(view, orient="horizontal", command=self.canvas.xview), ttk.Scrollbar(view, command=self.canvas.yview)
        self.canvas.configure(xscrollcommand=sx.set, yscrollcommand=sy.set)
        sx.pack(side="bottom", fill="x")
        sy.pack(side="right", fill="y")
        self.canvas.pack(fill="both", expand=True)
        for button in (1, 3):
            self.canvas.bind(f"<ButtonPress-{button}>", lambda e, b=button: self.begin_stroke(e, b))
            self.canvas.bind(f"<B{button}-Motion>", lambda e, b=button: self.paint(e, b))
            self.canvas.bind(f"<ButtonRelease-{button}>", lambda e: setattr(self, "last", None))
        self.canvas.bind("<Motion>", self.coordinates)
        self.canvas.bind("<ButtonPress-2>", lambda e: self.canvas.scan_mark(e.x, e.y))
        self.canvas.bind("<B2-Motion>", lambda e: self.canvas.scan_dragto(e.x, e.y, gain=1))
        self.canvas.bind("<Control-MouseWheel>", lambda e: self.change_zoom(1.25 if e.delta > 0 else 0.8))
        self.canvas.bind("<Control-Button-4>", lambda e: self.change_zoom(1.25))
        self.canvas.bind("<Control-Button-5>", lambda e: self.change_zoom(0.8))
        root.bind("<Control-z>", lambda e: self.undo())
        root.bind("<Control-y>", lambda e: self.redo())
        self.render()

    def field(self, parent, label, value):
        self.ttk.Label(parent, text=label).pack(anchor="w")
        var = self.tk.StringVar(value=str(value))
        self.ttk.Entry(parent, textvariable=var, width=26).pack(fill="x")
        return var

    def guard(self, action):
        from tkinter import messagebox
        try:
            action()
        except (ValueError, OSError, yaml.YAMLError) as exc:
            messagebox.showerror("Biome texture", str(exc))

    def snapshot(self):
        # Keep memory bounded even for large maps.
        budget = max(1, min(20, 64_000_000 // (self.image.width * self.image.height * 3)))
        self.undo_stack.append((self.image.copy(), dict(self.palette)))
        self.undo_stack = self.undo_stack[-budget:]
        self.redo_stack.clear()

    def refresh_palette(self):
        self.listbox.delete(0, "end")
        self.listbox.insert("end", "#000000  VOID")
        for color, name in self.palette.items():
            self.listbox.insert("end", f"{color}  {name}")
        self.listbox.selection_set(1 if self.palette else 0)

    def add_color(self):
        from tkinter import colorchooser
        name = self.biome_name.get().strip().upper()
        available = json.loads(Path(__file__).with_name("biomes.json").read_text())
        if name not in available:
            raise ValueError("Choose a TerraformGenerator biome from the list")
        color = colorchooser.askcolor(parent=self.root)[1]
        if not color:
            return
        color = color.upper()
        if rgb(color) == (0, 0, 0):
            raise ValueError("Black is reserved for void")
        self.snapshot()
        self.palette[color] = name
        self.refresh_palette()

    def remove_color(self):
        from tkinter import messagebox
        selection = self.listbox.curselection()
        if not selection or selection[0] == 0:
            return
        color = list(self.palette)[selection[0] - 1]
        if rgb(color) in {p for _, p in self.image.getcolors(MAX_PIXELS) or []}:
            messagebox.showinfo("Color in use", "Erase or repaint this color before removing it.")
            return
        self.snapshot()
        del self.palette[color]
        self.refresh_palette()

    def pixel(self, event):
        return (math.floor(self.canvas.canvasx(event.x) / self.zoom),
                math.floor(self.canvas.canvasy(event.y) / self.zoom))

    def begin_stroke(self, event, button):
        self.snapshot()
        self.last = None
        self.guard(lambda: self.paint(event, button))

    def paint(self, event, button):
        p = self.pixel(event)
        selection = self.listbox.curselection()
        color = "#000000" if button == 3 or not selection or selection[0] == 0 else list(self.palette)[selection[0] - 1]
        radius = max(0, min(1024, int(self.brush.get())))
        draw = ImageDraw.Draw(self.image)
        if self.last:
            draw.line((self.last, p), fill=rgb(color), width=2 * radius + 1)
        draw.ellipse((p[0] - radius, p[1] - radius, p[0] + radius, p[1] + radius), fill=rgb(color))
        self.last = p
        self.render()
        self.coordinates(event)

    def coordinates(self, event):
        x, z = self.pixel(event)
        try:
            wx, wz = int(self.center_x.get()) + x - self.image.width // 2, int(self.center_z.get()) + z - self.image.height // 2
            color = self.image.getpixel((x, z)) if 0 <= x < self.image.width and 0 <= z < self.image.height else (0, 0, 0)
            key = "#%02X%02X%02X" % color
            self.status.set(f"Pixel {x}, {z}  |  World X/Z {wx}, {wz}  |  {self.palette.get(key, 'VOID')}  |  Zoom {self.zoom:.2f}")
        except ValueError:
            pass

    def render(self):
        from PIL import ImageTk
        size = (max(1, round(self.image.width * self.zoom)), max(1, round(self.image.height * self.zoom)))
        self.photo = ImageTk.PhotoImage(self.image.resize(size, Image.Resampling.NEAREST))
        self.canvas.delete("all")
        self.canvas.create_image(0, 0, anchor="nw", image=self.photo)
        cx, cz = self.image.width // 2 * self.zoom, self.image.height // 2 * self.zoom
        self.canvas.create_line(cx - 6, cz, cx + 6, cz, fill="white")
        self.canvas.create_line(cx, cz - 6, cx, cz + 6, fill="white")
        self.canvas.configure(scrollregion=(0, 0, *size))

    def change_zoom(self, factor):
        proposed = max(0.125, min(8, self.zoom * factor))
        if self.image.width * self.image.height * proposed * proposed > 32_000_000:
            return
        self.zoom = proposed
        self.render()

    def regenerate(self):
        image = generate(int(self.width.get()), int(self.height.get()), self.palette,
                         int(self.seed.get()), int(self.count.get()), int(self.radius.get()),
                         int(self.spawn_radius.get()), int(self.regions.get()))
        self.snapshot()
        self.image = image
        self.render()

    def undo(self):
        if not self.undo_stack:
            return
        self.redo_stack.append((self.image.copy(), dict(self.palette)))
        self.image, self.palette = self.undo_stack.pop()
        self.refresh_palette()
        self.render()

    def redo(self):
        if not self.redo_stack:
            return
        self.undo_stack.append((self.image.copy(), dict(self.palette)))
        self.image, self.palette = self.redo_stack.pop()
        self.refresh_palette()
        self.render()

    def open(self):
        from tkinter import filedialog
        filename = filedialog.askopenfilename(filetypes=[("Texture / profile", "*.png *.yml *.yaml")])
        if not filename:
            return
        path = Path(filename)
        cfg_path = path if path.suffix in (".yml", ".yaml") else path.with_suffix(".yml")
        cfg = yaml.safe_load(cfg_path.read_text(encoding="utf-8")) if cfg_path.exists() else {}
        if path.suffix != ".png":
            path = cfg_path.parent / cfg["texture"]
        with Image.open(path) as source:
            image = source.convert("RGBA")
        palette = cfg.get("palette", self.palette)
        validate(image, palette)
        self.snapshot()
        self.image, self.palette, self.profile = normalize(image), dict(palette), dict(cfg)
        self.width.set(self.image.width)
        self.height.set(self.image.height)
        self.center_x.set(cfg.get("center-x", 0))
        self.center_z.set(cfg.get("center-z", 0))
        self.dispatch.set(cfg.get("dispatch", "fixed"))
        self.refresh_palette()
        self.render()

    def save(self):
        from tkinter import filedialog, messagebox
        filename = filedialog.asksaveasfilename(defaultextension=".png", filetypes=[("PNG texture", "*.png")])
        if filename:
            cfg_path = save_project(self.image, self.palette, Path(filename), self.dispatch.get(),
                                    int(self.center_x.get()), int(self.center_z.get()), self.profile)
            messagebox.showinfo("Saved", f"Texture and profile saved.\n{cfg_path}\nCopy the profile as floating-islands.yml beside its PNG.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--generate", type=Path, help="Generate PNG + YAML without opening the editor")
    parser.add_argument("--width", type=int, default=512)
    parser.add_argument("--height", type=int, default=512)
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument("--islands", type=int, default=12)
    parser.add_argument("--radius", type=int, default=45)
    parser.add_argument("--spawn-radius", type=int, default=36)
    parser.add_argument("--regions", type=int, default=16)
    parser.add_argument("--center-x", type=int, default=0)
    parser.add_argument("--center-z", type=int, default=0)
    parser.add_argument("--dispatch", choices=("fixed", "seeded"), default="fixed")
    parser.add_argument("--palette", type=Path, help="YAML profile containing a palette")
    parser.add_argument("--validate", type=Path, help="Validate a PNG against --palette or the default palette")
    args = parser.parse_args()
    profile = yaml.safe_load(args.palette.read_text(encoding="utf-8")) if args.palette else {}
    palette = profile.get("palette", DEFAULT_PALETTE)
    try:
        if args.validate:
            with Image.open(args.validate) as image:
                validate(image, palette)
            print("Texture valid")
        elif args.generate:
            image = generate(args.width, args.height, palette, args.seed, args.islands,
                             args.radius, args.spawn_radius, args.regions)
            config = save_project(image, palette, args.generate, args.dispatch, args.center_x, args.center_z, profile)
            print(f"Saved {args.generate.with_suffix('.png')} and {config}")
        else:
            import tkinter as tk
            root = tk.Tk()
            Editor(root, profile=profile)
            root.mainloop()
    except (ValueError, OSError, yaml.YAMLError) as exc:
        parser.error(str(exc))


if __name__ == "__main__":
    main()
