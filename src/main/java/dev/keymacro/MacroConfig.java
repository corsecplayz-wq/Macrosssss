package dev.keymacro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MacroConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public List<Macro> macros = new ArrayList<>();

    public static class Macro {
        public String name = "unnamed";
        public String trigger;          // e.g. "r", "mouse4", "key.keyboard.r"
        public boolean loop = false;    // repeat the sequence while the trigger is held
        public List<String> steps = new ArrayList<>();
    }

    static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("keymacro.json");
    }

    static MacroConfig load() {
        Path p = path();
        if (!Files.exists(p)) {
            MacroConfig def = defaults();
            try {
                Files.writeString(p, GSON.toJson(def));
            } catch (IOException e) {
                KeyMacroMod.LOG.error("Could not write default config", e);
            }
            return def;
        }
        try {
            MacroConfig cfg = GSON.fromJson(Files.readString(p), MacroConfig.class);
            return cfg != null ? cfg : new MacroConfig();
        } catch (Exception e) {
            KeyMacroMod.LOG.error("Could not read config, using empty one", e);
            return new MacroConfig();
        }
    }

    private static MacroConfig defaults() {
        MacroConfig c = new MacroConfig();

        Macro swap = new Macro();
        swap.name = "Quick swap";
        swap.trigger = "r";
        swap.steps.add("tap 2");        // select hotbar slot 2
        swap.steps.add("tap rmb");      // use item
        swap.steps.add("tap 1");        // back to slot 1
        c.macros.add(swap);

        Macro clicker = new Macro();
        clicker.name = "Rapid use";
        clicker.trigger = "g";
        clicker.loop = true;
        clicker.steps.add("tap rmb");
        clicker.steps.add("wait 1");
        c.macros.add(clicker);

        Macro hop = new Macro();
        hop.name = "Sneak hop";
        hop.trigger = "v";
        hop.steps.add("press left_shift");
        hop.steps.add("tap space");
        hop.steps.add("wait 3");
        hop.steps.add("release left_shift");
        c.macros.add(hop);

        return c;
    }
}
