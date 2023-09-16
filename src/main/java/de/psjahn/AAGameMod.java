package de.psjahn;

import de.psjahn.screen.AAScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AAGameMod implements ClientModInitializer {
	private static KeyBinding toggleGameVisible;
	@Override
	public void onInitializeClient() {
		toggleGameVisible = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.examplemod.spook", // action translation key
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_R, // keycode
				"category.examplemod.test" // category translation key
		));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleGameVisible.wasPressed()) {
				//Toggle Game Visibility
				MinecraftClient.getInstance().setScreen(new AAScreen());
			}
		});
	}
}