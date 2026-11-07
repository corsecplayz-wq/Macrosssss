package dev.keymacro;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyMacroMod implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("keymacro");
    private static final MacroRunner RUNNER = new MacroRunner();

    @Override
    public void onInitializeClient() {
        RUNNER.reload();
        ClientTickEvents.END_CLIENT_TICK.register(RUNNER::tick);

        // /keymacro reload  -> re-read config/keymacro.json without restarting
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("keymacro")
                .then(ClientCommandManager.literal("reload").executes(ctx -> {
                    int n = RUNNER.reload();
                    ctx.getSource().sendFeedback(Text.literal("[KeyMacro] Loaded " + n + " macro(s)."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("stop").executes(ctx -> {
                    RUNNER.stopAll();
                    ctx.getSource().sendFeedback(Text.literal("[KeyMacro] Stopped all macros."));
                    return 1;
                }))));
    }
}
