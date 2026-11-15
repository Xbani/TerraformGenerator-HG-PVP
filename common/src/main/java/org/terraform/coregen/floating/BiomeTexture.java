package org.terraform.coregen.floating;

import java.awt.image.BufferedImage;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Immutable one-pixel-per-block mask. Black and fully transparent pixels are void. */
public final class BiomeTexture {
    private final int width, height, centerX, centerZ;
    private final int[] colors;
    private final int[] distance;

    public BiomeTexture(BufferedImage image, int centerX, int centerZ) {
        width = image.getWidth();
        height = image.getHeight();
        if ((long) width * height > 16_777_216L) {
            throw new IllegalArgumentException("Texture exceeds 16,777,216 pixels");
        }
        if (Math.abs((long) centerX) + width > 30_000_000L
                || Math.abs((long) centerZ) + height > 30_000_000L) {
            throw new IllegalArgumentException("Texture must fit inside Minecraft world coordinates");
        }
        this.centerX = centerX;
        this.centerZ = centerZ;
        colors = image.getRGB(0, 0, width, height, null, 0, width);
        distance = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            int alpha = colors[i] >>> 24;
            if (alpha != 0 && alpha != 255) {
                throw new IllegalArgumentException("Use opaque palette colors or fully transparent void; disable antialiasing");
            }
            colors[i] = alpha == 0 ? 0 : colors[i] & 0xffffff;
            distance[i] = colors[i] == 0 ? 0 : 1_000_000;
        }
        // Chamfer distance to void, including the outside of the image. Biome boundaries
        // do not become island edges. O(width * height), calculated once on load.
        for (int z = 0; z < height; z++) for (int x = 0; x < width; x++) {
            int i = z * width + x;
            if (colors[i] == 0) continue;
            distance[i] = Math.min(distance[i], Math.min((x + 1) * 3, (z + 1) * 3));
            if (x > 0) distance[i] = Math.min(distance[i], distance[i - 1] + 3);
            if (z > 0) {
                distance[i] = Math.min(distance[i], distance[i - width] + 3);
                if (x > 0) distance[i] = Math.min(distance[i], distance[i - width - 1] + 4);
                if (x + 1 < width) distance[i] = Math.min(distance[i], distance[i - width + 1] + 4);
            }
        }
        for (int z = height - 1; z >= 0; z--) for (int x = width - 1; x >= 0; x--) {
            int i = z * width + x;
            if (colors[i] == 0) continue;
            distance[i] = Math.min(distance[i], Math.min((width - x) * 3, (height - z) * 3));
            if (x + 1 < width) distance[i] = Math.min(distance[i], distance[i + 1] + 3);
            if (z + 1 < height) {
                distance[i] = Math.min(distance[i], distance[i + width] + 3);
                if (x > 0) distance[i] = Math.min(distance[i], distance[i + width - 1] + 4);
                if (x + 1 < width) distance[i] = Math.min(distance[i], distance[i + width + 1] + 4);
            }
        }
    }

    private int index(int worldX, int worldZ) {
        long x = (long) worldX - centerX + width / 2;
        long z = (long) worldZ - centerZ + height / 2;
        return x < 0 || z < 0 || x >= width || z >= height ? -1 : (int) (z * width + x);
    }

    public int color(int x, int z) {
        int i = index(x, z);
        return i < 0 ? 0 : colors[i];
    }

    public double edgeDistance(int x, int z) {
        int i = index(x, z);
        return i < 0 ? 0 : distance[i] / 3.0;
    }

    public TreeSet<Integer> usedColors() {
        TreeSet<Integer> result = new TreeSet<>();
        for (int color : colors) if (color != 0) result.add(color);
        return result;
    }

    public int[] nearestLand(int x, int z) {
        long best = Long.MAX_VALUE;
        int[] result = null;
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == 0) continue;
            int wx = centerX + i % width - width / 2;
            int wz = centerZ + i / width - height / 2;
            long dx = (long) wx - x, dz = (long) wz - z;
            long d = dx * dx + dz * dz;
            if (d < best) { best = d; result = new int[]{wx, wz}; }
        }
        if (result == null) throw new IllegalArgumentException("Texture contains no land");
        return result;
    }

    public int thickness(int x, int z, int minimum, int maximum, int bevelWidth) {
        if (color(x, z) == 0) return 0;
        double edge = smoothStep((edgeDistance(x, z) - 1) / (bevelWidth * 2.0));
        return (int) Math.round(minimum + (maximum - minimum) * edge);
    }

    public double surface(long seed, int x, int z, int altitude, double amplitude,
                          int bevelWidth, int bevelDepth) {
        double edge = smoothStep((edgeDistance(x, z) - 1) / bevelWidth);
        return altitude + amplitude * relief(seed, x, z) - bevelDepth * (1 - edge);
    }

    public static <T> Map<Integer, T> dispatch(Set<Integer> colors, List<T> allowed, long seed) {
        if (allowed.isEmpty()) throw new IllegalArgumentException("No allowed biomes");
        List<T> biomes = new ArrayList<>(allowed);
        Collections.shuffle(biomes, new Random(seed));
        Map<Integer, T> result = new HashMap<>();
        int i = 0;
        for (int color : new TreeSet<>(colors)) result.put(color, biomes.get(i++ % biomes.size()));
        return Map.copyOf(result);
    }

    public static double smoothStep(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Smooth, bounded relief independent of chunk visitation order. */
    public static double relief(long seed, int x, int z) {
        int gx = Math.floorDiv(x, 32), gz = Math.floorDiv(z, 32);
        double tx = smoothStep(Math.floorMod(x, 32) / 32.0);
        double tz = smoothStep(Math.floorMod(z, 32) / 32.0);
        double a = lerp(noise(seed, gx, gz), noise(seed, gx + 1, gz), tx);
        double b = lerp(noise(seed, gx, gz + 1), noise(seed, gx + 1, gz + 1), tx);
        return lerp(a, b, tz);
    }

    private static double noise(long seed, int x, int z) {
        long h = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53 * 2 - 1;
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}
