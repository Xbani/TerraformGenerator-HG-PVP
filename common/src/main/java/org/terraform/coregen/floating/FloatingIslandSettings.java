package org.terraform.coregen.floating;

import org.bukkit.configuration.file.YamlConfiguration;
import org.terraform.biome.BiomeBank;
import org.terraform.main.config.TConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** A profile is loaded once; generation never performs disk I/O. */
public final class FloatingIslandSettings {
    public final BiomeTexture texture;
    public final Map<Integer, BiomeBank> palette;
    public final int altitude, minThickness, maxThickness, bevelWidth, bevelDepth;
    public final double relief, treeChance;
    public final String dispatch;
    public final long dispatchSeed;

    public FloatingIslandSettings(File file) throws Exception {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.load(file);
        File imageFile = new File(cfg.getString("texture", "biomes.png"));
        if (!imageFile.isAbsolute()) imageFile = new File(file.getParentFile(), imageFile.getPath());
        BufferedImage image;
        try (var input = ImageIO.createImageInputStream(imageFile)) {
            if (input == null) throw new IOException("Cannot read texture: " + imageFile);
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Texture must be a readable PNG: " + imageFile);
            var reader = readers.next();
            try {
                reader.setInput(input);
                if (!"png".equalsIgnoreCase(reader.getFormatName())) throw new IOException("Use a lossless PNG texture");
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 16_777_216L) {
                    throw new IllegalArgumentException("Texture exceeds 16,777,216 pixels");
                }
                image = reader.read(0);
            } finally { reader.dispose(); }
        }
        texture = new BiomeTexture(image,
                cfg.getInt("center-x", TConfig.c.HEIGHT_MAP_SPAWN_CENTER_X),
                cfg.getInt("center-z", TConfig.c.HEIGHT_MAP_SPAWN_CENTER_Z));
        altitude = cfg.getInt("altitude", 100);
        minThickness = cfg.getInt("min-thickness", 4);
        maxThickness = cfg.getInt("max-thickness", 18);
        bevelWidth = cfg.getInt("bevel-width", 8);
        bevelDepth = cfg.getInt("bevel-depth", 3);
        relief = cfg.getDouble("relief", 2);
        treeChance = cfg.getDouble("tree-chance", 0.08);
        if (minThickness < 3 || maxThickness < minThickness || maxThickness > 128
                || bevelWidth < 1 || bevelDepth < 0 || bevelDepth > 32
                || !Double.isFinite(relief) || relief < 0 || relief > 32
                || !Double.isFinite(treeChance) || treeChance < 0 || treeChance > 1) {
            throw new IllegalArgumentException("Invalid floating island dimensions, relief or tree chance");
        }
        dispatch = cfg.getString("dispatch", "fixed").toLowerCase(Locale.ROOT);
        if (!dispatch.equals("fixed") && !dispatch.equals("seeded")) {
            throw new IllegalArgumentException("dispatch must be fixed or seeded");
        }
        dispatchSeed = cfg.getLong("dispatch-seed", 0);
        LinkedHashMap<Integer, BiomeBank> entries = new LinkedHashMap<>();
        var section = cfg.getConfigurationSection("palette");
        if (section == null) throw new IllegalArgumentException("Missing palette");
        for (String key : section.getKeys(false)) {
            if (!key.matches("#?[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Invalid RGB color: " + key);
            int color = Integer.parseInt(key.replace("#", ""), 16);
            if (color == 0) throw new IllegalArgumentException("Black is reserved for void");
            if (entries.put(color, BiomeBank.valueOf(section.getString(key).toUpperCase(Locale.ROOT))) != null) {
                throw new IllegalArgumentException("Duplicate RGB color: " + key);
            }
        }
        List<BiomeBank> allowed = cfg.getStringList("allowed-biomes").stream()
                .map(s -> BiomeBank.valueOf(s.toUpperCase(Locale.ROOT))).distinct().toList();
        if (dispatch.equals("fixed") && !allowed.isEmpty()
                && texture.usedColors().stream().anyMatch(color -> !allowed.contains(entries.get(color)))) {
            throw new IllegalArgumentException("Fixed palette contains a biome excluded by allowed-biomes");
        }
        for (int color : texture.usedColors()) if (!entries.containsKey(color)) {
            throw new IllegalArgumentException(String.format("Unknown texture color #%06X; disable antialiasing", color));
        }
        if (texture.usedColors().isEmpty()) throw new IllegalArgumentException("Texture contains no land");
        palette = Collections.unmodifiableMap(entries);
        allowedBiomes = allowed.isEmpty() ? entries.values().stream().distinct().toList() : allowed;
    }

    private final List<BiomeBank> allowedBiomes;

    public Map<Integer, BiomeBank> paletteFor(long worldSeed) {
        if (dispatch.equals("fixed")) return palette;
        return BiomeTexture.dispatch(texture.usedColors(), allowedBiomes, worldSeed ^ dispatchSeed);
    }
}
