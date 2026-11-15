import org.terraform.coregen.floating.BiomeTexture;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Dependency-free regression tests: run with javac/java, assertions always enabled here. */
public final class BiomeTextureTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static BufferedImage filled(int width, int height, int argb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int z = 0; z < height; z++) for (int x = 0; x < width; x++) image.setRGB(x, z, argb);
        return image;
    }
    public static void main(String[] args) {
        BufferedImage image = filled(64, 48, 0xff78b84a);
        image.setRGB(32, 24, 0xff287a38);
        BiomeTexture map = new BiomeTexture(image, 100, -200);
        check(map.color(100, -200) == 0x287a38, "Center pixel maps to configured X/Z");
        check(map.color(68, -224) == 0x78b84a, "Negative half extent is included");
        check(map.color(131, -177) == 0x78b84a, "Last pixel is included");
        check(map.color(132, -200) == 0 && map.color(67, -200) == 0, "Outside texture is void");
        check(map.color(Integer.MIN_VALUE, 0) == 0, "Lookup cannot overflow");
        check(map.edgeDistance(100, -200) == 24, "Biome boundary must not bevel the island");
        check(map.edgeDistance(68, -200) == 1, "Outside image participates in bevel");
        check(map.thickness(68, -200, 4, 18, 8) == 4, "Edge is shallow");
        check(map.thickness(100, -200, 4, 18, 8) == 18, "Interior depth is capped");
        check(map.surface(42, 68, -200, 100, 0, 8, 3) == 97, "Top edge bevel");
        check(map.surface(42, 100, -200, 100, 0, 8, 3) == 100, "Interior stays flat");
        image.setRGB(32, 24, 0xff000000);
        image.setRGB(33, 24, 0x0078b84a);
        map = new BiomeTexture(image, 100, -200);
        check(map.color(100, -200) == 0, "Black is void");
        check(map.color(101, -200) == 0, "Transparent RGB is void");
        check(map.edgeDistance(99, -200) == 1, "Interior holes bevel");
        check(map.thickness(100, -200, 4, 18, 8) == 0, "Void has no thickness");
        check(map.nearestLand(100, -200)[0] == 100 && map.nearestLand(100, -200)[1] == -201, "Deterministic nearest safe land");
        check(map.usedColors().size() == 1, "Void omitted from palette");
        BiomeTexture odd = new BiomeTexture(filled(5, 3, 0xff78b84a), -31, 43);
        check(odd.color(-33, 42) != 0 && odd.color(-28, 43) == 0, "Odd extents and negative centers");
        try {
            new BiomeTexture(filled(1, 1, 0x8078b84a), 0, 0);
            throw new AssertionError("Partial alpha accepted");
        } catch (IllegalArgumentException expected) { checks++; }
        try {
            new BiomeTexture(filled(2, 2, 0xff000000), 0, 0).nearestLand(0, 0);
            throw new AssertionError("Empty map accepted");
        } catch (IllegalArgumentException expected) { checks++; }
        var colors = new java.util.HashSet<>(java.util.List.of(1, 2, 3, 4));
        var allowed = java.util.List.of("PLAINS", "DESERT", "TAIGA");
        var dispatch = BiomeTexture.dispatch(colors, allowed, 42);
        check(dispatch.equals(BiomeTexture.dispatch(new java.util.LinkedHashSet<>(java.util.List.of(4, 2, 3, 1)), allowed, 42)), "Dispatch independent of color iteration order");
        check(new java.util.HashSet<>(dispatch.values()).equals(new java.util.HashSet<>(allowed)), "Dispatch covers allowed biomes when enough colors exist");
        check(!dispatch.equals(BiomeTexture.dispatch(colors, allowed, 43)), "Different dispatch seeds can change assignments");
        Random rng = new Random(42);
        for (int i = 0; i < 5000; i++) {
            int x = rng.nextInt(5000) - 2500, z = rng.nextInt(5000) - 2500;
            double n = BiomeTexture.relief(42, x, z);
            check(n >= -1 && n <= 1, "Relief is bounded");
            check(n == BiomeTexture.relief(42, x, z), "Stable relief independent of call order");
            check(Math.abs(n - BiomeTexture.relief(42, x + 1, z)) < 0.1, "No chunk/grid seams");
        }
        // Compare distance to a shortest-path oracle for random masks (8-neighbor chamfer weights).
        for (int trial = 0; trial < 20; trial++) {
            BufferedImage mask = filled(15, 13, 0xff78b84a);
            for (int z = 0; z < 13; z++) for (int x = 0; x < 15; x++) if (rng.nextInt(6) == 0) mask.setRGB(x,z,0xff000000);
            BiomeTexture sample = new BiomeTexture(mask, 7, 6);
            for (int z = 0; z < 13; z++) for (int x = 0; x < 15; x++) {
                if (sample.color(x,z) == 0) continue;
                int expected = 3 * Math.min(Math.min(x+1,15-x),Math.min(z+1,13-z));
                for (int vz = 0; vz < 13; vz++) for (int vx = 0; vx < 15; vx++) if (sample.color(vx,vz) == 0) {
                    int dx = Math.abs(x-vx), dz = Math.abs(z-vz);
                    expected = Math.min(expected, 3*Math.max(dx,dz)+Math.min(dx,dz));
                }
                check(Math.abs(sample.edgeDistance(x,z)*3-expected)<0.001, "Distance transform agrees with oracle");
                int depth = sample.thickness(x,z,4,18,8);
                check(depth >= 4 && depth <= 18, "Every solid column has bounded depth");
            }
        }
        System.out.println("BiomeTexture: " + checks + " checks passed");
    }
}
