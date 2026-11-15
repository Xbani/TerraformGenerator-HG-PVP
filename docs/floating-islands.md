# Texture-driven floating islands

This mode uses a lossless PNG as a finite world mask: **one pixel = one block**.
RGB palette colors select TerraformGenerator biomes. Black (`#000000`), fully
transparent pixels and everything outside the image are void. Partial alpha and
unknown colors cause a configuration error instead of inventing land.

## Setup

1. Run `tools/biome-map-editor/biome_map_editor.py` to draw or generate a map.
   A 512×512 starter texture is installed automatically on first use if you skip
   custom authoring.
2. Copy the exported PNG and YAML into `plugins/TerraformGenerator/`. Rename the
   YAML to `floating-islands.yml`; keep its `texture` pointing to the PNG.
3. In `config.yml`, select the mode:

   ```yaml
   generation:
     mode: floating-islands
   ```

   Or keep `generation.mode: normal` and select `TerraformGenerator:floating-islands`
   for one world using your world manager. With Bukkit's `WorldCreator`, call
   `.generator("TerraformGenerator:floating-islands")`.
4. Create a **new** world. Generation settings do not replace existing chunks.

A world-specific profile at `plugins/TerraformGenerator/floating-islands/<world>.yml`
overrides the default profile. Texture paths are relative to their YAML file.
Profiles and textures are loaded once on world creation; restart to change them.
Do not change a mask or dispatch seed midway through generating the same world.

The mode uses Bukkit's native generation and biome provider, avoiding the normal
NMS terrain injection and populators. Existing normal worlds retain their pipeline.

## Coordinates

Image columns map to Minecraft X, rows map to Minecraft Z (Y is elevation):

```text
worldX = centerX + pixelX - floor(width / 2)
worldZ = centerZ + pixelZ - floor(height / 2)
```

Pixel `(floor(width/2), floor(height/2))` maps exactly to `(centerX, centerZ)`, even
for odd sizes. A 1000×600 texture centered on `(100, -50)` covers X `[-400, 599]`
and Z `[-350, 249]`. No scaling, wraparound or interpolation of biome colors occurs.
Omit `center-x` and `center-z` to inherit `heightmap.spawn.center-x` and `.center-z`.
The editor exports them explicitly; update them if moving the map.

Terrain and palette decisions are exact per column. Minecraft's stored biome
resolution is coarser (4×4×4); native biome boundaries cannot represent every pixel.
Custom TerraformGenerator biomes use their handler's corresponding vanilla biome
for client colors, while their surface crust still comes from the custom handler.

## Profile

```yaml
texture: biomes.png
center-x: 0
center-z: 0
altitude: 100
relief: 2.0
min-thickness: 4
max-thickness: 18
bevel-width: 8
bevel-depth: 3
tree-chance: 0.08
dispatch: fixed
dispatch-seed: 0
allowed-biomes: [PLAINS, FOREST, DESERT, TAIGA]
palette:
  '#78B84A': PLAINS
  '#287A38': FOREST
  '#E6CB73': DESERT
  '#6BA5A0': TAIGA
```

| Setting | Effect |
| --- | --- |
| `altitude` | Nominal top Y, before relief, bevel and spawn shaping |
| `relief` | Maximum height variation in blocks; `0` gives a flat interior |
| `min-thickness` | Edge thickness, at least 3 blocks |
| `max-thickness` | Interior thickness cap, up to 128; independent of island size |
| `bevel-width` | Width of the rounded upper edge; lower taper spans twice this width |
| `bevel-depth` | Height drop at the outermost solid column |
| `tree-chance` | Probability per tree site (four sites/chunk), from 0 to 1 |
| `dispatch: fixed` | Exact color-to-biome assignment, independent of world seed |
| `dispatch: seeded` | Shuffle allowed biomes using `worldSeed XOR dispatch-seed` |
| `allowed-biomes` | Biome whitelist; omitted/empty uses unique palette biomes |

`seeded` dispatch changes the meaning of colors, preserving all island outlines
and colored regions. A color has one meaning throughout a world. Assignments
remain identical after reload and regardless of chunk/thread order. Change the
world seed or `dispatch-seed` for a different dispatch on a new world. Biomes
cycle through sorted used color slots; fewer colors than allowed biomes cannot
represent the whole list. In fixed mode, used palette entries must be in the whitelist.

The top has smooth low-amplitude relief; the bottom tapers without becoming a long
cone. There are no bedrock floors, seas, caves, lakes, canyons or generated structures
in this mode. Ocean/river biome labels produce dry floating land with their crust.
Non-solid crust materials are replaced with stone to keep a supported surface.
Trees are small, sparse vanilla trees on suitable grass surfaces, independent of
the normal biome handlers' dense forests. Crowns stay within their generation cell
and never occupy a void column. No animals are populated during chunk generation;
normal gameplay mob spawning remains controlled by server rules.

## Existing spawn feature

The existing `heightmap.spawn` settings apply to this mode:

- `simple`: flat core at the profile's `altitude`, using `spawn-flat-radius`.
- `advanced`: the existing center-Y → edge-Y curve and outer blend distance.
- `none`: the normal low-relief floating surface.

The configured spawn center remains independent of the texture center when you
set an explicit map center. The enabled spawn core must be painted entirely as
land. Intersections with black/outside pixels reject the profile; the generator
never silently fills void. An advanced spawn needs `max-thickness >= 6` to preserve
its support volume. The solid floating volume already prevents cave erosion, so
`surface-repair.enabled` does not need a post-generation repair pass.

The fixed player spawn is centered above the configured spawn column (`topY + 1`).
If no spawn core is enabled and that column is void, the nearest land column is
used deterministically. Trees are excluded around it and inside an enabled spawn
core. Altitude and spawn heights must fit inside the world's build limits.

## Verification

```bash
mkdir -p /tmp/floating-island-tests
java com.sun.tools.javac.Main -d /tmp/floating-island-tests \
  common/src/main/java/org/terraform/coregen/floating/BiomeTexture.java \
  tests/java/BiomeTextureTest.java
java -cp /tmp/floating-island-tests BiomeTextureTest
python -m unittest discover -s tests/python -v
./gradlew :common:compileJava
```

Before using the mode on your beta server, generate a fresh test world and inspect
an island edge, a black hole, the map boundary and the configured advanced spawn.
Check that blocks below the islands and outside the map remain air, and that
reloading the world does not change biome dispatch. The standalone tests cover the
mask/geometry/dispatch and Python generation/export; they do not replace a Paper
server integration test.
