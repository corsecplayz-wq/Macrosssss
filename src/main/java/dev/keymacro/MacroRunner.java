package dev.keymacro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Step syntax (one string per step):
 *   tap <key>            press + release instantly (can chain many in one tick)
 *   press <key>          hold the key down
 *   release <key>        let go of the key
 *   hold <key> <ticks>   press, wait N ticks, release
 *   wait <ticks>         pause the sequence (20 ticks = 1 second)
 *
 * Keys: "f", "2", "space", "left_shift", "lmb", "rmb", "mmb",
 *       or full names like "key.keyboard.left.control", "key.mouse.4".
 */
final class MacroRunner {
    private enum Op { TAP, PRESS, RELEASE, WAIT }

    private record Step(Op op, InputUtil.Key key, int n) {}

    private static final class Compiled {
        String name;
        InputUtil.Key trigger;
        boolean loop;
        List<Step> steps = new ArrayList<>();
        boolean wasDown;
        boolean running;
        int idx;
        int wait;
        final Set<InputUtil.Key> held = new HashSet<>();
    }

    private final List<Compiled> macros = new ArrayList<>();
    private boolean enabled = true;

    int reload() {
        stopAll();
        macros.clear();
        MacroConfig cfg = MacroConfig.load();
        enabled = cfg.enabled;
        for (MacroConfig.Macro m : cfg.macros) {
            try {
                Compiled c = new Compiled();
                c.name = m.name;
                c.trigger = parseKey(m.trigger);
                c.loop = m.loop;
                for (String s : m.steps) parseStep(s, c.steps);
                macros.add(c);
            } catch (Exception e) {
                KeyMacroMod.LOG.error("Skipping macro '{}': {}", m.name, e.getMessage());
            }
        }
        KeyMacroMod.LOG.info("Loaded {} macro(s)", macros.size());
        return macros.size();
    }

    void stopAll() {
        for (Compiled c : macros) finish(c);
    }

    void tick(MinecraftClient client) {
        if (client.getWindow() == null) return;
        long handle = client.getWindow().getHandle();
        boolean canRun = enabled && client.player != null && client.currentScreen == null;

        for (Compiled m : macros) {
            boolean down = isDown(handle, m.trigger);
            if (down && !m.wasDown && !m.running && canRun) {
                m.running = true;
                m.idx = 0;
                m.wait = 0;
            }
            m.wasDown = down;

            if (!m.running) continue;
            if (!canRun) { finish(m); continue; }

            if (m.wait > 0) {
                m.wait--;
                if (m.wait > 0) continue;
            }

            // Run as many steps as possible this tick (no delay between them)
            int guard = 0;
            while (m.idx < m.steps.size() && guard++ < 500) {
                Step s = m.steps.get(m.idx++);
                switch (s.op) {
                    case TAP -> { press(m, s.key); release(m, s.key); }
                    case PRESS -> press(m, s.key);
                    case RELEASE -> release(m, s.key);
                    case WAIT -> m.wait = s.n;
                }
                if (s.op == Op.WAIT && s.n > 0) break;
            }

            if (m.idx >= m.steps.size() && m.wait <= 0) {
                if (m.loop && down) {
                    m.idx = 0;      // restart next tick while trigger is still held
                } else {
                    finish(m);
                }
            }
        }
    }

    private static void press(Compiled m, InputUtil.Key key) {
        KeyBinding.onKeyPressed(key);
        KeyBinding.setKeyPressed(key, true);
        m.held.add(key);
    }

    private static void release(Compiled m, InputUtil.Key key) {
        KeyBinding.setKeyPressed(key, false);
        m.held.remove(key);
    }

    private static void finish(Compiled m) {
        for (InputUtil.Key k : new ArrayList<>(m.held)) KeyBinding.setKeyPressed(k, false);
        m.held.clear();
        m.running = false;
        m.idx = 0;
        m.wait = 0;
    }

    private static boolean isDown(long handle, InputUtil.Key key) {
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(handle, key.getCode());
    }

    // ---------- parsing ----------

    private static void parseStep(String raw, List<Step> out) {
        String[] t = raw.trim().split("\\s+");
        String cmd = t[0].toLowerCase(Locale.ROOT);
        switch (cmd) {
            case "tap" -> out.add(new Step(Op.TAP, parseKey(t[1]), 0));
            case "press" -> out.add(new Step(Op.PRESS, parseKey(t[1]), 0));
            case "release" -> out.add(new Step(Op.RELEASE, parseKey(t[1]), 0));
            case "wait" -> out.add(new Step(Op.WAIT, null, Integer.parseInt(t[1])));
            case "hold" -> {
                InputUtil.Key k = parseKey(t[1]);
                out.add(new Step(Op.PRESS, k, 0));
                out.add(new Step(Op.WAIT, null, Integer.parseInt(t[2])));
                out.add(new Step(Op.RELEASE, k, 0));
            }
            default -> throw new IllegalArgumentException("Unknown step: " + raw);
        }
    }

    private static InputUtil.Key parseKey(String s) {
        String k = s.trim().toLowerCase(Locale.ROOT);
        switch (k) {
            case "lmb", "mouse_left" -> k = "key.mouse.left";
            case "rmb", "mouse_right" -> k = "key.mouse.right";
            case "mmb", "mouse_middle" -> k = "key.mouse.middle";
            default -> {
                if (k.matches("mouse\\d")) k = "key.mouse." + k.substring(5);
                else if (!k.startsWith("key.")) k = "key.keyboard." + k.replace('_', '.');
            }
        }
        try {
            return InputUtil.fromTranslationKey(k);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown key '" + s + "'");
        }
    }
}
