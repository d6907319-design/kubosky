package dev.kubosky;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class SkyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SkyConfig inst = new SkyConfig();

    public boolean enabled = true, stars = true, sun = true, moon = true, clouds = true, dimAtNight = true;
    public int top = 0x2b1b4a, horizon = 0xff7a45, bottom = 0x3a1a2a, sunColor = 0xffd27a, moonColor = 0xdfe6ff;
    public int starCount = 1500, sunSize = 30, moonSize = 20;
    public boolean killEffect = true, killSound = true, killBanner = true, hitParticles = true, hitMarker = true, lowHp = true;
    public int killStyle = 0;
    public boolean aurora = true, meteors = true, nebula = true, hudWatermark = true, hudKeys = true, hudEffects = true, hudArmor = true, hudList = true, rainbow = false, floatDmg = true, customCross = true, trail = false, killLightning = true;
    public int meteorRate = 4, zoom = 4, crossStyle = 1, auraMode = 0, accent = 0x8fb0ff;

    public static final String[] PRESETS = {"Закат", "Космос", "Неон", "Ясный день"};
    private static final int[][] PRESET_COLORS = {
            {0x2b1b4a, 0xff7a45, 0x3a1a2a, 0xffd27a, 0xdfe6ff},
            {0x02030a, 0x0a1030, 0x000000, 0xfff2c0, 0xdfe6ff},
            {0x14002e, 0xff2fa6, 0x1b0840, 0x6cf0ff, 0xffb0f0},
            {0x4a86e8, 0xbfe0ff, 0x9cc0e8, 0xfff4b0, 0xdfe6ff}
    };

    public static SkyConfig get() { return inst; }

    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("kubosky.json"); }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    SkyConfig c = GSON.fromJson(r, SkyConfig.class);
                    if (c != null) inst = c;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        save();
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(inst, w);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getColor(int i) {
        switch (i) {
            case 0: return top;
            case 1: return horizon;
            case 2: return bottom;
            case 3: return sunColor;
            default: return moonColor;
        }
    }

    public void setColor(int i, int v) {
        switch (i) {
            case 0: top = v; break;
            case 1: horizon = v; break;
            case 2: bottom = v; break;
            case 3: sunColor = v; break;
            default: moonColor = v;
        }
    }

    public void applyPreset(int i) {
        int[] p = PRESET_COLORS[i];
        for (int k = 0; k < 5; k++) setColor(k, p[k]);
    }
}
