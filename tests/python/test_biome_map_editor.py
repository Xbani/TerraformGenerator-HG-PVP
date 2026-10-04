import importlib.util
from pathlib import Path
import tempfile
import unittest

from PIL import Image
import yaml

SOURCE = Path(__file__).resolve().parents[2] / "tools/biome-map-editor/biome_map_editor.py"
spec = importlib.util.spec_from_file_location("biome_map_editor", SOURCE)
editor = importlib.util.module_from_spec(spec)
spec.loader.exec_module(editor)


class BiomeMapTests(unittest.TestCase):
    def test_determinism_and_palette(self):
        a = editor.generate(128, 96, editor.DEFAULT_PALETTE, seed=42, radius=12, spawn_radius=12)
        b = editor.generate(128, 96, editor.DEFAULT_PALETTE, seed=42, radius=12, spawn_radius=12)
        c = editor.generate(128, 96, editor.DEFAULT_PALETTE, seed=43, radius=12, spawn_radius=12)
        self.assertEqual(a.tobytes(), b.tobytes())
        self.assertNotEqual(a.tobytes(), c.tobytes())
        editor.validate(a, editor.DEFAULT_PALETTE)

    def test_spawn_disc(self):
        image = editor.generate(128, 96, editor.DEFAULT_PALETTE, islands=0, spawn_radius=36)
        for dz in range(-36, 37):
            for dx in range(-36, 37):
                if dx * dx + dz * dz <= 36 * 36:
                    self.assertNotEqual(image.getpixel((64 + dx, 48 + dz)), (0, 0, 0))
        self.assertEqual(image.getpixel((0, 0)), (0, 0, 0))

    def test_roundtrip_profile(self):
        image = editor.generate(64, 64, editor.DEFAULT_PALETTE, radius=8, spawn_radius=12)
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "biomes.png"
            cfg_path = editor.save_project(image, editor.DEFAULT_PALETTE, target, "seeded", -30, 45,
                                           {"altitude": 120, "dispatch-seed": 18})
            profile = yaml.safe_load(cfg_path.read_text())
            self.assertEqual(profile["palette"], editor.DEFAULT_PALETTE)
            self.assertEqual(profile["center-x"], -30)
            self.assertEqual(profile["center-z"], 45)
            self.assertEqual(profile["dispatch"], "seeded")
            self.assertEqual(profile["altitude"], 120)
            self.assertEqual(profile["dispatch-seed"], 18)
            with Image.open(target) as result:
                self.assertEqual(result.tobytes(), image.tobytes())

    def test_unknown_color_rejected(self):
        with self.assertRaisesRegex(ValueError, "Colors missing"):
            editor.validate(Image.new("RGB", (8, 8), "red"), editor.DEFAULT_PALETTE)

    def test_empty_map_rejected(self):
        with self.assertRaisesRegex(ValueError, "no land"):
            editor.validate(Image.new("RGB", (8, 8)), editor.DEFAULT_PALETTE)

    def test_alpha(self):
        image = Image.new("RGBA", (3, 3), (120, 184, 74, 255))
        image.putpixel((0, 0), (255, 0, 0, 0))
        editor.validate(image, editor.DEFAULT_PALETTE)
        self.assertEqual(editor.normalize(image).getpixel((0, 0)), (0, 0, 0))
        image.putpixel((0, 0), (120, 184, 74, 128))
        with self.assertRaisesRegex(ValueError, "Partial transparency"):
            editor.validate(image, editor.DEFAULT_PALETTE)

    def test_invalid_generation_parameters(self):
        for kwargs in [{"width": 0}, {"spawn_radius": 50}, {"radius": 0}, {"regions": 0}]:
            args = {"width": 64, "height": 64, "palette": editor.DEFAULT_PALETTE, "spawn_radius": 12}
            args.update(kwargs)
            with self.assertRaises(ValueError):
                editor.generate(**args)


if __name__ == "__main__":
    unittest.main()
