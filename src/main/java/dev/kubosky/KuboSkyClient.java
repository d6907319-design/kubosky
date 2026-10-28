package dev.kubosky;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KuboSkyClient implements ClientModInitializer {
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        SkyConfig.load();
        Effects.init();
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kubosky.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.kubosky"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.openScreen(new SkyScreen(0));
                }
            }
        });
    }
}
